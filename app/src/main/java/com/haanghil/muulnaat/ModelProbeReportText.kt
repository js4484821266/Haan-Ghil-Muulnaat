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
        pass -> "성공: 복원 공격 후 얼굴 특징이 억제됐습니다 " +
            "(점수=${formatProbeNumber(antiDetectionScore)})"
        !hasFeatureBaseline(originalFaceCount, originalFeatureCount) ->
            "실패: 원본 얼굴/특징 기준값을 찾지 못했습니다 " +
                "(얼굴=$originalFaceCount, 특징=$originalFeatureCount)"
        protectedFeatureCount > 0 ->
            "실패: 공격 후에도 얼굴 특징이 감지됩니다 " +
                "(특징: $originalFeatureCount -> $protectedFeatureCount)"
        else -> "실패: 얼굴 특징 억제 점수가 부족합니다 " +
            "(점수=${formatProbeNumber(antiDetectionScore)}, " +
            "필요>=${formatProbeNumber(ModelProbe.PASS_THRESHOLD)})"
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
    append("원본 얼굴 수: $originalFaceCount; ")
    append("테스트 얼굴 수: $protectedFaceCount; ")
    append("원본 얼굴 특징: $originalFeatureCount; ")
    append("테스트 얼굴 특징: $protectedFeatureCount; ")
    append("얼굴 억제: ${formatProbeNumber(faceSuppression)}; ")
    append("특징 억제: ${formatProbeNumber(featureSuppression)}; ")
    append("라벨 변화량: ${formatProbeNumber(labelShift)}; ")
    append("방어 점수: ${formatProbeNumber(antiDetectionScore)}\n")
    append("판정 사유: $reason")
}

private fun formatProbeNumber(value: Double): String {
    return String.format(Locale.US, "%.2f", value)
}