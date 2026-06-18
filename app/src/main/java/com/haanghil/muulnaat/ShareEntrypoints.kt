package com.haanghil.muulnaat

/**
 * Manifest에 노출되는 공유 대상입니다.
 *
 * 하위 클래스는 모든 전달 코드를 공유합니다. Android 공유 시트에 보이는 기본
 * 모드와 자동 저장 은닉 방법만 다릅니다.
 */
class ShareReadyToSaveActivity : ShareForwardingActivity() {
    internal override val shareMode: String = ShareContract.MODE_READY_TO_SAVE
    internal override val autoSaveConfig: HidingConfig = HidingConfig.noise()
}

class ShareAutoSaveNoiseActivity : ShareForwardingActivity() {
    internal override val shareMode: String = ShareContract.MODE_AUTO_SAVE
    internal override val autoSaveConfig: HidingConfig = HidingConfig.noise()
}

class ShareAutoSaveBlurActivity : ShareForwardingActivity() {
    internal override val shareMode: String = ShareContract.MODE_AUTO_SAVE
    internal override val autoSaveConfig: HidingConfig = HidingConfig.blur()
}

class ShareAutoSaveSolidFillActivity : ShareForwardingActivity() {
    internal override val shareMode: String = ShareContract.MODE_AUTO_SAVE
    internal override val autoSaveConfig: HidingConfig = HidingConfig.solidFill()
}
