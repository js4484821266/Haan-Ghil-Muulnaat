package com.haanghil.muulnaat

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HidingMethodTest {
    @Test
    fun `only noise and blur support strength search`() {
        assertTrue(HidingMethod.NOISE.supportsStrengthSearch())
        assertTrue(HidingMethod.BLUR.supportsStrengthSearch())
        assertFalse(HidingMethod.SOLID_FILL.supportsStrengthSearch())
    }

    @Test
    fun `solid fill parses only #RRGGBB`() {
        assertEquals(0xFF336699.toInt(), parseSolidColor("#336699"))
        assertNull(parseSolidColor("336699"))
        assertNull(parseSolidColor("#33669"))
    }

    @Test
    fun `solid fill changes only covered pixels`() {
        val pixels = intArrayOf(0xFF010203.toInt(), 0xFF040506.toInt(), 0xFF070809.toInt())
        val mask = booleanArrayOf(false, true, false)

        val result = SolidFillEngine.applyPixels(
            pixels = pixels,
            width = 3,
            height = 1,
            regionMask = mask,
            color = 0xFF112233.toInt(),
        )

        assertArrayEquals(
            intArrayOf(0xFF010203.toInt(), 0xFF112233.toInt(), 0xFF070809.toInt()),
            result,
        )
    }

    @Test
    fun `blur strength zero keeps original pixels`() {
        val pixels = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt())

        val result = BlurEngine.applyPixels(
            pixels = pixels,
            width = 2,
            height = 1,
            strength = 0,
            regionMask = null,
        )

        assertArrayEquals(pixels, result)
    }

    @Test
    fun `blur strength hundred averages covered pixels instead of using chosen solid color`() {
        val pixels = intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), 0xFFFFFFFF.toInt())
        val mask = booleanArrayOf(true, true, false)

        val result = BlurEngine.applyPixels(
            pixels = pixels,
            width = 3,
            height = 1,
            strength = 100,
            regionMask = mask,
        )

        assertEquals(0xFF7F007F.toInt(), result[0])
        assertEquals(0xFF7F007F.toInt(), result[1])
        assertEquals(0xFFFFFFFF.toInt(), result[2])
    }

    @Test
    fun `solid fill is not searched as an optimal strength method`() {
        val result = NoiseSearcher.findMinimumStrength(
            lo = 0,
            hi = 100,
            test = { true },
        )
        assertEquals(0, result)
        assertFalse(HidingConfig.solidFill().method.supportsStrengthSearch())
    }
}
