package com.haanghil.muulnaat

import android.graphics.Rect

/**
 * 얼굴 bounding box 목록을 픽셀 단위 마스크로 바꿉니다.
 *
 * null은 "전역 적용"이라는 기존 동작을 보존하고,
 * 빈 리스트는 "적용할 얼굴 영역 없음"으로 처리합니다.
 */
object NoiseRegionMask {
    fun build(width: Int, height: Int, regions: List<Rect>?): BooleanArray? {
        if (regions == null) return null

        val mask = BooleanArray(width * height)

        for (region in regions) {
            val left = region.left.coerceIn(0, width)
            val top = region.top.coerceIn(0, height)
            val right = region.right.coerceIn(0, width)
            val bottom = region.bottom.coerceIn(0, height)

            if (left >= right || top >= bottom) continue

            for (y in top until bottom) {
                val rowOffset = y * width
                for (x in left until right) {
                    mask[rowOffset + x] = true
                }
            }
        }

        return mask
    }
}
