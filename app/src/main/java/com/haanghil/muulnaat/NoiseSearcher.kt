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
     * [lo, hi] 범위의 기본 후보 강도 중 [test]가 true를 반환하는 최솟값을
     * 이진 탐색으로 찾습니다.
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

        // 가장 강한 후보를 먼저 검사합니다. 이것도 실패하면 이진 탐색은 비싼 ML Kit
        // 평가만 더 수행한 뒤 결국 null에 도달하게 됩니다.
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
                // 통과했다는 것은 이 강도가 유효하다는 뜻입니다. 더 작은 유효 후보를
                // 찾기 위해 왼쪽 범위를 계속 탐색합니다.
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
     * [NoiseEngine]과 [ModelProbe]로 테스트를 구성하는 편의 오버로드입니다.
     * 호출한 스레드에서 실행되므로 UI에서는 백그라운드 스레드에서 호출해야 합니다.
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
