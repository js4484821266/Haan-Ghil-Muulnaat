package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.graphics.Rect

object NoiseSearcher {
    internal fun candidateStrengths(lo: Int, hi: Int): List<Int> {
        if (lo > hi) return emptyList()
        return (listOf(0, 20, 40, 60) + (80..100)).filter { it in lo..hi }
    }

    data class SearchStep(
        val iteration: Int,
        val low: Int,
        val mid: Int,
        val high: Int,
        val passed: Boolean,
    )

    /**
     * [lo, hi] 범위의 기본 후보 강도 중 [test]가 true를 반환하는 최솟값을
     * 이분 탐색으로 찾습니다.
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
        var iteration = 1

        fun search(candidates: List<Int>): Int? {
            if (candidates.isEmpty()) return null

            var low = 0
            var high = candidates.lastIndex
            var lowerFailed: Int? = null
            var upperPassed: Int? = null

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
                    upperPassed = strength
                    high = mid - 1
                } else {
                    lowerFailed = strength
                    low = mid + 1
                }
                val bracketResult = boundedCandidate(candidates, lowerFailed, upperPassed)
                if (bracketResult != null) return bracketResult
                iteration += 1
            }
            return upperPassed
        }

        val coarseResult = search(candidateStrengths(lo, minOf(hi, 80)))
        if (coarseResult != null || shouldCancel()) return coarseResult
        return search(candidateStrengths(maxOf(lo, 81), hi))
    }

    private fun boundedCandidate(candidates: List<Int>, lowerFailed: Int?, upperPassed: Int?): Int? {
        if (lowerFailed == null || upperPassed == null) return null
        val lowerIndex = candidates.indexOf(lowerFailed)
        val upperIndex = candidates.indexOf(upperPassed)
        if (lowerIndex < 0 || upperIndex < 0) return null
        if (upperIndex - lowerIndex > 1) return null
        return upperPassed
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
