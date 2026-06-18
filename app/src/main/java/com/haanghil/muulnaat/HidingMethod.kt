package com.haanghil.muulnaat

enum class HidingMethod {
    NOISE,
    BLUR,
    SOLID_FILL,
}

data class HidingConfig(
    val method: HidingMethod,
    val solidColor: Int = SOLID_FILL_DEFAULT_COLOR,
) {
    companion object {
        const val SOLID_FILL_DEFAULT_COLOR: Int = -0x1000000

        fun noise(): HidingConfig = HidingConfig(HidingMethod.NOISE)
        fun blur(): HidingConfig = HidingConfig(HidingMethod.BLUR)
        fun solidFill(color: Int = SOLID_FILL_DEFAULT_COLOR): HidingConfig =
            HidingConfig(HidingMethod.SOLID_FILL, solidColor = color)
    }
}

fun HidingMethod.supportsStrengthSearch(): Boolean =
    this == HidingMethod.NOISE || this == HidingMethod.BLUR
