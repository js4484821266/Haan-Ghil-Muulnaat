package com.haanghil.muulnaat

import android.graphics.Bitmap

data class ModelProbeResult(
    val faceCountOriginal: Int,
    val faceCountProtected: Int,
    val facialFeatureCountOriginal: Int,
    val facialFeatureCountProtected: Int,
    val faceSuppressionScore: Double,
    val facialFeatureSuppressionScore: Double,
    val labelShift: Double,
    val antiDetectionScore: Double,
    val passed: Boolean,
    val decisionReason: String,
    val details: String
)

/**
 * 로컬 ML Kit 기반 모델 평가기입니다.
 *
 * 이것은 보편적인 보안 판정기가 아닙니다. 얼굴 특징 억제와 이미지 라벨 변화를 합쳐
 * 현재 이미지에 perturbation이 충분했는지 앱 안에서 판단하기 위한 실용적인 신호입니다.
 */
object ModelProbe {
    internal const val FACE_SUPPRESSION_WEIGHT = 0.20
    internal const val FACIAL_FEATURE_SUPPRESSION_WEIGHT = 0.50
    internal const val LABEL_SHIFT_WEIGHT = 0.30
    internal const val PASS_THRESHOLD = 0.50

    fun evaluate(original: Bitmap, protected: Bitmap): ModelProbeResult =
        evaluateModelProbe(original, protected)

    internal fun computeFaceSuppressionScore(originalFaceCount: Int, protectedFaceCount: Int): Double {
        return modelFaceSuppressionScore(originalFaceCount, protectedFaceCount)
    }

    internal fun computeFacialFeatureSuppressionScore(
        originalFeatureCount: Int,
        protectedFeatureCount: Int,
    ): Double {
        return modelFeatureSuppressionScore(originalFeatureCount, protectedFeatureCount)
    }

    internal fun computeLabelShift(originalLabels: List<String>, protectedLabels: List<String>): Double {
        return modelLabelShift(originalLabels, protectedLabels)
    }

    internal fun computeAntiDetectionScore(
        faceSuppression: Double,
        facialFeatureSuppression: Double,
        labelShift: Double,
    ): Double {
        return modelAntiDetectionScore(faceSuppression, facialFeatureSuppression, labelShift)
    }

    internal fun shouldPass(
        antiDetectionScore: Double,
        originalFaceCount: Int,
        originalFeatureCount: Int,
        protectedFeatureCount: Int,
    ): Boolean {
        return modelProbeShouldPass(
            antiDetectionScore,
            originalFaceCount,
            originalFeatureCount,
            protectedFeatureCount,
        )
    }
}
