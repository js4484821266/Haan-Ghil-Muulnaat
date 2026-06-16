package com.haanghil.muulnaat

import android.graphics.Bitmap

/**
 * 노이즈를 되돌리려는 "레드팀" AI 공격을 흉내 냅니다.
 *
 * 실제 생성형 복원 모델을 탑재하지는 않지만, 보호 이미지가 흔히 거칠 수 있는
 * denoising, 2x upscaling, sharpening 순서를 고정해 방어 평가 입력을 만듭니다.
 */
object RedTeamEngine {

    fun simulateAttack(noisyBitmap: Bitmap): Bitmap {
        val denoised = denoiseForRestoration(noisyBitmap)
        val upscaled = upscaleForRestoration(denoised)
        return sharpenForRestoration(upscaled)
    }
}
