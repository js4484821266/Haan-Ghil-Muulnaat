package com.haanghil.muulnaat

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max

/**
 * Edge-aware perturbation module.
 *
 * Intent: keep perturbations concentrated around high-frequency regions where
 * restoration models tend to reconstruct semantics, while avoiding unnecessary
 * distortion on smooth areas.
 */
object NoiseEngine : PerturbationModule {
    private const val NOISE_BASE = 2f
    private const val NOISE_STRENGTH_MULTIPLIER = 2.5f

    override fun applyProtection(source: Bitmap, strength: Int): Bitmap {
        val safeStrength = strength.coerceIn(0, 100)
        val width = source.width
        val height = source.height

        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val edgeMap = buildEdgeMap(srcPixels, width, height)
        val outPixels = IntArray(srcPixels.size)
        var seed = ((width * 73856093) xor (height * 19349663) xor (safeStrength * 83492791)).toUInt().toInt()

        val baseNoise = NOISE_BASE + safeStrength * NOISE_STRENGTH_MULTIPLIER

        for (i in srcPixels.indices) {
            val pixel = srcPixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            val edgeBoost = 1f + edgeMap[i] * 0.9f
            val amplitude = baseNoise * edgeBoost

            seed = xorshift32(seed)
            val n1 = ((seed and 0xFF) - 128) / 128f
            seed = xorshift32(seed)
            val n2 = ((seed and 0xFF) - 128) / 128f
            seed = xorshift32(seed)
            val n3 = ((seed and 0xFF) - 128) / 128f

            val nr = clampChannel(r + (n1 * amplitude * 1.1f).toInt())
            val ng = clampChannel(g + (n2 * amplitude).toInt())
            val nb = clampChannel(b + (n3 * amplitude * 1.15f).toInt())

            outPixels[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            it.setPixels(outPixels, 0, width, 0, 0, width, height)
        }
    }

    fun protect(source: Bitmap, strength: Int): Bitmap = applyProtection(source, strength)

    private fun buildEdgeMap(pixels: IntArray, width: Int, height: Int): FloatArray {
        val map = FloatArray(pixels.size)
        if (width < 3 || height < 3) return map

        for (y in 1 until (height - 1)) {
            for (x in 1 until (width - 1)) {
                val idx = y * width + x
                val left = luma(pixels[idx - 1])
                val right = luma(pixels[idx + 1])
                val up = luma(pixels[idx - width])
                val down = luma(pixels[idx + width])

                val gx = abs(right - left)
                val gy = abs(down - up)
                val gradient = max(gx, gy)
                map[idx] = (gradient / 255f).coerceIn(0f, 1f)
            }
        }
        return map
    }

    private fun luma(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun clampChannel(value: Int): Int = value.coerceIn(0, 255)

    private fun xorshift32(state: Int): Int {
        var x = if (state == 0) 0x12345678 else state
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        return x
    }
}
