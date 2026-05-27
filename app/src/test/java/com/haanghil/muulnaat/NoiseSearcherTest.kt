package com.haanghil.muulnaat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseSearcherTest {

    // Simulates a scenario where strength >= threshold makes the test pass.
    private fun thresholdTest(threshold: Int): (Int) -> Boolean = { it >= threshold }

    @Test
    fun `returns null when nothing in range passes`() {
        // Nothing passes – the ceiling (100) itself fails
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = { false })
        assertNull(result)
    }

    @Test
    fun `returns lo when lo itself passes`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(0))
        assertEquals(0, result)
    }

    @Test
    fun `returns hi when only hi passes`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(100))
        assertEquals(100, result)
    }

    @Test
    fun `finds exact midpoint threshold`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(50))
        assertEquals(50, result)
    }

    @Test
    fun `finds threshold at 1`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(1))
        assertEquals(1, result)
    }

    @Test
    fun `finds threshold at 99`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(99))
        assertEquals(99, result)
    }

    @Test
    fun `works with narrow range`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 10, hi = 20, test = thresholdTest(15))
        assertEquals(15, result)
    }

    @Test
    fun `returns null when range is empty because hi fails`() {
        // hi=50, threshold=51 — hi itself fails
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 50, test = thresholdTest(51))
        assertNull(result)
    }

    @Test
    fun `returns correct minimum not just any passing value`() {
        // Passing strengths: 30, 31, …, 100 — minimum should be 30
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(30))
        assertEquals(30, result)
    }

    @Test
    fun `single-element range passes`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 42, hi = 42, test = thresholdTest(42))
        assertEquals(42, result)
    }

    @Test
    fun `single-element range fails`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 42, hi = 42, test = thresholdTest(43))
        assertNull(result)
    }

    @Test
    fun `reports progress for each binary search step`() {
        val steps = mutableListOf<NoiseSearcher.SearchStep>()

        val result = NoiseSearcher.findMinimumStrength(
            lo = 0,
            hi = 100,
            test = thresholdTest(30),
            onStep = { steps.add(it) },
        )

        assertEquals(30, result)
        assertTrue(steps.isNotEmpty())
        for (step in steps) {
            assertTrue(step.iteration >= 1)
            assertTrue(step.low <= step.high)
            assertTrue(step.mid in step.low..step.high)
        }
    }
}
