package com.haanghil.muulnaat

import android.content.ClipData
import android.content.Intent
import android.net.Uri

/**
 * MainActivity로 넘길 명시적 Intent를 만듭니다.
 *
 * MainActivity가 공유 이미지를 디코딩하는 동안 Android가 각 URI의 읽기 권한을
 * 유지하도록 EXTRA_URIS와 함께 ClipData도 붙입니다.
 */
internal fun ShareForwardingActivity.mainActivityIntent(mode: String, sharedUris: List<Uri>): Intent {
    return Intent(this, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(ShareContract.EXTRA_MODE, mode)
        putParcelableArrayListExtra(ShareContract.EXTRA_URIS, ArrayList(sharedUris))
        clipData = clipDataFor(sharedUris)
    }
}

private fun ShareForwardingActivity.clipDataFor(uris: List<Uri>): ClipData {
    val data = ClipData.newUri(contentResolver, "shared image", uris.first())
    uris.drop(1).forEach { uri ->
        data.addItem(ClipData.Item(uri))
    }
    return data
}
