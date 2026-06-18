package com.haanghil.muulnaat

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

/**
 * 얇은 공유 진입 Activity입니다.
 *
 * Android 공유 대상은 Activity여야 하지만, 실제 작업은 MainActivity(단일 이미지,
 * 화면 표시) 또는 AutoSaveProtectionService(일괄/백그라운드)가 맡습니다.
 * 이 클래스는 공유 URI 목록을 어느 경로로 보낼지만 결정합니다.
 */
abstract class ShareForwardingActivity : Activity() {
    internal abstract val shareMode: String
    internal abstract val autoSaveConfig: HidingConfig
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
            AutoSaveProtectionService.start(this, uris, autoSaveConfig)
        }
        finish()
    }
}

internal const val REQUEST_AUTO_SAVE_PERMISSIONS = 6101
