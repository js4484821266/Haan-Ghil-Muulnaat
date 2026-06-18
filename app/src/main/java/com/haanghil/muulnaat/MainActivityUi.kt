package com.haanghil.muulnaat

import android.view.View

/**
 * MainActivity의 작은 UI 상태 헬퍼입니다.
 *
 * 이 함수들은 기존 위젯의 상태만 토글합니다. 메트릭과 이미지 렌더링은 주변 파일에
 * 두어 이 파일을 빠르게 훑을 수 있게 합니다.
 */
internal fun MainActivity.clearResultCards() {
    binding.protectionStatusValue.text = getString(R.string.status_na)
    binding.modelFaceCountValue.text = getString(R.string.metric_face_count_placeholder)
    binding.modelFacialFeatureValue.text = getString(R.string.metric_facial_features_placeholder)
    binding.modelLabelShiftValue.text = getString(R.string.metric_label_shift_placeholder)
    binding.modelScoreValue.text = getString(R.string.metric_score_placeholder)
    binding.qualityPsnrValue.text = getString(R.string.metric_psnr_placeholder)
    binding.qualityDeltaValue.text = getString(R.string.metric_delta_placeholder)
    binding.qualityEdgeDeltaValue.text = getString(R.string.metric_edge_delta_placeholder)
}

internal fun MainActivity.statusLabel(status: ProtectionStatus): String = when (status) {
    ProtectionStatus.PASS -> getString(R.string.status_pass)
    ProtectionStatus.BROKEN -> getString(R.string.status_broken)
    ProtectionStatus.HELD -> getString(R.string.status_held)
}

internal fun MainActivity.statusLabelForSearchStep(step: NoiseSearcher.SearchStep): String {
    return if (step.passed) getString(R.string.status_held) else getString(R.string.status_broken)
}

internal fun MainActivity.setBusy(isBusy: Boolean, message: String? = null) {
    binding.busyProgressBar.visibility = if (isBusy) View.VISIBLE else View.GONE
    binding.pickButton.isEnabled = !isBusy
    binding.hidingMethodSpinner.isEnabled = !isBusy
    binding.autoRecoverySwitch.isEnabled = !isBusy
    binding.applyButton.isEnabled = !isBusy
    binding.attackButton.isEnabled = !isBusy && state.selectedMethod != HidingMethod.SOLID_FILL
    binding.resetOptimalButton.isEnabled = !isBusy && state.optimalStrength != null
    binding.saveButton.isEnabled = !isBusy
    binding.strengthSeekBar.isEnabled = !isBusy
    binding.solidColorInput.isEnabled = !isBusy
    if (message != null) binding.resultText.text = message
}

internal fun MainActivity.showSearchProgress(message: String) {
    binding.searchProgressText.visibility = View.VISIBLE
    binding.searchProgressText.text = message
    binding.resetOptimalButton.visibility = View.GONE
}

internal fun MainActivity.showOptimalActionButton() {
    binding.searchProgressText.visibility = View.GONE
    binding.resetOptimalButton.visibility = View.VISIBLE
}

internal fun MainActivity.setTechnicalDetailsVisible(visible: Boolean) {
    binding.technicalDetailsContainer.visibility = if (visible) View.VISIBLE else View.GONE
    binding.technicalDetailsToggle.text = if (visible) {
        getString(R.string.technical_details_shown)
    } else {
        getString(R.string.technical_details_hidden)
    }
}
