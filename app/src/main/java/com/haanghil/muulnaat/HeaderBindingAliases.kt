package com.haanghil.muulnaat

import com.haanghil.muulnaat.databinding.MainHeaderControlsBinding

/**
 * Keeps Kotlin code readable after the header layout was split into includes.
 */
internal val MainHeaderControlsBinding.helpButton get() = headerPickActions.helpButton
internal val MainHeaderControlsBinding.pickButton get() = headerPickActions.pickButton
internal val MainHeaderControlsBinding.recommendedStrengthText get() = headerStrengthControls.recommendedStrengthText
internal val MainHeaderControlsBinding.searchProgressText get() = headerStrengthControls.searchProgressText
internal val MainHeaderControlsBinding.resetOptimalButton get() = headerStrengthControls.resetOptimalButton
internal val MainHeaderControlsBinding.strengthLabel get() = headerStrengthControls.strengthLabel
internal val MainHeaderControlsBinding.strengthSeekBar get() = headerStrengthControls.strengthSeekBar
internal val MainHeaderControlsBinding.autoRecoverySwitch get() = headerStrengthControls.autoRecoverySwitch
internal val MainHeaderControlsBinding.applyButton get() = headerProcessingActions.applyButton
internal val MainHeaderControlsBinding.attackButton get() = headerProcessingActions.attackButton
internal val MainHeaderControlsBinding.busyProgressBar get() = headerProcessingActions.busyProgressBar
