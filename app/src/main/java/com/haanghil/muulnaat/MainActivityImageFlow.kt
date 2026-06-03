package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.net.Uri
import kotlin.concurrent.thread

internal fun MainActivity.processSingleImageUri(uri: Uri, autoSaveAfterProtection: Boolean) {
    val loaded = ImageStore.loadBitmapFromUri(this, uri)
    if (loaded == null) {
        binding.resultText.text = getString(R.string.result_load_failed)
        return
    }

    prepareLoadedImage(loaded)
    startOptimalStrengthFlow(loaded, autoSaveAfterProtection)
}

internal fun MainActivity.prepareLoadedImage(loaded: Bitmap) {
    state.originalBitmap = loaded
    state.protectedBitmap = null
    state.optimalStrength = null
    state.lastAppliedStrength = null
    state.lastEvaluationStrength = null
    binding.originalImage.setImageBitmap(loaded)
    binding.noisyImage.setImageDrawable(null)
    binding.recoveredImage.setImageDrawable(null)
    binding.perturbationSummaryText.text = getString(R.string.perturbation_summary_placeholder)
    binding.recommendedStrengthText.text = getString(R.string.recommended_strength_default)
    showSearchProgress(getString(R.string.result_scanning_optimal))
    binding.resetOptimalButton.isEnabled = false
    clearResultCards()
}

internal fun MainActivity.startOptimalStrengthFlow(source: Bitmap, autoSaveAfterProtection: Boolean) {
    binding.resultText.text = getString(R.string.result_image_loaded_scanning)
    setBusy(true, getString(R.string.result_scanning_optimal))
    // ML Kit 호출은 Tasks.await()로 블로킹되므로 강도 탐색은 UI 스레드 밖에서 실행합니다.
    thread {
        val minStrength = StrengthAdvisor.findRecommendedStrength(
            original = source,
            perturbationModule = perturbationModule,
            defenseEvaluator = defenseEvaluator,
            onStep = { step -> runOnUiThread { renderSearchStep(step) } },
        )
        runOnUiThread {
            state.optimalStrength = minStrength
            binding.recommendedStrengthText.text = StrengthAdvisor.recommendationText(this, minStrength)
            if (minStrength == null) {
                showSearchProgress(getString(R.string.result_scan_none))
                binding.resetOptimalButton.isEnabled = false
                binding.resultText.text = getString(R.string.result_scan_none)
                setBusy(false)
            } else {
                startProtectionFromRecommendedStrength(source, minStrength, autoSaveAfterProtection)
            }
        }
    }
}

private fun MainActivity.renderSearchStep(step: NoiseSearcher.SearchStep) {
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

private fun MainActivity.startProtectionFromRecommendedStrength(
    source: Bitmap,
    minStrength: Int,
    autoSaveAfterProtection: Boolean,
) {
    showOptimalActionButton()
    binding.resetOptimalButton.isEnabled = true
    binding.strengthSeekBar.progress = minStrength
    binding.strengthLabel.text = getString(R.string.noise_strength_value, minStrength)
    val isAutoRecoveryOn = binding.autoRecoverySwitch.isChecked
    val msg = if (isAutoRecoveryOn) {
        getString(R.string.result_auto_applying_optimal, minStrength)
    } else {
        getString(R.string.result_scan_found, minStrength)
    }
    // 공유로 들어온 이미지는 추천 강도 적용 후 수동 저장 탭을 건너뛸 수 있습니다.
    runProtectionFlow(source, minStrength, isAutoRecoveryOn, true, msg) { protected ->
        if (autoSaveAfterProtection) saveImageToGalleryAsync(protected)
    }
}
