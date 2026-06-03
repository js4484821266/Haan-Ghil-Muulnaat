package com.haanghil.muulnaat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log

/**
 * URI를 Bitmap으로 읽어 오는 경로입니다.
 *
 * Android 공유는 안정적인 파일 경로가 아니라 content URI를 전달합니다. 이 헬퍼는
 * 먼저 이미지 크기만 읽고, 큰 이미지는 다운샘플링한 뒤, EXIF 방향을 적용해 이후
 * 파이프라인이 표시 가능한 비트맵으로 다룰 수 있게 합니다.
 */
fun ImageStore.loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return null

        applyExifOrientation(decoded, readExifOrientation(context, uri))
    } catch (e: Exception) {
        Log.e(IMAGE_STORE_TAG, "Error loading bitmap from URI: ${e.message}", e)
        null
    }
}

private fun sampleSizeFor(originalWidth: Int, originalHeight: Int): Int {
    var sampleSize = 1
    var width = originalWidth
    var height = originalHeight
    while (width > IMAGE_STORE_TARGET_MAX_SIDE || height > IMAGE_STORE_TARGET_MAX_SIDE) {
        width /= 2
        height /= 2
        sampleSize *= 2
    }
    return sampleSize
}
