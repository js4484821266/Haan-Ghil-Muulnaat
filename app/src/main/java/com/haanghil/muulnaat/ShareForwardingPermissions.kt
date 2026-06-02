package com.haanghil.muulnaat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Runtime permission gate for background auto-save.
 *
 * Notifications are required on Android 13+, and gallery writes need legacy
 * storage permission on Android 9 and older.
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
