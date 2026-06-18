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
    val method = state.selectedMethod
    if (method?.supportsStrengthSearch() == true) {
        startOptimalStrengthFlow(loaded, autoSaveAfterProtection, method)
    } else if (autoSaveAfterProtection) {
        val fallbackMethod = HidingMethod.NOISE
        state.selectedMethod = fallbackMethod
        startOptimalStrengthFlow(loaded, autoSaveAfterProtection, fallbackMethod)
    }
}

internal fun MainActivity.prepareLoadedImage(loaded: Bitmap) {
    state.originalBitmap = loaded
    state.protectedBitmap = null
    state.optimalStrength = null
    state.noiseOptimalStrength = null
    state.blurOptimalStrength = null
    state.lastAppliedStrength = null
    state.lastAppliedMethod = null
    state.lastEvaluationStrength = null
    binding.originalImage.setImageBitmap(loaded)
    binding.noisyImage.setImageDrawable(null)
    binding.recoveredImage.setImageDrawable(null)
    binding.perturbationSummaryText.text = getString(R.string.perturbation_summary_placeholder)
    binding.recommendedStrengthText.text = if (state.selectedMethod == HidingMethod.SOLID_FILL) {
        getString(R.string.recommended_strength_solid_fill)
    } else {
        getString(R.string.recommended_strength_waiting_for_method)
    }
    showSearchProgress(getString(R.string.search_progress_waiting_for_method))
    binding.resetOptimalButton.isEnabled = false
    clearResultCards()
}

internal fun MainActivity.startOptimalStrengthFlow(
    source: Bitmap,
    autoSaveAfterProtection: Boolean,
    method: HidingMethod,
) {
    if (!method.supportsStrengthSearch()) return

    val config = configForMethod(method)
    binding.resultText.text = getString(R.string.result_image_loaded_scanning_for_method, methodLabel(method))
    setBusy(true, getString(R.string.result_scanning_optimal_for_method, methodLabel(method)))
    // ML Kit 호출은 Tasks.await()로 블로킹되므로 강도 탐색은 UI 스레드 밖에서 실행합니다.
    thread {
        val minStrength = StrengthAdvisor.findRecommendedStrength(
            original = source,
            perturbationModule = perturbationModule,
            defenseEvaluator = defenseEvaluator,
            config = config,
            onStep = { step -> runOnUiThread { renderSearchStep(step) } },
        )
        runOnUiThread {
            storeOptimalStrength(method, minStrength)
            binding.recommendedStrengthText.text = StrengthAdvisor.recommendationText(this, minStrength)
            if (minStrength == null) {
                showSearchProgress(getString(R.string.result_scan_none))
                binding.resetOptimalButton.isEnabled = false
                binding.resultText.text = getString(R.string.result_scan_none)
                setBusy(false)
            } else {
                startProtectionFromRecommendedStrength(source, minStrength, autoSaveAfterProtection, config)
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
    config: HidingConfig,
) {
    showOptimalActionButton()
    binding.resetOptimalButton.isEnabled = true
    binding.strengthSeekBar.progress = minStrength
    binding.strengthLabel.text = getString(R.string.noise_strength_value, minStrength)
    val isAutoRecoveryOn = binding.autoRecoverySwitch.isChecked
    val msg = if (isAutoRecoveryOn) {
        getString(R.string.result_auto_applying_optimal_for_method, methodLabel(config.method), minStrength)
    } else {
        getString(R.string.result_scan_found, minStrength)
    }
    // 공유로 들어온 이미지는 추천 강도 적용 후 수동 저장 탭을 건너뛸 수 있습니다.
    runProtectionFlow(source, minStrength, config, isAutoRecoveryOn, true, msg) { protected ->
        if (autoSaveAfterProtection) saveImageToGalleryAsync(protected)
    }
}
