package com.haanghil.muulnaat

import com.haanghil.muulnaat.databinding.MainTechnicalDetailsBinding

/**
 * Keeps metric-field access flat after the technical section was split.
 */
internal val MainTechnicalDetailsBinding.modelFaceCountValue get() = modelMetrics.modelFaceCountValue
internal val MainTechnicalDetailsBinding.modelLabelShiftValue get() = modelMetrics.modelLabelShiftValue
internal val MainTechnicalDetailsBinding.modelScoreValue get() = modelMetrics.modelScoreValue
