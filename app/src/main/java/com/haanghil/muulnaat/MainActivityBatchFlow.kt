package com.haanghil.muulnaat

import android.net.Uri
import kotlin.concurrent.thread

internal fun MainActivity.processImageBatch(uris: List<Uri>) {
    if (uris.isEmpty()) return

    resetBatchUi(uris.size)
    // Batch mode reuses the same protection pipeline while keeping progress visible on the main screen.
    thread {
        var savedCount = 0
        var skippedCount = 0

        uris.forEachIndexed { index, uri ->
            val result = processBatchItem(index + 1, uris.size, uri)
            if (result) savedCount += 1 else skippedCount += 1
        }

        runOnUiThread {
            showSearchProgress(getString(R.string.result_batch_complete, savedCount, skippedCount))
            binding.resultText.text = getString(R.string.result_batch_complete, savedCount, skippedCount)
            setBusy(false)
        }
    }
}

private fun MainActivity.resetBatchUi(total: Int) {
    state.originalBitmap = null
    state.protectedBitmap = null
    state.optimalStrength = null
    state.lastAppliedStrength = null
    state.lastEvaluationStrength = null
    clearResultCards()
    binding.noisyImage.setImageDrawable(null)
    binding.recoveredImage.setImageDrawable(null)
    binding.perturbationSummaryText.text = getString(R.string.perturbation_summary_placeholder)
    binding.recommendedStrengthText.text = getString(R.string.recommended_strength_default)
    showSearchProgress(getString(R.string.result_batch_start, total))
    setBusy(true, getString(R.string.result_batch_start, total))
}

private fun MainActivity.processBatchItem(itemNumber: Int, total: Int, uri: Uri): Boolean {
    runOnUiThread {
        binding.resultText.text = getString(R.string.result_batch_item_processing, itemNumber, total)
        showSearchProgress(getString(R.string.result_batch_item_processing, itemNumber, total))
    }

    val loaded = ImageStore.loadBitmapFromUri(this, uri)
    if (loaded == null) {
        runOnUiThread {
            binding.resultText.text = getString(R.string.result_batch_item_load_failed, itemNumber, total)
        }
        return false
    }

    // The latest item is mirrored in the UI so batch processing remains inspectable instead of opaque.
    runOnUiThread {
        prepareLoadedImage(loaded)
        setBusy(true, getString(R.string.result_batch_item_processing, itemNumber, total))
    }

    val minStrength = findBatchStrength(loaded, itemNumber, total) ?: return false
    val protected = perturbationModule.applyProtection(loaded, minStrength)
    val defenseReport = if (binding.autoRecoverySwitch.isChecked) {
        defenseEvaluator.evaluateAfterAttack(loaded, protected)
    } else {
        null
    }
    val saveResult = saveImageToGallery(protected)

    runOnUiThread {
        renderBatchResult(loaded, protected, minStrength, defenseReport, saveResult, itemNumber, total)
    }
    return saveResult.success
}

private fun MainActivity.findBatchStrength(
    loaded: android.graphics.Bitmap,
    itemNumber: Int,
    total: Int,
): Int? {
    val minStrength = StrengthAdvisor.findRecommendedStrength(
        original = loaded,
        perturbationModule = perturbationModule,
        defenseEvaluator = defenseEvaluator,
        onStep = { step -> runOnUiThread { renderBatchSearchStep(step) } },
    )
    if (minStrength == null) {
        runOnUiThread {
            state.optimalStrength = null
            binding.recommendedStrengthText.text = StrengthAdvisor.recommendationText(this, null)
            showSearchProgress(getString(R.string.result_scan_none))
            binding.resultText.text = getString(R.string.result_batch_item_scan_failed, itemNumber, total)
        }
    }
    return minStrength
}

private fun MainActivity.renderBatchSearchStep(step: NoiseSearcher.SearchStep) {
    binding.strengthSeekBar.progress = step.mid
    binding.strengthLabel.text = getString(R.string.noise_strength_value, step.mid)
    showSearchProgress(
        getString(R.string.search_progress_step, step.iteration, step.low, step.mid, step.high, statusLabelForSearchStep(step))
    )
}

private fun MainActivity.renderBatchResult(
    loaded: android.graphics.Bitmap,
    protected: android.graphics.Bitmap,
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
    binding.resultText.text = if (saveResult.success) {
        getString(R.string.result_batch_item_saved, itemNumber, total, saveResult.filename)
    } else {
        getString(R.string.result_batch_item_save_failed, itemNumber, total)
    }
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
