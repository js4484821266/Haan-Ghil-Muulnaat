package com.haanghil.muulnaat

import android.app.Activity
import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat

abstract class ShareForwardingActivity : Activity() {
    protected abstract val shareMode: String
    private var pendingAutoSaveUris: List<Uri> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        forwardShare(intent)
    }

    private fun forwardShare(incoming: Intent?) {
        val sharedUris = incoming?.sharedImageUris().orEmpty()
        if (sharedUris.isEmpty()) {
            finish()
            return
        }

        val effectiveMode = if (
            incoming?.action == Intent.ACTION_SEND_MULTIPLE ||
            sharedUris.size > 1
        ) {
            ShareContract.MODE_AUTO_SAVE_BATCH
        } else {
            shareMode
        }

        if (effectiveMode == ShareContract.MODE_READY_TO_SAVE) {
            startActivity(mainActivityIntent(effectiveMode, sharedUris))
            finish()
            return
        }

        pendingAutoSaveUris = sharedUris
        val missingPermissions = missingAutoSavePermissions()
        if (missingPermissions.isEmpty()) {
            startAutoSaveService(sharedUris)
        } else {
            requestPermissions(missingPermissions, REQUEST_AUTO_SAVE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_AUTO_SAVE_PERMISSIONS) return

        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startAutoSaveService(pendingAutoSaveUris)
        } else {
            Toast.makeText(this, getString(R.string.toast_autosave_permission_denied), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startAutoSaveService(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            AutoSaveProtectionService.start(this, uris)
        }
        finish()
    }

    private fun mainActivityIntent(mode: String, sharedUris: List<Uri>): Intent {
        val target = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(ShareContract.EXTRA_MODE, mode)
            putParcelableArrayListExtra(ShareContract.EXTRA_URIS, ArrayList(sharedUris))
            clipData = clipDataFor(sharedUris)
        }
        return target
    }

    private fun missingAutoSavePermissions(): Array<String> {
        val missing = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission)
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission)
            }
        }
        return missing.toTypedArray()
    }

    private fun Intent.sharedImageUris(): List<Uri> {
        val uris = mutableListOf<Uri>()

        if (action == Intent.ACTION_SEND_MULTIPLE) {
            getUriArrayListExtraCompat(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
        } else {
            getUriExtraCompat(Intent.EXTRA_STREAM)?.let { uris.add(it) }
        }

        clipData?.let { data ->
            for (index in 0 until data.itemCount) {
                data.getItemAt(index).uri?.let { uri ->
                    if (!uris.contains(uri)) {
                        uris.add(uri)
                    }
                }
            }
        }

        return uris
    }

    private fun clipDataFor(uris: List<Uri>): ClipData {
        val data = ClipData.newUri(contentResolver, "shared image", uris.first())
        uris.drop(1).forEach { uri ->
            data.addItem(ClipData.Item(uri))
        }
        return data
    }

    @Suppress("DEPRECATION")
    private fun Intent.getUriExtraCompat(name: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(name, Uri::class.java)
        } else {
            getParcelableExtra(name)
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.getUriArrayListExtraCompat(name: String): ArrayList<Uri>? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(name, Uri::class.java)
        } else {
            getParcelableArrayListExtra(name)
        }
    }
}

class ShareReadyToSaveActivity : ShareForwardingActivity() {
    override val shareMode: String = ShareContract.MODE_READY_TO_SAVE
}

class ShareAutoSaveActivity : ShareForwardingActivity() {
    override val shareMode: String = ShareContract.MODE_AUTO_SAVE
}

private const val REQUEST_AUTO_SAVE_PERMISSIONS = 6101
