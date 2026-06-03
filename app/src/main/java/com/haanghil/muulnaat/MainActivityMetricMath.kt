package com.haanghil.muulnaat

import android.graphics.Bitmap
import kotlin.math.abs

/**
 * 화면 표시 전용 계산 헬퍼입니다.
 *
 * 이 값들은 UI 요약용이며 최종 보호 판단에는 쓰지 않습니다.
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
