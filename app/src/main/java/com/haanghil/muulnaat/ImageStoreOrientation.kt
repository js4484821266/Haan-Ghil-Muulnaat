package com.haanghil.muulnaat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log

/**
 * EXIF orientation handling.
 *
 * Many phone photos are stored sideways with orientation metadata. Correcting
 * that once at load time keeps perturbation, preview, and saved output aligned.
 */
internal fun readExifOrientation(context: Context, uri: Uri): Int {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (e: Exception) {
        Log.w(IMAGE_STORE_TAG, "Unable to read EXIF orientation: ${e.message}", e)
        ExifInterface.ORIENTATION_NORMAL
    }
}

internal fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
    val matrix = orientationMatrix(orientation) ?: return bitmap
    return try {
        val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (oriented != bitmap) bitmap.recycle()
        oriented
    } catch (e: Exception) {
        Log.w(IMAGE_STORE_TAG, "Unable to apply EXIF orientation: ${e.message}", e)
        bitmap
    }
}

private fun orientationMatrix(orientation: Int): Matrix? {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> matrix.rotateThenFlip(90f)
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> matrix.rotateThenFlip(270f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        else -> return null
    }
    return matrix
}

private fun Matrix.rotateThenFlip(degrees: Float) {
    postRotate(degrees)
    postScale(-1f, 1f)
}
