package com.haanghil.muulnaat

import android.graphics.PointF
import android.graphics.Rect

/**
 * 보호 perturbation을 적용할 얼굴 영역입니다.
 *
 * [bounds]는 감지기가 준 얼굴의 기본 안전 범위이고, [outlinePoints]는 얼굴 외곽 contour를
 * 원본 Bitmap 좌표계로 표현한 점 목록입니다. contour가 없으면 bounds 안의 타원형 영역으로
 * 근사합니다.
 */
data class FaceProtectionRegion(
    val bounds: Rect,
    val outlinePoints: List<PointF> = emptyList(),
) {
    companion object {
        fun fromRect(rect: Rect): FaceProtectionRegion =
            FaceProtectionRegion(bounds = Rect(rect), outlinePoints = emptyList())
    }
}
