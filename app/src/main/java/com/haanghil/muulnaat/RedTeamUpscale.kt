package com.haanghil.muulnaat

import android.graphics.Bitmap

private const val RESTORATION_UPSCALE_FACTOR = 2

/**
 * 얼굴 특징이 복원될 수 있는 상황을 더 보수적으로 보려고 2배 확대합니다.
 * Android의 filtered scaling을 사용해 외부 모델이나 다운로드 없이 재현합니다.
 */
internal fun upscaleForRestoration(source: Bitmap): Bitmap {
    val targetWidth = source.width * RESTORATION_UPSCALE_FACTOR
    val targetHeight = source.height * RESTORATION_UPSCALE_FACTOR
    return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
}
