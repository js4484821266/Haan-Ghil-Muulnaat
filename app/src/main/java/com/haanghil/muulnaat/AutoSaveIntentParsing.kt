package com.haanghil.muulnaat

import android.content.Intent
import android.net.Uri
import android.os.Build

@Suppress("DEPRECATION")
internal fun Intent.sharedUris(): List<Uri> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(ShareContract.EXTRA_URIS, Uri::class.java).orEmpty()
    } else {
        getParcelableArrayListExtra<Uri>(ShareContract.EXTRA_URIS).orEmpty()
    }
}
