package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.graphics.Color

/**
 * 복원 모델의 첫 단계처럼 작은 창 안에서 튀는 픽셀을 누그러뜨립니다.
 * 중앙값은 점 잡음에 강하고, 평균은 주변 색 합의값을 반영합니다.
 */
internal fun denoiseForRestoration(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    val outPixels = IntArray(pixels.size)
    val windowR = IntArray(9)
    val windowG = IntArray(9)
    val windowB = IntArray(9)

    source.getPixels(pixels, 0, width, 0, 0, width, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            var index = 0
            for (ky in -1..1) {
                for (kx in -1..1) {
                    val px = (x + kx).coerceIn(0, width - 1)
                    val py = (y + ky).coerceIn(0, height - 1)
                    val pixel = pixels[py * width + px]
                    windowR[index] = Color.red(pixel)
                    windowG[index] = Color.green(pixel)
                    windowB[index] = Color.blue(pixel)
                    index += 1
                }
            }

            windowR.sort()
            windowG.sort()
            windowB.sort()

            outPixels[y * width + x] = Color.rgb(
                restoredChannel(windowR),
                restoredChannel(windowG),
                restoredChannel(windowB),
            )
        }
    }

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    result.setPixels(outPixels, 0, width, 0, 0, width, height)
    return result
}

private fun restoredChannel(sortedWindow: IntArray): Int {
    val median = sortedWindow[4]
    val mean = sortedWindow.sum() / sortedWindow.size
    return (median * 0.7 + mean * 0.3).toInt().coerceIn(0, 255)
}
