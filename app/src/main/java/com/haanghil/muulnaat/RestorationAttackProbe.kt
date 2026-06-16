package com.haanghil.muulnaat

import android.graphics.Bitmap
import java.util.Locale

class RestorationAttackProbe : DefenseEvaluator {
    override fun evaluateAfterAttack(original: Bitmap, protected: Bitmap): DefenseEvaluationReport {
        val attackedBitmap = RedTeamEngine.simulateAttack(protected)
        val modelResult = ModelProbe.evaluate(original, attackedBitmap)
        val imageResult = ImageMetrics.evaluate(original, attackedBitmap)

        val status = if (modelResult.passed) {
            ProtectionStatus.HELD
        } else {
            ProtectionStatus.BROKEN
        }

        return DefenseEvaluationReport(
            status = status,
            attackedBitmap = attackedBitmap,
            evaluationMetrics = EvaluationMetrics(
                faceCountOriginal = modelResult.faceCountOriginal,
                faceCountAfterAttack = modelResult.faceCountProtected,
                facialFeatureCountOriginal = modelResult.facialFeatureCountOriginal,
                facialFeatureCountAfterAttack = modelResult.facialFeatureCountProtected,
                facialFeatureSuppressionScore = modelResult.facialFeatureSuppressionScore,
                labelShift = modelResult.labelShift,
                antiDetectionScore = modelResult.antiDetectionScore,
                decisionReason = modelResult.decisionReason
            ),
            qualityMetrics = QualityMetrics(
                psnr = imageResult.psnr,
                meanAbsDelta = imageResult.meanAbsDelta,
                edgeDelta = imageResult.edgeDelta
            ),
            summary = buildString {
                append("Defense Evaluation: ")
                append(if (status == ProtectionStatus.HELD) "HELD" else "BROKEN")
                append(" | Score=")
                append(String.format(Locale.US, "%.2f", modelResult.antiDetectionScore))
                append(" | Features=")
                append(modelResult.facialFeatureCountOriginal)
                append("->")
                append(modelResult.facialFeatureCountProtected)
                append(" | ")
                append(modelResult.decisionReason)
            }
        )
    }
}
