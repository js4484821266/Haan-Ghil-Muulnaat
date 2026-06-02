package com.haanghil.muulnaat

import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

internal fun MainActivity.configureUiActions() {
    binding.helpButton.setOnClickListener { showManualDialog() }
    setTechnicalDetailsVisible(false)
    binding.technicalDetailsToggle.setOnClickListener {
        setTechnicalDetailsVisible(binding.technicalDetailsContainer.visibility != android.view.View.VISIBLE)
    }

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

    binding.applyButton.setOnClickListener { applyCurrentStrength() }
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

private fun MainActivity.applyCurrentStrength() {
    val source = state.originalBitmap
    if (source == null) {
        Toast.makeText(this, getString(R.string.toast_pick_image_first), Toast.LENGTH_SHORT).show()
        return
    }

    val strength = binding.strengthSeekBar.progress
    runProtectionFlow(
        source = source,
        strength = strength,
        autoRecovery = binding.autoRecoverySwitch.isChecked,
        clearResultsBeforeRun = state.lastEvaluationStrength != null && state.lastEvaluationStrength != strength,
        startMessage = getString(R.string.result_applying_protection)
    )
}

private fun MainActivity.runManualDefenseEvaluation() {
    val source = state.protectedBitmap
    val original = state.originalBitmap
    if (source == null || original == null) {
        Toast.makeText(this, getString(R.string.toast_apply_protection_first), Toast.LENGTH_SHORT).show()
        return
    }

    binding.resultText.text = getString(R.string.result_running_recovery)
    setBusy(true, getString(R.string.result_running_recovery))
    kotlin.concurrent.thread {
        val defenseReport = defenseEvaluator.evaluateAfterAttack(original, source)
        runOnUiThread {
            renderRecoveredImage(defenseReport.attackedBitmap)
            renderDefenseResult(defenseReport.status, defenseReport.evaluationMetrics, defenseReport.qualityMetrics)
            state.lastEvaluationStrength = state.lastAppliedStrength ?: binding.strengthSeekBar.progress
            binding.resultText.text = getString(R.string.result_recovery_complete, statusLabel(defenseReport.status))
            setBusy(false)
        }
    }
}

private fun MainActivity.applyRememberedOptimalStrength() {
    val rememberedStrength = state.optimalStrength
    if (rememberedStrength == null) {
        Toast.makeText(this, getString(R.string.toast_no_optimal_strength), Toast.LENGTH_SHORT).show()
        return
    }

    val source = state.originalBitmap
    if (source == null) {
        Toast.makeText(this, getString(R.string.toast_pick_image_first), Toast.LENGTH_SHORT).show()
        return
    }

    binding.strengthSeekBar.progress = rememberedStrength
    binding.strengthLabel.text = getString(R.string.noise_strength_value, rememberedStrength)
    val msg = if (binding.autoRecoverySwitch.isChecked) {
        getString(R.string.result_reset_optimal_auto, rememberedStrength)
    } else {
        getString(R.string.result_reset_optimal, rememberedStrength)
    }
    runProtectionFlow(source, rememberedStrength, binding.autoRecoverySwitch.isChecked, true, msg)
}
