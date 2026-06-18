package com.haanghil.muulnaat

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast

internal fun MainActivity.configureHidingMethodControls() {
    val labels = listOf(
        getString(R.string.hiding_method_select),
        getString(R.string.hiding_method_noise),
        getString(R.string.hiding_method_blur),
        getString(R.string.hiding_method_solid_fill),
    )
    binding.hidingMethodSpinner.adapter = ArrayAdapter(
        this,
        android.R.layout.simple_spinner_dropdown_item,
        labels,
    )
    binding.hidingMethodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onHidingMethodSelected(methodFromSpinnerPosition(position))
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            onHidingMethodSelected(null)
        }
    }

    binding.solidColorInput.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            parseSolidColor(s?.toString())?.let { color ->
                binding.solidColorPreview.setBackgroundColor(color)
            }
        }
        override fun afterTextChanged(s: Editable?) = Unit
    })
    binding.solidColorPreview.setBackgroundColor(HidingConfig.SOLID_FILL_DEFAULT_COLOR)
    updateHidingControls(null)
}

internal fun MainActivity.currentHidingConfigOrNull(showToast: Boolean = true): HidingConfig? {
    return when (state.selectedMethod) {
        HidingMethod.NOISE -> HidingConfig.noise()
        HidingMethod.BLUR -> HidingConfig.blur()
        HidingMethod.SOLID_FILL -> {
            val color = parseSolidColor(binding.solidColorInput.text?.toString())
            if (color == null) {
                if (showToast) {
                    Toast.makeText(this, getString(R.string.toast_invalid_solid_color), Toast.LENGTH_SHORT).show()
                }
                null
            } else {
                HidingConfig.solidFill(color)
            }
        }
        null -> {
            if (showToast) {
                Toast.makeText(this, getString(R.string.toast_select_hiding_method), Toast.LENGTH_SHORT).show()
            }
            null
        }
    }
}

internal fun MainActivity.defaultBatchConfig(): HidingConfig = when (state.selectedMethod) {
    HidingMethod.BLUR -> HidingConfig.blur()
    HidingMethod.SOLID_FILL -> {
        val color = parseSolidColor(binding.solidColorInput.text?.toString())
            ?: HidingConfig.SOLID_FILL_DEFAULT_COLOR
        HidingConfig.solidFill(color)
    }
    HidingMethod.NOISE, null -> HidingConfig.noise()
}

internal fun MainActivity.storeOptimalStrength(method: HidingMethod, strength: Int?) {
    when (method) {
        HidingMethod.NOISE -> state.noiseOptimalStrength = strength
        HidingMethod.BLUR -> state.blurOptimalStrength = strength
        HidingMethod.SOLID_FILL -> Unit
    }
    state.optimalStrength = rememberedOptimalStrength(method)
}

internal fun MainActivity.rememberedOptimalStrength(method: HidingMethod?): Int? = when (method) {
    HidingMethod.NOISE -> state.noiseOptimalStrength
    HidingMethod.BLUR -> state.blurOptimalStrength
    HidingMethod.SOLID_FILL, null -> null
}

internal fun MainActivity.configForMethod(method: HidingMethod): HidingConfig = when (method) {
    HidingMethod.NOISE -> HidingConfig.noise()
    HidingMethod.BLUR -> HidingConfig.blur()
    HidingMethod.SOLID_FILL -> HidingConfig.solidFill(
        parseSolidColor(binding.solidColorInput.text?.toString()) ?: HidingConfig.SOLID_FILL_DEFAULT_COLOR
    )
}

private fun MainActivity.onHidingMethodSelected(method: HidingMethod?) {
    val previous = state.selectedMethod
    state.selectedMethod = method
    state.optimalStrength = rememberedOptimalStrength(method)
    updateHidingControls(method)

    if (previous != method) {
        state.protectedBitmap = null
        state.lastAppliedStrength = null
        state.lastAppliedMethod = null
        state.lastEvaluationStrength = null
        binding.noisyImage.setImageDrawable(null)
        binding.recoveredImage.setImageDrawable(null)
        clearResultCards()
    }

    val source = state.originalBitmap ?: return
    if (method?.supportsStrengthSearch() == true) {
        startOptimalStrengthFlow(source, autoSaveAfterProtection = false, method = method)
    } else if (method == HidingMethod.SOLID_FILL) {
        binding.recommendedStrengthText.text = getString(R.string.recommended_strength_solid_fill)
        showSearchProgress(getString(R.string.search_progress_solid_fill))
        binding.resetOptimalButton.isEnabled = false
    }
}

private fun MainActivity.updateHidingControls(method: HidingMethod?) {
    val isSolid = method == HidingMethod.SOLID_FILL
    binding.strengthLabel.visibility = if (isSolid) View.GONE else View.VISIBLE
    binding.strengthSeekBar.visibility = if (isSolid) View.GONE else View.VISIBLE
    binding.solidColorControls.visibility = if (isSolid) View.VISIBLE else View.GONE
    binding.attackButton.isEnabled = method != HidingMethod.SOLID_FILL

    if (method == HidingMethod.SOLID_FILL) {
        binding.recommendedStrengthText.text = getString(R.string.recommended_strength_solid_fill)
    } else {
        binding.recommendedStrengthText.text = StrengthAdvisor.recommendationText(this, state.optimalStrength)
    }
}

private fun methodFromSpinnerPosition(position: Int): HidingMethod? = when (position) {
    1 -> HidingMethod.NOISE
    2 -> HidingMethod.BLUR
    3 -> HidingMethod.SOLID_FILL
    else -> null
}

internal fun parseSolidColor(value: String?): Int? {
    val text = value?.trim().orEmpty()
    if (!Regex("^#[0-9a-fA-F]{6}$").matches(text)) return null
    return HidingConfig.SOLID_FILL_DEFAULT_COLOR or text.substring(1).toInt(16)
}
