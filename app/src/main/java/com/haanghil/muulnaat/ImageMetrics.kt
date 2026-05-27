package com.haanghil.muulnaat

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

data class ComparisonResult(
    val psnr: Double,
    val meanAbsDelta: Double,
    val edgeDelta: Double,
    val passed: Boolean,
    val details: String
)

object ImageMetrics {
    fun evaluate(original: Bitmap, protected: Bitmap): ComparisonResult {
        val width = minOf(original.width, protected.width)
        val height = minOf(original.height, protected.height)

        val a = if (original.width != width || original.height != height) {
            Bitmap.createScaledBitmap(original, width, height, true)
        } else {
            original
        }
        val b = if (protected.width != width || protected.height != height) {
            Bitmap.createScaledBitmap(protected, width, height, true)
        } else {
            protected
        }

        val p1 = IntArray(width * height)
        val p2 = IntArray(width * height)
        a.getPixels(p1, 0, width, 0, 0, width, height)
        b.getPixels(p2, 0, width, 0, 0, width, height)

        var mse = 0.0
        var mad = 0.0

        for (i in p1.indices) {
            val dR = ((p1[i] shr 16) and 0xFF) - ((p2[i] shr 16) and 0xFF)
            val dG = ((p1[i] shr 8) and 0xFF) - ((p2[i] shr 8) and 0xFF)
            val dB = (p1[i] and 0xFF) - (p2[i] and 0xFF)

            mse += (dR * dR + dG * dG + dB * dB) / 3.0
            mad += (abs(dR) + abs(dG) + abs(dB)) / 3.0
        }

        val n = p1.size.toDouble()
        mse /= n
        mad /= n

        val psnr = if (mse == 0.0) 99.0 else 10.0 * log10((255.0 * 255.0) / mse)
        val edgeDelta = edgeEnergyDelta(p1, p2, width, height)

        val pass = psnr >= 22.0 && mad >= 3.0 && edgeDelta >= 0.08
        val details = buildString {
            append("PSNR: %.2f dB, ".format(psnr))
            append("MeanAbsDelta: %.2f, ".format(mad))
            append("EdgeDelta: %.3f".format(edgeDelta))
        }

        return ComparisonResult(psnr, mad, edgeDelta, pass, details)
    }

    private fun edgeEnergyDelta(a: IntArray, b: IntArray, width: Int, height: Int): Double {
        if (width < 3 || height < 3) return 0.0

        var energyA = 0.0
        var energyB = 0.0
        var count = 0

        for (y in 1 until (height - 1)) {
            for (x in 1 until (width - 1)) {
                val idx = y * width + x
                val ax = gray(a[idx + 1]) - gray(a[idx - 1])
                val ay = gray(a[idx + width]) - gray(a[idx - width])
                val bx = gray(b[idx + 1]) - gray(b[idx - 1])
                val by = gray(b[idx + width]) - gray(b[idx - width])

                energyA += max(abs(ax), abs(ay)).toDouble()
                energyB += max(abs(bx), abs(by)).toDouble()
                count++
            }
        }

        if (count == 0 || energyA == 0.0) return 0.0
        val normA = energyA / count
        val normB = energyB / count
        return kotlin.math.abs(normB - normA) / normA
    }

    private fun gray(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val bl = pixel and 0xFF
        return (r * 299 + g * 587 + bl * 114) / 1000
    }
}
