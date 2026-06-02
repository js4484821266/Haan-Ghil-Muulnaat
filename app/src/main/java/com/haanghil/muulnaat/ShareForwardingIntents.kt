package com.haanghil.muulnaat

import android.content.ClipData
import android.content.Intent
import android.net.Uri

/**
 * Builds the explicit MainActivity handoff.
 *
 * ClipData is attached alongside EXTRA_URIS so Android keeps read permission for
 * every shared image URI while MainActivity decodes it.
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
