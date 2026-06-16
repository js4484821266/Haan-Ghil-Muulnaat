package com.haanghil.muulnaat

import com.haanghil.muulnaat.databinding.ActivityMainBinding

// XML은 include로 나뉘어 있어도 Kotlin 흐름은 예전의 평평한 레이아웃처럼 읽히게 합니다.
internal val ActivityMainBinding.helpButton get() = headerControls.helpButton
internal val ActivityMainBinding.pickButton get() = headerControls.pickButton
internal val ActivityMainBinding.recommendedStrengthText get() = headerControls.recommendedStrengthText
internal val ActivityMainBinding.searchProgressText get() = headerControls.searchProgressText
internal val ActivityMainBinding.resetOptimalButton get() = headerControls.resetOptimalButton
internal val ActivityMainBinding.strengthLabel get() = headerControls.strengthLabel
internal val ActivityMainBinding.strengthSeekBar get() = headerControls.strengthSeekBar
internal val ActivityMainBinding.autoRecoverySwitch get() = headerControls.autoRecoverySwitch
internal val ActivityMainBinding.applyButton get() = headerControls.applyButton
internal val ActivityMainBinding.attackButton get() = headerControls.attackButton
internal val ActivityMainBinding.busyProgressBar get() = headerControls.busyProgressBar

internal val ActivityMainBinding.originalImage get() = imagePanels.originalImage
internal val ActivityMainBinding.noisyImage get() = imagePanels.noisyImage
internal val ActivityMainBinding.recoveredImage get() = imagePanels.recoveredImage
internal val ActivityMainBinding.perturbationSummaryText get() = imagePanels.perturbationSummaryText

internal val ActivityMainBinding.protectionStatusValue get() = statusCard.protectionStatusValue

internal val ActivityMainBinding.technicalDetailsToggle get() = technicalDetails.technicalDetailsToggle
internal val ActivityMainBinding.technicalDetailsContainer get() = technicalDetails.technicalDetailsContainer
internal val ActivityMainBinding.modelFaceCountValue get() = technicalDetails.modelFaceCountValue
internal val ActivityMainBinding.modelFacialFeatureValue get() = technicalDetails.modelFacialFeatureValue
internal val ActivityMainBinding.modelLabelShiftValue get() = technicalDetails.modelLabelShiftValue
internal val ActivityMainBinding.modelScoreValue get() = technicalDetails.modelScoreValue
internal val ActivityMainBinding.qualityPsnrValue get() = technicalDetails.qualityMetrics.qualityPsnrValue
internal val ActivityMainBinding.qualityDeltaValue get() = technicalDetails.qualityMetrics.qualityDeltaValue
internal val ActivityMainBinding.qualityEdgeDeltaValue get() = technicalDetails.qualityMetrics.qualityEdgeDeltaValue
internal val ActivityMainBinding.resultText get() = technicalDetails.resultText
internal val ActivityMainBinding.saveButton get() = technicalDetails.saveButton
