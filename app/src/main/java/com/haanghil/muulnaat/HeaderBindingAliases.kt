package com.haanghil.muulnaat

import com.haanghil.muulnaat.databinding.MainHeaderControlsBinding

/**
 * 헤더 레이아웃을 include로 쪼갠 뒤에도 Kotlin 코드가 평평하게 읽히도록 돕습니다.
 */
internal val MainHeaderControlsBinding.helpButton get() = headerPickActions.helpButton
internal val MainHeaderControlsBinding.pickButton get() = headerPickActions.pickButton
internal val MainHeaderControlsBinding.recommendedStrengthText get() = headerStrengthControls.recommendedStrengthText
internal val MainHeaderControlsBinding.searchProgressText get() = headerStrengthControls.searchProgressText
internal val MainHeaderControlsBinding.resetOptimalButton get() = headerStrengthControls.resetOptimalButton
internal val MainHeaderControlsBinding.hidingMethodSpinner get() = headerStrengthControls.hidingMethodSpinner
internal val MainHeaderControlsBinding.strengthLabel get() = headerStrengthControls.strengthLabel
internal val MainHeaderControlsBinding.strengthSeekBar get() = headerStrengthControls.strengthSeekBar
internal val MainHeaderControlsBinding.solidColorControls get() = headerStrengthControls.solidColorControls
internal val MainHeaderControlsBinding.solidColorInput get() = headerStrengthControls.solidColorInput
internal val MainHeaderControlsBinding.solidColorPreview get() = headerStrengthControls.solidColorPreview
internal val MainHeaderControlsBinding.autoRecoverySwitch get() = headerStrengthControls.autoRecoverySwitch
internal val MainHeaderControlsBinding.applyButton get() = headerProcessingActions.applyButton
internal val MainHeaderControlsBinding.attackButton get() = headerProcessingActions.attackButton
internal val MainHeaderControlsBinding.busyProgressBar get() = headerProcessingActions.busyProgressBar
