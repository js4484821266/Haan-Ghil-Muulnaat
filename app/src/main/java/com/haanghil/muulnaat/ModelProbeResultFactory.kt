package com.haanghil.muulnaat

internal fun buildModelProbeResult(
    originalFaceCount: Int,
    protectedFaceCount: Int,
    originalFeatureCount: Int,
    protectedFeatureCount: Int,
    faceSuppression: Double,
    featureSuppression: Double,
    labelShift: Double,
    antiDetectionScore: Double,
): ModelProbeResult {
    val pass = ModelProbe.shouldPass(
        antiDetectionScore,
        originalFaceCount,
        originalFeatureCount,
        protectedFeatureCount,
    )
    val reason = modelProbeDecisionReason(
        pass,
        originalFaceCount,
        originalFeatureCount,
        protectedFeatureCount,
        antiDetectionScore,
    )
    return ModelProbeResult(
        faceCountOriginal = originalFaceCount,
        faceCountProtected = protectedFaceCount,
        facialFeatureCountOriginal = originalFeatureCount,
        facialFeatureCountProtected = protectedFeatureCount,
        faceSuppressionScore = faceSuppression,
        facialFeatureSuppressionScore = featureSuppression,
        labelShift = labelShift,
        antiDetectionScore = antiDetectionScore,
        passed = pass,
        decisionReason = reason,
        details = modelProbeDetails(
            originalFaceCount,
            protectedFaceCount,
            originalFeatureCount,
            protectedFeatureCount,
            faceSuppression,
            featureSuppression,
            labelShift,
            antiDetectionScore,
            reason,
        ),
    )
}
