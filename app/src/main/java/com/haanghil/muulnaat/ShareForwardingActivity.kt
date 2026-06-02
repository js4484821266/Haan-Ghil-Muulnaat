package com.haanghil.muulnaat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * Thin share-entry activity.
 *
 * Android share targets must be activities, but the real work belongs either to
 * MainActivity (single image, user-visible) or AutoSaveProtectionService
 * (batch/background). This class only decides which path should receive the
 * shared URI list.
 */
abstract class ShareForwardingActivity : Activity() {
    internal abstract val shareMode: String
    internal var pendingAutoSaveUris: List<Uri> = emptyList()

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

        val effectiveMode = if (incoming.isBatchShare(sharedUris)) {
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
        requestMissingPermissionsOrStart(sharedUris)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_AUTO_SAVE_PERMISSIONS) return

        if (grantResults.allGranted()) {
            startAutoSaveService(pendingAutoSaveUris)
        } else {
            Toast.makeText(this, getString(R.string.toast_autosave_permission_denied), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    internal fun startAutoSaveService(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            AutoSaveProtectionService.start(this, uris)
        }
        finish()
    }
}

internal const val REQUEST_AUTO_SAVE_PERMISSIONS = 6101
