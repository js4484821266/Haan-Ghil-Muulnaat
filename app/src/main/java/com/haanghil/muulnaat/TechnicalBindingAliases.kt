package com.haanghil.muulnaat

import com.haanghil.muulnaat.databinding.MainTechnicalDetailsBinding

/**
 * 기술 세부 영역을 쪼갠 뒤에도 메트릭 필드 접근을 평평하게 유지합니다.
 */
internal val MainTechnicalDetailsBinding.modelFaceCountValue get() = modelMetrics.modelFaceCountValue
internal val MainTechnicalDetailsBinding.modelFacialFeatureValue get() = modelMetrics.modelFacialFeatureValue
internal val MainTechnicalDetailsBinding.modelLabelShiftValue get() = modelMetrics.modelLabelShiftValue
internal val MainTechnicalDetailsBinding.modelScoreValue get() = modelMetrics.modelScoreValue
