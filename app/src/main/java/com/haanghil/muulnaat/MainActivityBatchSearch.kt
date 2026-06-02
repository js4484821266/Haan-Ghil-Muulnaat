package com.haanghil.muulnaat

import android.graphics.Bitmap

/**
 * Minimum-strength search for one item in a visible batch.
 *
 * The search itself is shared with the single-image path; this wrapper only
 * translates progress and failure into main-screen UI updates.
 */
internal fun MainActivity.findBatchStrength(
    loaded: Bitmap,
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
        getString(
            R.string.search_progress_step,
            step.iteration,
            step.low,
            step.mid,
            step.high,
            statusLabelForSearchStep(step),
        )
    )
}
