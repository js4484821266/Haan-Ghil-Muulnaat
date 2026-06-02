package com.haanghil.muulnaat

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Public start helper for the auto-save foreground service.
 *
 * Keeping this as a companion extension preserves the existing call shape:
 * `AutoSaveProtectionService.start(context, uris)`.
 */
fun AutoSaveProtectionService.Companion.start(context: Context, uris: List<Uri>) {
    val intent = Intent(context, AutoSaveProtectionService::class.java).apply {
        action = ShareContract.ACTION_START_AUTO_SAVE
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putParcelableArrayListExtra(ShareContract.EXTRA_URIS, ArrayList(uris))
        if (uris.isNotEmpty()) {
            clipData = clipDataForAutoSave(context, uris)
        }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

private fun clipDataForAutoSave(context: Context, uris: List<Uri>): ClipData {
    return ClipData.newUri(context.contentResolver, "shared image", uris.first()).also { data ->
        uris.drop(1).forEach { uri -> data.addItem(ClipData.Item(uri)) }
    }
}
