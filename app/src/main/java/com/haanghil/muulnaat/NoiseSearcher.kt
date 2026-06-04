package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.graphics.Rect

object NoiseSearcher {
    private val DEFAULT_CANDIDATE_STRENGTHS = listOf(0, 20, 40, 60, 80, 100)

    data class SearchStep(
        val iteration: Int,
        val low: Int,
        val mid: Int,
        val high: Int,
        val passed: Boolean,
    )

    /**
     * [lo, hi] 범위의 기본 후보 강도 중 [test]가 true를 반환하는 최솟값을
     * 낮은 후보부터 차례대로 검사해 찾습니다.
     * 범위 안에서 통과하는 후보가 없으면 null을 반환합니다.
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

        var iteration = 1
        val highest = candidates.last()
        for (strength in candidates) {
            if (shouldCancel()) return null
            val passed = test(strength)
            onStep?.invoke(
                SearchStep(
                    iteration = iteration,
                    low = strength,
                    mid = strength,
                    high = highest,
                    passed = passed,
                )
            )
            if (passed) return strength
            iteration += 1
        }
        return null
    }

    fun findMinimumStrength(
        original: Bitmap,
        perturbationModule: PerturbationModule,
        defenseEvaluator: DefenseEvaluator,
        regions: List<Rect>? = null,
        lo: Int = 0,
        hi: Int = 100,
        onStep: ((SearchStep) -> Unit)? = null,
        shouldCancel: () -> Boolean = { false },
    ): Int? =
        findMinimumStrength(
            lo = lo,
            hi = hi,
            test = { strength ->
                val protected = perturbationModule.applyProtection(original, strength, regions)
                defenseEvaluator.evaluateAfterAttack(original, protected).status == ProtectionStatus.HELD
            },
            onStep = onStep,
            shouldCancel = shouldCancel,
        )

    /**
     * [NoiseEngine]과 [ModelProbe]로 테스트를 구성하는 편의 오버로드입니다.
     * 호출한 스레드에서 실행되므로 UI에서는 백그라운드 스레드에서 호출해야 합니다.
     */
    fun findMinimumStrength(
        original: Bitmap,
        regions: List<Rect>? = null,
        lo: Int = 0,
        hi: Int = 100,
        onStep: ((SearchStep) -> Unit)? = null,
        shouldCancel: () -> Boolean = { false },
    ): Int? =
        findMinimumStrength(
            lo = lo,
            hi = hi,
            test = { strength ->
                val protected = NoiseEngine.protect(original, strength, regions)
                ModelProbe.evaluate(original, protected).passed
            },
            onStep = onStep,
            shouldCancel = shouldCancel,
        )
}
