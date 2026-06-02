package com.haanghil.muulnaat

/**
 * Manifest-visible share targets.
 *
 * The two subclasses share all forwarding code. They differ only in the default
 * mode shown to Android's share sheet.
 */
class ShareReadyToSaveActivity : ShareForwardingActivity() {
    internal override val shareMode: String = ShareContract.MODE_READY_TO_SAVE
}

class ShareAutoSaveActivity : ShareForwardingActivity() {
    internal override val shareMode: String = ShareContract.MODE_AUTO_SAVE
}
