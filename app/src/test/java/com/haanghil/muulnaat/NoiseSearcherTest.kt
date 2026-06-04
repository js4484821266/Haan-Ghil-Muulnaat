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
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = { false })
        assertNull(result)
    }

    @Test
    fun `returns lo when lo itself passes`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(0))
        assertEquals(0, result)
    }

    @Test
    fun `returns highest candidate when only it passes`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(90))
        assertEquals(100, result)
    }

    @Test
    fun `returns null when threshold is above highest candidate`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(101))
        assertNull(result)
    }

    @Test
    fun `finds exact midpoint threshold`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(50))
        assertEquals(60, result)
    }

    @Test
    fun `finds threshold at 1`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(1))
        assertEquals(20, result)
    }

    @Test
    fun `returns highest candidate for threshold at 99`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(99))
        assertEquals(100, result)
    }

    @Test
    fun `works with narrow range`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 10, hi = 20, test = thresholdTest(15))
        assertEquals(20, result)
    }

    @Test
    fun `returns null when range is empty because hi fails`() {
        // hi=50, threshold=51 — hi itself fails
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 50, test = thresholdTest(51))
        assertNull(result)
    }

    @Test
    fun `returns correct minimum not just any passing value`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 0, hi = 100, test = thresholdTest(30))
        assertEquals(40, result)
    }

    @Test
    fun `single-candidate range passes`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 40, hi = 40, test = thresholdTest(40))
        assertEquals(40, result)
    }

    @Test
    fun `range with no candidates fails`() {
        val result = NoiseSearcher.findMinimumStrength(lo = 42, hi = 42, test = thresholdTest(42))
        assertNull(result)
    }

    @Test
    fun `reports progress for each ascending candidate step`() {
        val steps = mutableListOf<NoiseSearcher.SearchStep>()

        val result = NoiseSearcher.findMinimumStrength(
            lo = 0,
            hi = 100,
            test = thresholdTest(30),
            onStep = { steps.add(it) },
        )

        assertEquals(40, result)
        assertEquals(listOf(0, 20, 40), steps.map { it.mid })
        assertTrue(steps.isNotEmpty())
        for (step in steps) {
            assertTrue(step.iteration >= 1)
            assertTrue(step.low <= step.high)
            assertTrue(step.mid in step.low..step.high)
            assertEquals(0, step.mid % 20)
        }
    }
}
