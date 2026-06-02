package com.haanghil.muulnaat

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

/**
 * Gallery save path.
 *
 * MediaStore is used instead of raw filesystem paths so Android version-specific
 * gallery rules are handled by the platform.
 */
fun ImageStore.saveImageToGallery(context: Context, image: Bitmap): GallerySaveResult {
    val filename = "haan_ghil_muulnaat_${System.currentTimeMillis()}.png"
    val resolver = context.contentResolver
    val values = imageContentValues(filename)
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return GallerySaveResult(success = false, failure = GallerySaveFailure.CREATE_ENTRY)

    return try {
        val success = resolver.openOutputStream(uri)?.use { output ->
            image.compress(Bitmap.CompressFormat.PNG, 100, output).also { output.flush() }
        } == true
        if (success) {
            markWriteCompleteIfNeeded(context, values, uri)
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

private fun imageContentValues(filename: String): ContentValues {
    return ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
        put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Haan Ghil Muulnaat")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
}

private fun markWriteCompleteIfNeeded(context: Context, values: ContentValues, uri: android.net.Uri) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
    values.clear()
    values.put(MediaStore.Images.Media.IS_PENDING, 0)
    context.contentResolver.update(uri, values, null, null)
}
