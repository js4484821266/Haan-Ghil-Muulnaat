package com.haanghil.muulnaat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseRegionMaskTest {
    @Test
    fun `returns null when regions are null to keep global apply meaning`() {
        val mask = NoiseRegionMask.build(width = 4, height = 4, regions = null)

        assertNull(mask)
    }

    @Test
    fun `returns all false mask when regions are empty`() {
        val mask = NoiseRegionMask.buildForMaskRegions(
            width = 4,
            height = 4,
            regions = emptyList(),
        )

        assertFalse(mask.any { it })
    }

    @Test
    fun `oval fallback keeps center and excludes rectangle corners`() {
        val width = 4
        val mask = NoiseRegionMask.buildForMaskRegions(
            width = width,
            height = 4,
            regions = listOf(MaskRegion(bounds = MaskBounds(left = 0, top = 0, right = 4, bottom = 4))),
        )

        assertFalse(mask.at(width, x = 0, y = 0))
        assertTrue(mask.at(width, x = 1, y = 1))
        assertTrue(mask.at(width, x = 2, y = 2))
        assertFalse(mask.at(width, x = 3, y = 3))
    }

    @Test
    fun `polygon outline keeps only pixels inside outline`() {
        val width = 5
        val mask = NoiseRegionMask.buildForMaskRegions(
            width = width,
            height = 5,
            regions = listOf(
                MaskRegion(
                    bounds = MaskBounds(left = 0, top = 0, right = 5, bottom = 5),
                    outlinePoints = listOf(
                        MaskPoint(x = 1f, y = 1f),
                        MaskPoint(x = 4f, y = 1f),
                        MaskPoint(x = 4f, y = 4f),
                        MaskPoint(x = 1f, y = 4f),
                    ),
                )
            ),
        )

        assertFalse(mask.at(width, x = 0, y = 0))
        assertTrue(mask.at(width, x = 2, y = 2))
        assertFalse(mask.at(width, x = 4, y = 4))
    }

    private fun BooleanArray.at(width: Int, x: Int, y: Int): Boolean = this[y * width + x]
}
