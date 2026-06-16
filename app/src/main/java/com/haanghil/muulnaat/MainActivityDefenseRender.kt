package com.haanghil.muulnaat

/**
 * 방어 메트릭을 기술 세부 패널에 씁니다.
 */
internal fun MainActivity.renderDefenseResult(
    status: ProtectionStatus,
    evaluationMetrics: EvaluationMetrics,
    qualityMetrics: QualityMetrics,
) {
    binding.protectionStatusValue.text = statusLabel(status)
    binding.modelFaceCountValue.text = getString(
        R.string.metric_face_count_format,
        evaluationMetrics.faceCountOriginal,
        evaluationMetrics.faceCountAfterAttack,
    )
    binding.modelFacialFeatureValue.text = getString(
        R.string.metric_facial_features_format,
        evaluationMetrics.facialFeatureCountOriginal,
        evaluationMetrics.facialFeatureCountAfterAttack,
        evaluationMetrics.facialFeatureSuppressionScore,
    )
    binding.modelLabelShiftValue.text = getString(R.string.metric_label_shift_format, evaluationMetrics.labelShift)
    binding.modelScoreValue.text = getString(R.string.metric_score_format, evaluationMetrics.antiDetectionScore)
    binding.qualityPsnrValue.text = getString(
        R.string.metric_psnr_format,
        qualityMetrics.psnr,
        psnrToPercent(qualityMetrics.psnr),
    )
    binding.qualityDeltaValue.text = getString(R.string.metric_delta_format, qualityMetrics.meanAbsDelta)
    binding.qualityEdgeDeltaValue.text = getString(R.string.metric_edge_delta_format, qualityMetrics.edgeDelta)
}
