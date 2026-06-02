package com.haanghil.muulnaat

/**
 * Namespace for Android image I/O helpers.
 *
 * Kotlin extension functions in the neighboring ImageStore* files keep the
 * public call shape (`ImageStore.loadBitmapFromUri(...)`) while each operation
 * stays small enough to read alone.
 */
object ImageStore

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

internal const val IMAGE_STORE_TAG = "ImageStore"
internal const val IMAGE_STORE_TARGET_MAX_SIDE = 1280
