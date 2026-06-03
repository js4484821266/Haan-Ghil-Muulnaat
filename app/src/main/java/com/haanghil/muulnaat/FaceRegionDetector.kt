package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * 기기 안에서 얼굴 위치만 찾는 얇은 ML Kit 래퍼입니다.
 *
 * 반환된 Rect는 원본 Bitmap 좌표계 안으로 잘라 둡니다.
 * 호출자는 UI 스레드가 아니라 작업자 스레드에서 호출해야 합니다.
 */
object FaceRegionDetector {
    fun detectRegions(source: Bitmap): List<Rect> {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        val detector = FaceDetection.getClient(options)

        return try {
            val image = InputImage.fromBitmap(source, 0)
            Tasks.await(detector.process(image))
                .mapNotNull { face -> face.boundingBox.clipTo(source.width, source.height) }
        } catch (_: Exception) {
            emptyList()
        } finally {
            detector.close()
        }
    }

    private fun Rect.clipTo(width: Int, height: Int): Rect? {
        val clipped = Rect(
            left.coerceIn(0, width),
            top.coerceIn(0, height),
            right.coerceIn(0, width),
            bottom.coerceIn(0, height),
        )

        return if (clipped.width() > 0 && clipped.height() > 0) clipped else null
    }
}
