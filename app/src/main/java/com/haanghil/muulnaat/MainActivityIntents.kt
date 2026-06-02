package com.haanghil.muulnaat

import android.content.Intent
import android.net.Uri
import android.os.Build

internal fun MainActivity.handleForwardedShareIntent(intent: Intent?) {
    if (intent == null) return

    val shareMode = intent.getStringExtra(ShareContract.EXTRA_MODE) ?: return
    val sharedUris = intent.getSharedUrisExtra()
    intent.removeExtra(ShareContract.EXTRA_MODE)
    intent.removeExtra(ShareContract.EXTRA_URIS)

    if (sharedUris.isEmpty()) {
        binding.resultText.text = getString(R.string.result_no_image_selected)
        return
    }

    when (shareMode) {
        ShareContract.MODE_READY_TO_SAVE ->
            processSingleImageUri(sharedUris.first(), autoSaveAfterProtection = false)
        ShareContract.MODE_AUTO_SAVE ->
            runWithStoragePermissionIfNeeded {
                processSingleImageUri(sharedUris.first(), autoSaveAfterProtection = true)
            }
        ShareContract.MODE_AUTO_SAVE_BATCH ->
            runWithStoragePermissionIfNeeded { processImageBatch(sharedUris) }
    }
}

internal fun MainActivity.handleBackgroundStatusIntent(intent: Intent?) {
    if (intent == null) return

    val statusMessage = intent.getStringExtra(ShareContract.EXTRA_STATUS_MESSAGE)
    val progressMessage = intent.getStringExtra(ShareContract.EXTRA_PROGRESS_MESSAGE)
    if (statusMessage == null && progressMessage == null) return

    binding.resultText.text = progressMessage ?: statusMessage
    if (progressMessage != null) showSearchProgress(progressMessage)
    intent.removeExtra(ShareContract.EXTRA_STATUS_MESSAGE)
    intent.removeExtra(ShareContract.EXTRA_PROGRESS_MESSAGE)
}

@Suppress("DEPRECATION")
internal fun Intent.getSharedUrisExtra(): List<Uri> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableArrayListExtra(ShareContract.EXTRA_URIS, Uri::class.java).orEmpty()
    } else {
        getParcelableArrayListExtra<Uri>(ShareContract.EXTRA_URIS).orEmpty()
    }
}
