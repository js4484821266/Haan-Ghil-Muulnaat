package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt

private const val UNSHARP_AMOUNT = 0.5

/**
 * 업스케일된 이미지에서 복원 모델이 눈, 코, 입 경계를 또렷하게 만드는 효과를
 * 단순 unsharp mask로 근사합니다.
 */
internal fun sharpenForRestoration(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    val outPixels: IntArray

    source.getPixels(pixels, 0, width, 0, 0, width, height)
    outPixels = pixels.copyOf()

    if (width < 3 || height < 3) {
        return source.copy(Bitmap.Config.ARGB_8888, false)
    }

    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val index = y * width + x
            outPixels[index] = sharpenPixel(
                center = pixels[index],
                left = pixels[index - 1],
                right = pixels[index + 1],
                top = pixels[index - width],
                bottom = pixels[index + width],
            )
        }
    }

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    result.setPixels(outPixels, 0, width, 0, 0, width, height)
    return result
}

private fun sharpenPixel(center: Int, left: Int, right: Int, top: Int, bottom: Int): Int {
    val surroundingR = (Color.red(left) + Color.red(right) + Color.red(top) + Color.red(bottom)) / 4
    val surroundingG = (Color.green(left) + Color.green(right) + Color.green(top) + Color.green(bottom)) / 4
    val surroundingB = (Color.blue(left) + Color.blue(right) + Color.blue(top) + Color.blue(bottom)) / 4

    return Color.rgb(
        sharpenChannel(Color.red(center), surroundingR),
        sharpenChannel(Color.green(center), surroundingG),
        sharpenChannel(Color.blue(center), surroundingB),
    )
}

private fun sharpenChannel(center: Int, surrounding: Int): Int {
    return (center + (center - surrounding) * UNSHARP_AMOUNT).roundToInt().coerceIn(0, 255)
}
