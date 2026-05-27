package com.haanghil.muulnaat

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log

enum class GallerySaveFailure {
    CREATE_ENTRY,
    WRITE_DATA,
    ERROR
}

data class GallerySaveResult(
    val success: Boolean,
    val filename: String? = null,
    val failure: GallerySaveFailure? = null,
    val errorMessage: String? = null,
)

object ImageStore {
    private const val TAG = "ImageStore"

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val resolver = context.contentResolver

            val optionsBounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, optionsBounds)
            }

            if (optionsBounds.outWidth <= 0 || optionsBounds.outHeight <= 0) return null

            val targetMaxSide = 1280
            var sampleSize = 1
            var width = optionsBounds.outWidth
            var height = optionsBounds.outHeight
            while (width > targetMaxSide || height > targetMaxSide) {
                width /= 2
                height /= 2
                sampleSize *= 2
            }

            val optionsDecode = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, optionsDecode)
            } ?: return null

            applyExifOrientation(decoded, readExifOrientation(context, uri))
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap from URI: ${e.message}", e)
            null
        }
    }

    fun saveImageToGallery(context: Context, image: Bitmap): GallerySaveResult {
        val filename = "haan_ghil_muulnaat_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Haan Ghil Muulnaat")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return GallerySaveResult(success = false, failure = GallerySaveFailure.CREATE_ENTRY)

        return try {
            val success = resolver.openOutputStream(uri)?.use { output ->
                image.compress(Bitmap.CompressFormat.PNG, 100, output).also {
                    output.flush()
                }
            } == true

            if (success) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                GallerySaveResult(success = true, filename = filename)
            } else {
                resolver.delete(uri, null, null)
                GallerySaveResult(success = false, failure = GallerySaveFailure.WRITE_DATA)
            }
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            GallerySaveResult(success = false, failure = GallerySaveFailure.ERROR, errorMessage = e.message)
        }
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read EXIF orientation: ${e.message}", e)
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }

        return try {
            val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (oriented != bitmap) {
                bitmap.recycle()
            }
            oriented
        } catch (e: Exception) {
            Log.w(TAG, "Unable to apply EXIF orientation: ${e.message}", e)
            bitmap
        }
    }
}
