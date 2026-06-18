package com.haanghil.muulnaat

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import kotlin.math.max
import kotlin.math.min

/**
 * 얼굴 마스크 내부만 흐리게 만드는 은닉 엔진입니다.
 *
 * strength 100은 사용자가 지정한 단색이 아니라, 마스크 내부 전체가 거의 같은 평균색이 될 만큼
 * 큰 blur 반경을 사용합니다.
 */
object BlurEngine {
    fun applyProtection(
        source: Bitmap,
        strength: Int,
        regions: List<FaceProtectionRegion>?,
    ): Bitmap {
        val width = source.width
        val height = source.height
        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val regionMask = NoiseRegionMask.build(width, height, regions)
        val outPixels = applyPixels(srcPixels, width, height, strength, regionMask)

        return createBitmap(width, height).also {
            it.setPixels(outPixels, 0, width, 0, 0, width, height)
        }
    }

    internal fun applyPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        strength: Int,
        regionMask: BooleanArray?,
    ): IntArray {
        val safeStrength = strength.coerceIn(0, 100)
        if (safeStrength == 0) return pixels.copyOf()

        val coverage = PixelCoverage(width, height, regionMask)
        val radius = if (safeStrength == 100) {
            max(width, height)
        } else {
            safeStrength
        }
        val sums = MaskedIntegralSums(pixels, width, height, coverage)
        val outPixels = pixels.copyOf()

        for (y in 0 until height) {
            val top = max(0, y - radius)
            val bottom = min(height - 1, y + radius)
            val rowOffset = y * width
            for (x in 0 until width) {
                val index = rowOffset + x
                if (!coverage.includes(index)) continue

                val left = max(0, x - radius)
                val right = min(width - 1, x + radius)
                val count = sums.countIn(left, top, right, bottom)
                if (count <= 0) continue

                val r = (sums.redIn(left, top, right, bottom) / count).toInt()
                val g = (sums.greenIn(left, top, right, bottom) / count).toInt()
                val b = (sums.blueIn(left, top, right, bottom) / count).toInt()
                outPixels[index] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        return outPixels
    }
}

/**
 * 지정 RGB로 얼굴 마스크 내부를 계단형 경계 그대로 칠합니다.
 */
object SolidFillEngine {
    fun applyProtection(
        source: Bitmap,
        regions: List<FaceProtectionRegion>?,
        color: Int,
    ): Bitmap {
        val width = source.width
        val height = source.height
        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)
        val regionMask = NoiseRegionMask.build(width, height, regions)
        val outPixels = applyPixels(srcPixels, width, height, regionMask, color)

        return createBitmap(width, height).also {
            it.setPixels(outPixels, 0, width, 0, 0, width, height)
        }
    }

    internal fun applyPixels(
        pixels: IntArray,
        width: Int,
        height: Int,
        regionMask: BooleanArray?,
        color: Int,
    ): IntArray {
        val coverage = PixelCoverage(width, height, regionMask)
        val fillColor = HidingConfig.SOLID_FILL_DEFAULT_COLOR or (color and 0x00FFFFFF)
        val outPixels = pixels.copyOf()
        for (index in outPixels.indices) {
            if (coverage.includes(index)) outPixels[index] = fillColor
        }
        return outPixels
    }
}

internal class PixelCoverage(
    width: Int,
    height: Int,
    private val regionMask: BooleanArray?,
) {
    private val pixelCount = width * height

    fun includes(index: Int): Boolean {
        if (index !in 0 until pixelCount) return false
        return regionMask?.get(index) ?: true
    }
}

private class MaskedIntegralSums(
    pixels: IntArray,
    private val width: Int,
    height: Int,
    coverage: PixelCoverage,
) {
    private val stride = width + 1
    private val red = LongArray((width + 1) * (height + 1))
    private val green = LongArray(red.size)
    private val blue = LongArray(red.size)
    private val count = IntArray(red.size)

    init {
        for (y in 1..height) {
            for (x in 1..width) {
                val sourceIndex = (y - 1) * width + (x - 1)
                val integralIndex = y * stride + x
                val left = integralIndex - 1
                val up = integralIndex - stride
                val diagonal = up - 1
                val pixel = pixels[sourceIndex]
                val included = coverage.includes(sourceIndex)

                red[integralIndex] = red[left] + red[up] - red[diagonal] + if (included) red(pixel).toLong() else 0L
                green[integralIndex] = green[left] + green[up] - green[diagonal] + if (included) green(pixel).toLong() else 0L
                blue[integralIndex] = blue[left] + blue[up] - blue[diagonal] + if (included) blue(pixel).toLong() else 0L
                count[integralIndex] = count[left] + count[up] - count[diagonal] + if (included) 1 else 0
            }
        }
    }

    fun redIn(left: Int, top: Int, right: Int, bottom: Int): Long = sum(red, left, top, right, bottom)
    fun greenIn(left: Int, top: Int, right: Int, bottom: Int): Long = sum(green, left, top, right, bottom)
    fun blueIn(left: Int, top: Int, right: Int, bottom: Int): Long = sum(blue, left, top, right, bottom)
    fun countIn(left: Int, top: Int, right: Int, bottom: Int): Int = sum(count, left, top, right, bottom)

    private fun sum(values: LongArray, left: Int, top: Int, right: Int, bottom: Int): Long {
        val x1 = left
        val y1 = top
        val x2 = right + 1
        val y2 = bottom + 1
        return values[y2 * stride + x2] - values[y1 * stride + x2] -
            values[y2 * stride + x1] + values[y1 * stride + x1]
    }

    private fun sum(values: IntArray, left: Int, top: Int, right: Int, bottom: Int): Int {
        val x1 = left
        val y1 = top
        val x2 = right + 1
        val y2 = bottom + 1
        return values[y2 * stride + x2] - values[y1 * stride + x2] -
            values[y2 * stride + x1] + values[y1 * stride + x1]
    }

    private fun red(pixel: Int): Int = (pixel shr 16) and 0xFF
    private fun green(pixel: Int): Int = (pixel shr 8) and 0xFF
    private fun blue(pixel: Int): Int = pixel and 0xFF
}
