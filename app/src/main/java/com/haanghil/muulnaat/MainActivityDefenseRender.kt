package com.haanghil.muulnaat

/**
 * Writes defense metrics into the technical detail panel.
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
