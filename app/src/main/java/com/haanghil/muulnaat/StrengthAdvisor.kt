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
        onStep: ((NoiseSearcher.SearchStep) -> Unit)? = null,
        shouldCancel: () -> Boolean = { false },
    ): Int? {
        val faceRegions = FaceRegionDetector.detectRegions(original)

        return NoiseSearcher.findMinimumStrength(
            original = original,
            perturbationModule = perturbationModule,
            defenseEvaluator = defenseEvaluator,
            regions = faceRegions,
            onStep = onStep,
            shouldCancel = shouldCancel,
        )
    }
}
