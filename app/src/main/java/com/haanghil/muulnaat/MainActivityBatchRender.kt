package com.haanghil.muulnaat

import android.graphics.Bitmap

/**
 * UI rendering for the latest completed batch item.
 *
 * Batch mode intentionally reuses the single-image preview panels. That keeps
 * the user informed without creating a separate batch-results screen.
 */
internal fun MainActivity.renderBatchResult(
    loaded: Bitmap,
    protected: Bitmap,
    minStrength: Int,
    defenseReport: DefenseEvaluationReport?,
    saveResult: GallerySaveResult,
    itemNumber: Int,
    total: Int,
) {
    state.originalBitmap = loaded
    state.protectedBitmap = protected
    state.optimalStrength = minStrength
    state.lastAppliedStrength = minStrength
    binding.recommendedStrengthText.text = StrengthAdvisor.recommendationText(this, minStrength)
    binding.strengthSeekBar.progress = minStrength
    binding.strengthLabel.text = getString(R.string.noise_strength_value, minStrength)
    showOptimalActionButton()
    renderProtectedImage(loaded, protected)
    renderBatchDefense(defenseReport, minStrength)
    binding.resultText.text = batchSaveMessage(saveResult, itemNumber, total)
}

private fun MainActivity.renderBatchDefense(defenseReport: DefenseEvaluationReport?, minStrength: Int) {
    if (defenseReport == null) {
        renderRecoveredImage(null)
    } else {
        renderRecoveredImage(defenseReport.attackedBitmap)
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
