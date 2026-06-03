package com.haanghil.muulnaat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 백그라운드 자동 저장을 위한 런타임 권한 관문입니다.
 *
 * Android 13 이상에서는 알림 권한이 필요하고, Android 9 이하에서는 갤러리 쓰기에
 * 레거시 저장소 권한이 필요합니다.
 */
internal fun ShareForwardingActivity.requestMissingPermissionsOrStart(sharedUris: List<android.net.Uri>) {
    val missingPermissions = missingAutoSavePermissions()
    if (missingPermissions.isEmpty()) {
        startAutoSaveService(sharedUris)
    } else {
        requestPermissions(missingPermissions, REQUEST_AUTO_SAVE_PERMISSIONS)
    }
}

internal fun IntArray.allGranted(): Boolean {
    return isNotEmpty() && all { it == PackageManager.PERMISSION_GRANTED }
}

private fun ShareForwardingActivity.missingAutoSavePermissions(): Array<String> {
    val missing = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        addIfMissing(missing, Manifest.permission.POST_NOTIFICATIONS)
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        addIfMissing(missing, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    return missing.toTypedArray()
}

private fun ShareForwardingActivity.addIfMissing(missing: MutableList<String>, permission: String) {
    if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        missing.add(permission)
    }
}
