package com.haanghil.muulnaat

import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Android 공유 데이터를 순서 있는 URI 목록 하나로 정규화합니다.
 *
 * 어떤 발신 앱은 EXTRA_STREAM을 쓰고, 어떤 앱은 ClipData를 쓰며, 둘 다 주는 앱도
 * 있습니다. 중복 제거를 통해 이후 파이프라인이 같은 이미지를 두 번 처리하지 않게 합니다.
 */
internal fun Intent.sharedImageUris(): List<Uri> {
    val uris = mutableListOf<Uri>()
    if (action == Intent.ACTION_SEND_MULTIPLE) {
        getUriArrayListExtraCompat(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
    } else {
        getUriExtraCompat(Intent.EXTRA_STREAM)?.let { uris.add(it) }
    }
    clipData?.appendUniqueUrisTo(uris)
    return uris
}

internal fun Intent?.isBatchShare(sharedUris: List<Uri>): Boolean {
    return this?.action == Intent.ACTION_SEND_MULTIPLE || sharedUris.size > 1
}

private fun android.content.ClipData.appendUniqueUrisTo(uris: MutableList<Uri>) {
    for (index in 0 until itemCount) {
        getItemAt(index).uri?.let { uri ->
            if (!uris.contains(uri)) uris.add(uri)
        }
    }
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
