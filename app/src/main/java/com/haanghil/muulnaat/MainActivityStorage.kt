package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread

internal fun MainActivity.runWithStoragePermissionIfNeeded(action: () -> Unit) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
        val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        val hasPermission = ContextCompat.checkSelfPermission(this, permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            state.pendingStoragePermissionAction = action
            writeStoragePermissionLauncher.launch(permission)
            return
        }
    }
    action()
}

internal fun MainActivity.saveProtectedImageToGallery() {
    val image = state.protectedBitmap ?: return
    saveImageToGalleryAsync(image)
}

internal fun MainActivity.saveImageToGalleryAsync(image: Bitmap) {
    setBusy(true, getString(R.string.result_saving_protected_image))
    thread {
        val result = saveImageToGallery(image)
        runOnUiThread {
            renderSaveResult(result)
            setBusy(false)
        }
    }
}

internal fun MainActivity.saveImageToGallery(image: Bitmap): GallerySaveResult {
    return ImageStore.saveImageToGallery(this, image)
}

private fun MainActivity.renderSaveResult(result: GallerySaveResult) {
    if (result.success) {
        Toast.makeText(this, getString(R.string.toast_saved_to_gallery), Toast.LENGTH_SHORT).show()
        binding.resultText.text = getString(R.string.result_saved_image, result.filename)
        return
    }

    binding.resultText.text = when (result.failure) {
        GallerySaveFailure.CREATE_ENTRY -> getString(R.string.result_save_failed_entry)
        GallerySaveFailure.WRITE_DATA -> getString(R.string.result_save_failed_write)
        GallerySaveFailure.ERROR -> getString(
            R.string.result_save_error,
            result.errorMessage ?: getString(R.string.error_unknown)
        )
        null -> getString(R.string.result_save_error, getString(R.string.error_unknown))
    }
}
