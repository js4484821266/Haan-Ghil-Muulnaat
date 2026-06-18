package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * 기기 안에서 얼굴 위치와 외곽 contour를 찾는 얇은 ML Kit 래퍼입니다.
 *
 * 반환된 영역은 원본 Bitmap 좌표계 안으로 잘라 둡니다. contour가 없으면 호출자는
 * bounds 기반 fallback을 사용할 수 있습니다.
 * 호출자는 UI 스레드가 아니라 작업자 스레드에서 호출해야 합니다.
 */
object FaceRegionDetector {
    fun detectRegions(source: Bitmap): List<FaceProtectionRegion> {
        val image = InputImage.fromBitmap(source, 0)
        val baseDetector = FaceDetection.getClient(baseOptions())
        val contourDetector = FaceDetection.getClient(contourOptions())

        return try {
            val contourRegions = detectContourRegions(contourDetector, image, source.width, source.height)

            Tasks.await(baseDetector.process(image))
                .mapNotNull { face ->
                    val bounds = face.boundingBox.clipTo(source.width, source.height)
                        ?: return@mapNotNull null
                    FaceProtectionRegion(
                        bounds = bounds,
                        outlinePoints = contourRegions.bestOutlineFor(bounds),
                    )
                }
        } catch (_: Exception) {
            emptyList()
        } finally {
            baseDetector.close()
            contourDetector.close()
        }
    }

    private fun baseOptions(): FaceDetectorOptions =
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()

    private fun contourOptions(): FaceDetectorOptions =
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()

    private fun detectContourRegions(
        detector: com.google.mlkit.vision.face.FaceDetector,
        image: InputImage,
        width: Int,
        height: Int,
    ): List<FaceProtectionRegion> {
        return try {
            Tasks.await(detector.process(image))
                .mapNotNull { face ->
                    val bounds = face.boundingBox.clipTo(width, height)
                        ?: return@mapNotNull null
                    FaceProtectionRegion(
                        bounds = bounds,
                        outlinePoints = face.getContour(FaceContour.FACE)
                            ?.points
                            .orEmpty()
                            .map { point -> point.clipTo(width, height) },
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun List<FaceProtectionRegion>.bestOutlineFor(bounds: Rect): List<PointF> {
        val bestMatch = maxByOrNull { region -> overlapRatio(bounds, region.bounds) }
        val bestRatio = bestMatch?.let { overlapRatio(bounds, it.bounds) } ?: 0f
        return if (bestRatio >= MIN_CONTOUR_OVERLAP_RATIO) bestMatch?.outlinePoints.orEmpty() else emptyList()
    }

    private fun overlapRatio(first: Rect, second: Rect): Float {
        val left = maxOf(first.left, second.left)
        val top = maxOf(first.top, second.top)
        val right = minOf(first.right, second.right)
        val bottom = minOf(first.bottom, second.bottom)
        val intersection = area(left, top, right, bottom)
        if (intersection <= 0) return 0f

        val union = area(first.left, first.top, first.right, first.bottom) +
            area(second.left, second.top, second.right, second.bottom) - intersection
        return if (union > 0) intersection.toFloat() / union else 0f
    }

    private fun area(left: Int, top: Int, right: Int, bottom: Int): Int =
        maxOf(0, right - left) * maxOf(0, bottom - top)

    private fun Rect.clipTo(width: Int, height: Int): Rect? {
        val clipped = Rect(
            left.coerceIn(0, width),
            top.coerceIn(0, height),
            right.coerceIn(0, width),
            bottom.coerceIn(0, height),
        )

        return if (clipped.left < clipped.right && clipped.top < clipped.bottom) clipped else null
    }

    private fun PointF.clipTo(width: Int, height: Int): PointF =
        PointF(x.coerceIn(0f, width.toFloat()), y.coerceIn(0f, height.toFloat()))

    private const val MIN_CONTOUR_OVERLAP_RATIO = 0.30f
}
