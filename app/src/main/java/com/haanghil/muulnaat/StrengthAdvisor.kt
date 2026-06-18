package com.haanghil.muulnaat

import android.content.Context
import android.graphics.Bitmap

object StrengthAdvisor {
    fun recommendationText(context: Context, recommendedStrength: Int?): String {
        return if (recommendedStrength == null) {
            context.getString(R.string.recommended_strength_none)
        } else {
            context.getString(R.string.recommended_strength_found, recommendedStrength)
        }
    }

    fun findRecommendedStrength(
        original: Bitmap,
        perturbationModule: PerturbationModule,
        defenseEvaluator: DefenseEvaluator,
        config: HidingConfig = HidingConfig.noise(),
        onStep: ((NoiseSearcher.SearchStep) -> Unit)? = null,
        shouldCancel: () -> Boolean = { false },
    ): Int? {
        if (!config.method.supportsStrengthSearch()) return null
        val faceRegions = FaceRegionDetector.detectRegions(original)

        return NoiseSearcher.findMinimumStrength(
            original = original,
            perturbationModule = perturbationModule,
            defenseEvaluator = defenseEvaluator,
            regions = faceRegions,
            config = config,
            onStep = onStep,
            shouldCancel = shouldCancel,
        )
    }
}
