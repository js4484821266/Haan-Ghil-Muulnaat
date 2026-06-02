package com.haanghil.muulnaat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log

/**
 * URI-to-Bitmap loading.
 *
 * Android shares content URIs, not stable file paths. This helper reads bounds
 * first, downsamples large images, then applies EXIF orientation so the rest of
 * the pipeline can treat the bitmap as display-ready.
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
