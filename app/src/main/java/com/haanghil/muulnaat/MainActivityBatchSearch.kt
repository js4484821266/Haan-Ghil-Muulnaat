package com.haanghil.muulnaat

import android.graphics.Bitmap

/**
 * 화면에 보이는 일괄 처리 항목 하나의 최소 강도를 찾습니다.
 *
 * 탐색 자체는 단일 이미지 경로와 공유합니다. 이 래퍼는 진행 상황과 실패 상태를
 * 메인 화면 UI 업데이트로 바꾸는 역할만 합니다.
 */
internal fun MainActivity.findBatchStrength(
    loaded: Bitmap,
    itemNumber: Int,
    total: Int,
    config: HidingConfig,
): Int? {
    val minStrength = StrengthAdvisor.findRecommendedStrength(
        original = loaded,
        perturbationModule = perturbationModule,
        defenseEvaluator = defenseEvaluator,
        config = config,
        onStep = { step -> runOnUiThread { renderBatchSearchStep(step) } },
    )
    if (minStrength == null) {
        runOnUiThread {
            storeOptimalStrength(config.method, null)
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
