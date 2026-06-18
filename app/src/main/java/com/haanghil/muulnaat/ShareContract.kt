package com.haanghil.muulnaat

object ShareContract {
    const val ACTION_START_AUTO_SAVE = "com.haanghil.muulnaat.action.START_AUTO_SAVE"
    const val ACTION_CANCEL_AUTO_SAVE = "com.haanghil.muulnaat.action.CANCEL_AUTO_SAVE"

    const val EXTRA_MODE = "com.haanghil.muulnaat.extra.SHARE_MODE"
    const val EXTRA_URIS = "com.haanghil.muulnaat.extra.SHARE_URIS"
    const val EXTRA_STATUS_MESSAGE = "com.haanghil.muulnaat.extra.STATUS_MESSAGE"
    const val EXTRA_PROGRESS_MESSAGE = "com.haanghil.muulnaat.extra.PROGRESS_MESSAGE"
    const val EXTRA_HIDING_METHOD = "com.haanghil.muulnaat.extra.HIDING_METHOD"
    const val EXTRA_SOLID_COLOR = "com.haanghil.muulnaat.extra.SOLID_COLOR"

    const val METHOD_NOISE = "noise"
    const val METHOD_BLUR = "blur"
    const val METHOD_SOLID_FILL = "solid_fill"

    const val MODE_READY_TO_SAVE = "ready_to_save"
    const val MODE_AUTO_SAVE = "auto_save"
    const val MODE_AUTO_SAVE_BATCH = "auto_save_batch"
}
