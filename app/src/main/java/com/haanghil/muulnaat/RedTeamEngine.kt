package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Simulates a "Red Team" AI attack by attempting to reverse the noise.
 * In a full implementation, this would use a TFLite model like ESRGAN-tiny.
 * This implementation uses a bilateral-inspired denoising
 * algorithm that mimics the "softening" and "edge-preserving" effects of AI restoration.
 */
object RedTeamEngine {

    fun simulateAttack(noisyBitmap: Bitmap): Bitmap {
        val width = noisyBitmap.width
        val height = noisyBitmap.height
        val pixels = IntArray(width * height)
        noisyBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(pixels.size)
        
        // Simulating AI Denoising: A combination of Median filtering (to remove salt-and-pepper)
        // and a pass of local averaging (to reduce Gaussian variance).
        for (y in 0 until height) {
            for (x in 0 until width) {
                val windowR = mutableListOf<Int>()
                val windowG = mutableListOf<Int>()
                val windowB = mutableListOf<Int>()

                // 3x3 Window for median/mean simulation
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val px = (x + kx).coerceIn(0, width - 1)
                        val py = (y + ky).coerceIn(0, height - 1)
                        val pixel = pixels[py * width + px]
                        windowR.add(Color.red(pixel))
                        windowG.add(Color.green(pixel))
                        windowB.add(Color.blue(pixel))
                    }
                }

                windowR.sort()
                windowG.sort()
                windowB.sort()

                // AI models often find a "consensus" value. We'll use a weighted average 
                // of the median and the mean to simulate "smart" denoising.
                val medianR = windowR[4]
                val meanR = windowR.average().toInt()
                val finalR = (medianR * 0.7 + meanR * 0.3).toInt()

                val medianG = windowG[4]
                val meanG = windowG.average().toInt()
                val finalG = (medianG * 0.7 + meanG * 0.3).toInt()

                val medianB = windowB[4]
                val meanB = windowB.average().toInt()
                val finalB = (medianB * 0.7 + meanB * 0.3).toInt()

                outPixels[y * width + x] = Color.rgb(finalR, finalG, finalB)
            }
        }

        // Second pass: Upscale/Sharpen simulation (Simulating ESRGAN's edge enhancement)
        // We'll apply a simple unsharp mask logic.
        val sharpenedPixels = outPixels.copyOf()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val idx = y * width + x
                val center = outPixels[idx]
                val surrounding = (outPixels[idx-1] + outPixels[idx+1] + outPixels[idx-width] + outPixels[idx+width])
                
                val r = Color.red(center)
                val g = Color.green(center)
                val b = Color.blue(center)
                
                val sr = Color.red(surrounding) / 4
                val sg = Color.green(surrounding) / 4
                val sb = Color.blue(surrounding) / 4

                val nr = (r + (r - sr) * 0.5).toInt().coerceIn(0, 255)
                val ng = (g + (g - sg) * 0.5).toInt().coerceIn(0, 255)
                val nb = (b + (b - sb) * 0.5).toInt().coerceIn(0, 255)

                sharpenedPixels[idx] = Color.rgb(nr, ng, nb)
            }
        }

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        result.setPixels(sharpenedPixels, 0, width, 0, 0, width, height)
        return result
    }
}
