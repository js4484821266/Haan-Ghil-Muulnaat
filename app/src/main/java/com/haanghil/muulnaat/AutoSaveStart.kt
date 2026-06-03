package com.haanghil.muulnaat

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * 자동 저장 포그라운드 서비스를 시작하는 공개 헬퍼입니다.
 *
 * companion 확장으로 두면 기존 호출 형태인
 * `AutoSaveProtectionService.start(context, uris)`를 유지할 수 있습니다.
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
