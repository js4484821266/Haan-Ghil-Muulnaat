package com.haanghil.muulnaat

import android.graphics.Bitmap
import kotlin.concurrent.thread

internal fun MainActivity.runProtectionFlow(
    source: Bitmap,
    strength: Int,
    config: HidingConfig,
    autoEvaluate: Boolean,
    clearResultsBeforeRun: Boolean,
    startMessage: String,
    onComplete: ((Bitmap) -> Unit)? = null,
) {
    if (clearResultsBeforeRun) clearResultCards()

    binding.resultText.text = startMessage
    setBusy(true, startMessage)

    thread {
        val faceRegions = FaceRegionDetector.detectRegions(source)
        val output = perturbationModule.applyProtection(source, strength, faceRegions, config)
        val defenseReport = if (autoEvaluate) {
            HidingEvaluation.evaluate(source, output, config, defenseEvaluator)
        } else {
            null
        }

        runOnUiThread {
            state.protectedBitmap = output
            state.lastAppliedStrength = strength
            state.lastAppliedMethod = config.method
            renderProtectedImage(source, output)
            if (defenseReport == null) {
                renderRecoveredImage(null)
                binding.resultText.text = if (config.method == HidingMethod.SOLID_FILL) {
                    getString(R.string.result_solid_fill_applied)
                } else {
                    getString(R.string.result_protection_applied, strength)
                }
            } else {
                renderRecoveredImage(if (config.method == HidingMethod.NOISE) defenseReport.attackedBitmap else null)
                renderDefenseResult(defenseReport.status, defenseReport.evaluationMetrics, defenseReport.qualityMetrics)
                state.lastEvaluationStrength = strength
                binding.resultText.text = getString(
                    R.string.result_auto_evaluation_complete_for_method,
                    methodLabel(config.method),
                    strength,
                    statusLabel(defenseReport.status),
                )
            }
            setBusy(false)
            onComplete?.invoke(output)
        }
    }
}
