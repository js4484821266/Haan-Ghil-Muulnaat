package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * 노이즈를 되돌리려는 "레드팀" AI 공격을 흉내 냅니다.
 * 완전한 구현이라면 ESRGAN-tiny 같은 TFLite 모델을 사용할 수 있습니다.
 * 현재 구현은 양방향 필터에서 착안한 노이즈 제거 알고리즘으로 AI 복원의
 * "부드럽게 만들기"와 "엣지 보존" 효과를 근사합니다.
 */
object RedTeamEngine {

    fun simulateAttack(noisyBitmap: Bitmap): Bitmap {
        val width = noisyBitmap.width
        val height = noisyBitmap.height
        val pixels = IntArray(width * height)
        noisyBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val outPixels = IntArray(pixels.size)
        
        // AI 노이즈 제거 근사입니다. 점 잡음(salt-and-pepper)을 줄이는 중앙값 필터와
        // 가우시안 분산을 낮추는 지역 평균 단계를 함께 사용합니다.
        for (y in 0 until height) {
            for (x in 0 until width) {
                val windowR = mutableListOf<Int>()
                val windowG = mutableListOf<Int>()
                val windowB = mutableListOf<Int>()

                // 중앙값/평균 근사용 3x3 창입니다.
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

                // AI 모델은 주변 픽셀의 "합의값"을 찾는 경우가 많습니다. 여기서는 중앙값과
                // 평균의 가중 평균으로 "영리한" 노이즈 제거를 근사합니다.
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

        // 두 번째 단계는 업스케일/샤픈 근사입니다. ESRGAN식 엣지 강화를 흉내 내기 위해
        // 단순 언샤프 마스크 로직을 적용합니다.
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
