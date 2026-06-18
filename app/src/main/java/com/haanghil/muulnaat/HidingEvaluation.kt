package com.haanghil.muulnaat

import android.graphics.Bitmap
import java.util.Locale

object HidingEvaluation {
    fun evaluate(
        original: Bitmap,
        protected: Bitmap,
        config: HidingConfig,
        defenseEvaluator: DefenseEvaluator,
    ): DefenseEvaluationReport? {
        return when (config.method) {
            HidingMethod.NOISE -> defenseEvaluator.evaluateAfterAttack(original, protected)
            HidingMethod.BLUR -> evaluateBlurFeatureSuppression(original, protected)
            HidingMethod.SOLID_FILL -> null
        }
    }

    fun passesSearch(
        original: Bitmap,
        protected: Bitmap,
        config: HidingConfig,
        defenseEvaluator: DefenseEvaluator,
    ): Boolean {
        return when (config.method) {
            HidingMethod.NOISE -> defenseEvaluator.evaluateAfterAttack(original, protected).status == ProtectionStatus.HELD
            HidingMethod.BLUR -> blurFeatureSuppressionPassed(ModelProbe.evaluate(original, protected))
            HidingMethod.SOLID_FILL -> false
        }
    }

    private fun evaluateBlurFeatureSuppression(original: Bitmap, protected: Bitmap): DefenseEvaluationReport {
        val modelResult = ModelProbe.evaluate(original, protected)
        val imageResult = ImageMetrics.evaluate(original, protected)
        val passed = blurFeatureSuppressionPassed(modelResult)
        val status = if (passed) ProtectionStatus.HELD else ProtectionStatus.BROKEN
        val reason = blurDecisionReason(modelResult, passed)

        return DefenseEvaluationReport(
            status = status,
            attackedBitmap = protected,
            evaluationMetrics = EvaluationMetrics(
                faceCountOriginal = modelResult.faceCountOriginal,
                faceCountAfterAttack = modelResult.faceCountProtected,
                facialFeatureCountOriginal = modelResult.facialFeatureCountOriginal,
                facialFeatureCountAfterAttack = modelResult.facialFeatureCountProtected,
                facialFeatureSuppressionScore = modelResult.facialFeatureSuppressionScore,
                labelShift = modelResult.labelShift,
                antiDetectionScore = modelResult.antiDetectionScore,
                decisionReason = reason,
            ),
            qualityMetrics = QualityMetrics(
                psnr = imageResult.psnr,
                meanAbsDelta = imageResult.meanAbsDelta,
                edgeDelta = imageResult.edgeDelta,
            ),
            summary = buildString {
                append("Feature Evaluation: ")
                append(if (status == ProtectionStatus.HELD) "HELD" else "BROKEN")
                append(" | Features=")
                append(modelResult.facialFeatureCountOriginal)
                append("->")
                append(modelResult.facialFeatureCountProtected)
                append(" | Score=")
                append(String.format(Locale.US, "%.2f", modelResult.antiDetectionScore))
                append(" | ")
                append(reason)
            },
        )
    }

    private fun blurFeatureSuppressionPassed(result: ModelProbeResult): Boolean {
        return result.facialFeatureCountOriginal > 0 && result.facialFeatureCountProtected == 0
    }

    private fun blurDecisionReason(result: ModelProbeResult, passed: Boolean): String {
        return if (passed) {
            "PASS: protected image has no detected facial features"
        } else if (result.facialFeatureCountOriginal <= 0) {
            "FAIL: original facial feature baseline missing"
        } else {
            "FAIL: facial features still detected on protected image"
        }
    }
}
