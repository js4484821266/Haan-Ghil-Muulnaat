package com.haanghil.muulnaat

import android.graphics.PointF
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

/**
 * 얼굴 보호 영역 목록을 픽셀 단위 마스크로 바꿉니다.
 *
 * null은 "전역 적용"이라는 기존 동작을 보존하고,
 * 빈 리스트는 "적용할 얼굴 영역 없음"으로 처리합니다.
 */
object NoiseRegionMask {
    fun build(width: Int, height: Int, regions: List<FaceProtectionRegion>?): BooleanArray? {
        if (regions == null) return null

        return buildForMaskRegions(
            width = width,
            height = height,
            regions = regions.map { region ->
                MaskRegion(
                    bounds = region.bounds.toMaskBounds(),
                    outlinePoints = region.outlinePoints.map { point -> point.toMaskPoint() },
                )
            },
        )
    }

    internal fun buildForMaskRegions(
        width: Int,
        height: Int,
        regions: List<MaskRegion>,
    ): BooleanArray {
        val mask = BooleanArray(width * height)

        for (region in regions) {
            val bounds = region.bounds.clipTo(width, height) ?: continue
            val outline = region.outlinePoints
                .map { point -> point.clipTo(width, height) }
                .distinctBy { point -> point.x to point.y }

            if (outline.size >= MIN_POLYGON_POINTS) {
                fillPolygon(mask, width, bounds, outline)
            } else {
                fillOval(mask, width, bounds)
            }
        }

        return mask
    }

    private fun fillOval(mask: BooleanArray, imageWidth: Int, bounds: MaskBounds) {
        val centerX = (bounds.left + bounds.right) / 2f
        val centerY = (bounds.top + bounds.bottom) / 2f
        val radiusX = max(0.5f, (bounds.right - bounds.left) / 2f)
        val radiusY = max(0.5f, (bounds.bottom - bounds.top) / 2f)

        for (y in bounds.top until bounds.bottom) {
            val rowOffset = y * imageWidth
            val py = y + 0.5f
            val normalizedY = (py - centerY) / radiusY
            for (x in bounds.left until bounds.right) {
                val px = x + 0.5f
                val normalizedX = (px - centerX) / radiusX
                if (normalizedX * normalizedX + normalizedY * normalizedY <= 1f) {
                    mask[rowOffset + x] = true
                }
            }
        }
    }

    private fun fillPolygon(
        mask: BooleanArray,
        imageWidth: Int,
        bounds: MaskBounds,
        points: List<MaskPoint>,
    ) {
        val left = max(bounds.left, points.minOf { it.x }.toInt())
        val top = max(bounds.top, points.minOf { it.y }.toInt())
        val right = min(bounds.right, points.maxOf { it.x }.toInt() + 1)
        val bottom = min(bounds.bottom, points.maxOf { it.y }.toInt() + 1)

        if (left >= right || top >= bottom) return

        for (y in top until bottom) {
            val rowOffset = y * imageWidth
            val py = y + 0.5f
            for (x in left until right) {
                val px = x + 0.5f
                if (pointInPolygon(px, py, points)) {
                    mask[rowOffset + x] = true
                }
            }
        }
    }

    private fun pointInPolygon(x: Float, y: Float, points: List<MaskPoint>): Boolean {
        var inside = false
        var previous = points.last()

        for (current in points) {
            if (pointOnSegment(x, y, previous, current)) return true

            val crossesY = (current.y > y) != (previous.y > y)
            if (crossesY) {
                val intersectionX =
                    (previous.x - current.x) * (y - current.y) / (previous.y - current.y) + current.x
                if (x < intersectionX) inside = !inside
            }
            previous = current
        }

        return inside
    }

    private fun pointOnSegment(x: Float, y: Float, start: MaskPoint, end: MaskPoint): Boolean {
        val cross = (x - start.x) * (end.y - start.y) - (y - start.y) * (end.x - start.x)
        if (kotlin.math.abs(cross) > SEGMENT_EPSILON) return false

        val minX = min(start.x, end.x) - SEGMENT_EPSILON
        val maxX = max(start.x, end.x) + SEGMENT_EPSILON
        val minY = min(start.y, end.y) - SEGMENT_EPSILON
        val maxY = max(start.y, end.y) + SEGMENT_EPSILON
        return x in minX..maxX && y in minY..maxY
    }

    private fun Rect.toMaskBounds(): MaskBounds =
        MaskBounds(left = left, top = top, right = right, bottom = bottom)

    private fun PointF.toMaskPoint(): MaskPoint = MaskPoint(x = x, y = y)

    private const val MIN_POLYGON_POINTS = 3
    private const val SEGMENT_EPSILON = 0.001f
}

internal data class MaskRegion(
    val bounds: MaskBounds,
    val outlinePoints: List<MaskPoint> = emptyList(),
)

internal data class MaskBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    fun clipTo(width: Int, height: Int): MaskBounds? {
        val clipped = MaskBounds(
            left = left.coerceIn(0, width),
            top = top.coerceIn(0, height),
            right = right.coerceIn(0, width),
            bottom = bottom.coerceIn(0, height),
        )

        return if (clipped.left < clipped.right && clipped.top < clipped.bottom) clipped else null
    }
}

internal data class MaskPoint(
    val x: Float,
    val y: Float,
) {
    fun clipTo(width: Int, height: Int): MaskPoint =
        MaskPoint(x = x.coerceIn(0f, width.toFloat()), y = y.coerceIn(0f, height.toFloat()))
}
