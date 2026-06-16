package com.haanghil.muulnaat

internal fun modelFaceSuppressionScore(originalFaceCount: Int, protectedFaceCount: Int): Double {
    if (originalFaceCount <= 0) return 0.0
    return ((originalFaceCount - protectedFaceCount).toDouble() / originalFaceCount).coerceIn(0.0, 1.0)
}

internal fun modelFeatureSuppressionScore(originalFeatureCount: Int, protectedFeatureCount: Int): Double {
    if (originalFeatureCount <= 0) return 0.0
    return ((originalFeatureCount - protectedFeatureCount).toDouble() / originalFeatureCount).coerceIn(0.0, 1.0)
}

internal fun modelLabelShift(originalLabels: List<String>, protectedLabels: List<String>): Double {
    if (originalLabels.isEmpty()) return 0.0
    val originalSet = originalLabels.toSet()
    val intersection = originalSet.intersect(protectedLabels.toSet()).size
    return 1.0 - (intersection.toDouble() / originalSet.size)
}

internal fun modelAntiDetectionScore(
    faceSuppression: Double,
    facialFeatureSuppression: Double,
    labelShift: Double,
): Double {
    return ModelProbe.FACE_SUPPRESSION_WEIGHT * faceSuppression +
        ModelProbe.FACIAL_FEATURE_SUPPRESSION_WEIGHT * facialFeatureSuppression +
        ModelProbe.LABEL_SHIFT_WEIGHT * labelShift
}

internal fun modelProbeShouldPass(
    antiDetectionScore: Double,
    originalFaceCount: Int,
    originalFeatureCount: Int,
    protectedFeatureCount: Int,
): Boolean {
    return antiDetectionScore >= ModelProbe.PASS_THRESHOLD &&
        hasFeatureBaseline(originalFaceCount, originalFeatureCount) &&
        protectedFeatureCount == 0
}
