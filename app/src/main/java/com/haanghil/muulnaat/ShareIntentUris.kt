package com.haanghil.muulnaat

import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Normalizes Android share payloads into one ordered URI list.
 *
 * Some sender apps use EXTRA_STREAM, some use ClipData, and some provide both.
 * Duplicate filtering keeps later pipeline work from processing the same image
 * twice.
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
