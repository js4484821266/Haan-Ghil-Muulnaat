package com.haanghil.muulnaat

import android.graphics.Bitmap

object NoiseSearcher {
    private val DEFAULT_CANDIDATE_STRENGTHS = (0..90 step 10).toList()

    data class SearchStep(
        val iteration: Int,
        val low: Int,
        val mid: Int,
        val high: Int,
        val passed: Boolean,
    )

    /**
     * Binary-searches the default candidate strengths in [lo, hi] (inclusive) for
     * the minimum value where [test] returns true.
     * Returns null if no candidate in the range achieves a passing result.
     */
    fun findMinimumStrength(
        lo: Int = 0,
        hi: Int = 100,
        test: (strength: Int) -> Boolean,
        onStep: ((SearchStep) -> Unit)? = null,
        shouldCancel: () -> Boolean = { false },
    ): Int? {
        if (shouldCancel()) return null
        val candidates = DEFAULT_CANDIDATE_STRENGTHS.filter { it in lo..hi }
        if (candidates.isEmpty()) return null
        if (!test(candidates.last())) return null

        var low = 0
        var high = candidates.lastIndex
        var result: Int? = null
        var iteration = 1

        while (low <= high) {
            if (shouldCancel()) return null
            val mid = low + (high - low) / 2
            val strength = candidates[mid]
            val passed = test(strength)
            onStep?.invoke(
                SearchStep(
                    iteration = iteration,
                    low = candidates[low],
                    mid = strength,
                    high = candidates[high],
                    passed = passed,
                )
            )
            if (passed) {
                result = strength
                high = mid - 1
            } else {
                low = mid + 1
            }
            iteration += 1
        }
        return result
    }

    fun findMinimumStrength(
        original: Bitmap,
        perturbationModule: PerturbationModule,
        defenseEvaluator: DefenseEvaluator,
        lo: Int = 0,
        hi: Int = 100,
        onStep: ((SearchStep) -> Unit)? = null,
        shouldCancel: () -> Boolean = { false },
    ): Int? =
        findMinimumStrength(
            lo = lo,
            hi = hi,
            test = { strength ->
                val protected = perturbationModule.applyProtection(original, strength)
                defenseEvaluator.evaluateAfterAttack(original, protected).status == ProtectionStatus.HELD
            },
            onStep = onStep,
            shouldCancel = shouldCancel,
        )

    /**
     * Convenience overload that builds the test using [NoiseEngine] and [ModelProbe].
     * Runs on the calling thread; call from a background thread in the UI.
     */
    fun findMinimumStrength(
        original: Bitmap,
        lo: Int = 0,
        hi: Int = 100,
        onStep: ((SearchStep) -> Unit)? = null,
        shouldCancel: () -> Boolean = { false },
    ): Int? =
        findMinimumStrength(
            lo = lo,
            hi = hi,
            test = { strength ->
                val protected = NoiseEngine.protect(original, strength)
                ModelProbe.evaluate(original, protected).passed
            },
            onStep = onStep,
            shouldCancel = shouldCancel,
        )
}
