package com.haanghil.muulnaat

import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

/**
 * UI 컨트롤을 MainActivity 흐름에 연결합니다.
 *
 * 무거운 작업은 흐름 파일에 두고, 이 파일은 이미지 처리 세부사항을 섞지 않은 채
 * "각 버튼이 무엇을 하는가?"만 보여 줍니다.
 */
internal fun MainActivity.configureUiActions() {
    binding.helpButton.setOnClickListener { showManualDialog() }
    setTechnicalDetailsVisible(false)
    binding.technicalDetailsToggle.setOnClickListener {
        setTechnicalDetailsVisible(binding.technicalDetailsContainer.visibility != android.view.View.VISIBLE)
    }

    configureHidingMethodControls()

    binding.pickButton.setOnClickListener {
        pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    binding.strengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            binding.strengthLabel.text = getString(R.string.noise_strength_value, progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    })

    binding.applyButton.setOnClickListener { applyCurrentProtection() }
    binding.attackButton.setOnClickListener { runManualDefenseEvaluation() }
    binding.resetOptimalButton.setOnClickListener { applyRememberedOptimalStrength() }
    binding.saveButton.setOnClickListener {
        if (state.protectedBitmap == null) {
            Toast.makeText(this, getString(R.string.toast_apply_protection_first), Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }
        runWithStoragePermissionIfNeeded { saveProtectedImageToGallery() }
    }
}

private fun MainActivity.applyCurrentProtection() {
    val config = currentHidingConfigOrNull() ?: return
    val source = state.originalBitmap
    if (source == null) {
        Toast.makeText(this, getString(R.string.toast_pick_image_first), Toast.LENGTH_SHORT).show()
        return
    }

    val strength = if (config.method == HidingMethod.SOLID_FILL) 0 else binding.strengthSeekBar.progress
    val clearResults = state.lastEvaluationStrength != null &&
        (state.lastEvaluationStrength != strength || state.lastAppliedMethod != config.method)
    runProtectionFlow(
        source = source,
        strength = strength,
        config = config,
        autoEvaluate = binding.autoRecoverySwitch.isChecked && config.method != HidingMethod.SOLID_FILL,
        clearResultsBeforeRun = clearResults,
        startMessage = getString(R.string.result_applying_protection)
    )
}

private fun MainActivity.runManualDefenseEvaluation() {
    val config = currentHidingConfigOrNull() ?: return
    if (config.method == HidingMethod.SOLID_FILL) {
        Toast.makeText(this, getString(R.string.toast_solid_fill_no_evaluation), Toast.LENGTH_SHORT).show()
        return
    }

    val source = state.protectedBitmap
    val original = state.originalBitmap
    if (source == null || original == null) {
        Toast.makeText(this, getString(R.string.toast_apply_protection_first), Toast.LENGTH_SHORT).show()
        return
    }

    binding.resultText.text = getString(R.string.result_running_recovery_for_method, methodLabel(config.method))
    setBusy(true, getString(R.string.result_running_recovery_for_method, methodLabel(config.method)))
    // 평가는 방법별로 다릅니다. Noising은 복원 공격까지, Blurring은 보호 이미지의 얼굴 특징만 봅니다.
    kotlin.concurrent.thread {
        val defenseReport = HidingEvaluation.evaluate(original, source, config, defenseEvaluator)
        runOnUiThread {
            if (defenseReport != null) {
                renderRecoveredImage(if (config.method == HidingMethod.NOISE) defenseReport.attackedBitmap else null)
                renderDefenseResult(defenseReport.status, defenseReport.evaluationMetrics, defenseReport.qualityMetrics)
                state.lastEvaluationStrength = state.lastAppliedStrength ?: binding.strengthSeekBar.progress
                binding.resultText.text = getString(
                    R.string.result_evaluation_complete_for_method,
                    methodLabel(config.method),
                    statusLabel(defenseReport.status),
                )
            }
            setBusy(false)
        }
    }
}

private fun MainActivity.applyRememberedOptimalStrength() {
    val method = state.selectedMethod
    if (method == null || !method.supportsStrengthSearch()) {
        Toast.makeText(this, getString(R.string.toast_no_optimal_strength), Toast.LENGTH_SHORT).show()
        return
    }

    val rememberedStrength = rememberedOptimalStrength(method)
    if (rememberedStrength == null) {
        Toast.makeText(this, getString(R.string.toast_no_optimal_strength), Toast.LENGTH_SHORT).show()
        return
    }

    val source = state.originalBitmap
    if (source == null) {
        Toast.makeText(this, getString(R.string.toast_pick_image_first), Toast.LENGTH_SHORT).show()
        return
    }

    val config = configForMethod(method)
    binding.strengthSeekBar.progress = rememberedStrength
    binding.strengthLabel.text = getString(R.string.noise_strength_value, rememberedStrength)
    // 기억해 둔 강도를 다시 적용하면 사용자가 슬라이더를 손으로 바꾼 뒤에도
    // 탐색 결과를 바로 실행 가능한 값으로 되돌릴 수 있습니다.
    val msg = if (binding.autoRecoverySwitch.isChecked) {
        getString(R.string.result_reset_optimal_auto, rememberedStrength)
    } else {
        getString(R.string.result_reset_optimal, rememberedStrength)
    }
    runProtectionFlow(
        source = source,
        strength = rememberedStrength,
        config = config,
        autoEvaluate = binding.autoRecoverySwitch.isChecked,
        clearResultsBeforeRun = true,
        startMessage = msg,
    )
}

internal fun MainActivity.methodLabel(method: HidingMethod): String = when (method) {
    HidingMethod.NOISE -> getString(R.string.hiding_method_noise)
    HidingMethod.BLUR -> getString(R.string.hiding_method_blur)
    HidingMethod.SOLID_FILL -> getString(R.string.hiding_method_solid_fill)
}
