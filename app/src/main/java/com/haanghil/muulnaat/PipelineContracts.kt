package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.graphics.Rect

enum class ProtectionStatus {
    PASS,
    BROKEN,
    HELD
}

data class EvaluationMetrics(
    val faceCountOriginal: Int,
    val faceCountAfterAttack: Int,
    val labelShift: Double,
    val antiDetectionScore: Double,
    val decisionReason: String
)

data class QualityMetrics(
    val psnr: Double,
    val meanAbsDelta: Double,
    val edgeDelta: Double
)

data class DefenseEvaluationReport(
    val status: ProtectionStatus,
    val attackedBitmap: Bitmap,
    val evaluationMetrics: EvaluationMetrics,
    val qualityMetrics: QualityMetrics,
    val summary: String
)

interface PerturbationModule {
    fun applyProtection(source: Bitmap, strength: Int, regions: List<Rect>? = null): Bitmap
}

interface DefenseEvaluator {
    fun evaluateAfterAttack(original: Bitmap, protected: Bitmap): DefenseEvaluationReport
}
