package com.haanghil.muulnaat

import java.util.Locale

internal fun modelProbeDecisionReason(
    pass: Boolean,
    originalFaceCount: Int,
    originalFeatureCount: Int,
    protectedFeatureCount: Int,
    antiDetectionScore: Double,
): String {
    return when {
        pass -> "PASS: facial features suppressed after restoration attack " +
            "(score=${formatProbeNumber(antiDetectionScore)})"
        !hasFeatureBaseline(originalFaceCount, originalFeatureCount) ->
            "FAIL: original face/feature baseline missing " +
                "(faces=$originalFaceCount, features=$originalFeatureCount)"
        protectedFeatureCount > 0 ->
            "FAIL: facial features still detected after attack " +
                "(features: $originalFeatureCount -> $protectedFeatureCount)"
        else -> "FAIL: insufficient feature disruption " +
            "(score=${formatProbeNumber(antiDetectionScore)}, " +
            "need>=${formatProbeNumber(ModelProbe.PASS_THRESHOLD)})"
    }
}

internal fun modelProbeDetails(
    originalFaceCount: Int,
    protectedFaceCount: Int,
    originalFeatureCount: Int,
    protectedFeatureCount: Int,
    faceSuppression: Double,
    featureSuppression: Double,
    labelShift: Double,
    antiDetectionScore: Double,
    reason: String,
): String = buildString {
    append("Original faces: $originalFaceCount; ")
    append("Protected faces: $protectedFaceCount; ")
    append("Original features: $originalFeatureCount; ")
    append("Protected features: $protectedFeatureCount; ")
    append("FaceSuppression: ${formatProbeNumber(faceSuppression)}; ")
    append("FeatureSuppression: ${formatProbeNumber(featureSuppression)}; ")
    append("LabelShift: ${formatProbeNumber(labelShift)}; ")
    append("AntiDetectionScore: ${formatProbeNumber(antiDetectionScore)}\n")
    append("Decision: $reason")
}

private fun formatProbeNumber(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}
