package com.haanghil.muulnaat

import android.graphics.Bitmap
import kotlin.concurrent.thread

internal fun MainActivity.runProtectionFlow(
    source: Bitmap,
    strength: Int,
    autoRecovery: Boolean,
    clearResultsBeforeRun: Boolean,
    startMessage: String,
    onComplete: ((Bitmap) -> Unit)? = null,
) {
    if (clearResultsBeforeRun) clearResultCards()

    binding.resultText.text = startMessage
    setBusy(true, startMessage)

    thread {
        val output = perturbationModule.applyProtection(source, strength)
        val defenseReport = if (autoRecovery) defenseEvaluator.evaluateAfterAttack(source, output) else null

        runOnUiThread {
            state.protectedBitmap = output
            state.lastAppliedStrength = strength
            renderProtectedImage(source, output)
            if (defenseReport == null) {
                renderRecoveredImage(null)
                binding.resultText.text = getString(R.string.result_protection_applied, strength)
            } else {
                renderRecoveredImage(defenseReport.attackedBitmap)
                renderDefenseResult(defenseReport.status, defenseReport.evaluationMetrics, defenseReport.qualityMetrics)
                state.lastEvaluationStrength = strength
                binding.resultText.text =
                    getString(R.string.result_auto_recovery_complete, strength, statusLabel(defenseReport.status))
            }
            setBusy(false)
            onComplete?.invoke(output)
        }
    }
}
