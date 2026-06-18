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

internal fun Intent.hidingConfigExtra(): HidingConfig {
    val color = getIntExtra(ShareContract.EXTRA_SOLID_COLOR, HidingConfig.SOLID_FILL_DEFAULT_COLOR)
    return when (getStringExtra(ShareContract.EXTRA_HIDING_METHOD)) {
        ShareContract.METHOD_BLUR -> HidingConfig.blur()
        ShareContract.METHOD_SOLID_FILL -> HidingConfig.solidFill(color)
        else -> HidingConfig.noise()
    }
}
