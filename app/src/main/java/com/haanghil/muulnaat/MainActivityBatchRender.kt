package com.haanghil.muulnaat

import android.graphics.Bitmap

/**
 * 가장 최근에 완료된 일괄 처리 항목을 UI에 그립니다.
 *
 * 일괄 모드는 의도적으로 단일 이미지 미리보기 패널을 재사용합니다. 별도 결과 화면을
 * 만들지 않아도 사용자가 현재 상태를 파악할 수 있습니다.
 */
internal fun MainActivity.renderBatchResult(
    loaded: Bitmap,
    protected: Bitmap,
    minStrength: Int,
    config: HidingConfig,
    defenseReport: DefenseEvaluationReport?,
    saveResult: GallerySaveResult,
    itemNumber: Int,
    total: Int,
) {
    state.originalBitmap = loaded
    state.protectedBitmap = protected
    state.lastAppliedStrength = minStrength
    state.lastAppliedMethod = config.method
    if (config.method.supportsStrengthSearch()) {
        storeOptimalStrength(config.method, minStrength)
        binding.recommendedStrengthText.text = StrengthAdvisor.recommendationText(this, minStrength)
        binding.strengthSeekBar.progress = minStrength
        binding.strengthLabel.text = getString(R.string.noise_strength_value, minStrength)
        showOptimalActionButton()
    } else {
        binding.recommendedStrengthText.text = getString(R.string.recommended_strength_solid_fill)
        showSearchProgress(getString(R.string.search_progress_solid_fill))
    }
    renderProtectedImage(loaded, protected)
    renderBatchDefense(defenseReport, minStrength, config)
    binding.resultText.text = batchSaveMessage(saveResult, itemNumber, total)
}

private fun MainActivity.renderBatchDefense(
    defenseReport: DefenseEvaluationReport?,
    minStrength: Int,
    config: HidingConfig,
) {
    if (defenseReport == null) {
        renderRecoveredImage(null)
    } else {
        renderRecoveredImage(if (config.method == HidingMethod.NOISE) defenseReport.attackedBitmap else null)
        renderDefenseResult(defenseReport.status, defenseReport.evaluationMetrics, defenseReport.qualityMetrics)
        state.lastEvaluationStrength = minStrength
    }
}

private fun MainActivity.batchSaveMessage(saveResult: GallerySaveResult, itemNumber: Int, total: Int): String {
    return if (saveResult.success) {
        getString(R.string.result_batch_item_saved, itemNumber, total, saveResult.filename)
    } else {
        getString(R.string.result_batch_item_save_failed, itemNumber, total)
    }
}
