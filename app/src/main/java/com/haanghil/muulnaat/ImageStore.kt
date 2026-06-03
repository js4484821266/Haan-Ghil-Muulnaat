package com.haanghil.muulnaat

/**
 * Android 이미지 입출력 헬퍼를 모아 두는 네임스페이스입니다.
 *
 * 주변 ImageStore* 파일의 Kotlin 확장 함수들은 공개 호출 형태
 * (`ImageStore.loadBitmapFromUri(...)`)를 유지하면서 각 작업을 혼자 읽기
 * 충분히 작은 단위로 나눕니다.
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
