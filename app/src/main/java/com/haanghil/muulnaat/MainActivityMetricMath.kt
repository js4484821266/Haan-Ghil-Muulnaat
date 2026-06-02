package com.haanghil.muulnaat

import android.graphics.Bitmap
import kotlin.math.abs

/**
 * Display-only math helpers.
 *
 * These values are for UI summaries, not for final protection decisions.
 */
internal fun psnrToPercent(psnr: Double): Double {
    val normalized = (psnr - 8.0) / (50.0 - 8.0)
    return (normalized * 100.0).coerceIn(0.0, 100.0)
}

internal fun computeMeanAbsoluteDifference(reference: Bitmap, tested: Bitmap): Double {
    val width = minOf(reference.width, tested.width)
    val height = minOf(reference.height, tested.height)
    val refPixels = IntArray(width * height)
    val testedPixels = IntArray(width * height)
    reference.getPixels(refPixels, 0, width, 0, 0, width, height)
    tested.getPixels(testedPixels, 0, width, 0, 0, width, height)

    var totalDiff = 0.0
    for (i in refPixels.indices) {
        val ref = refPixels[i]
        val dst = testedPixels[i]
        val dr = abs(((ref shr 16) and 0xFF) - ((dst shr 16) and 0xFF))
        val dg = abs(((ref shr 8) and 0xFF) - ((dst shr 8) and 0xFF))
        val db = abs((ref and 0xFF) - (dst and 0xFF))
        totalDiff += (dr + dg + db) / 3.0
    }
    return totalDiff / testedPixels.size
}
