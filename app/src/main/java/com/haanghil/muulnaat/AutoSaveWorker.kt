package com.haanghil.muulnaat

import android.net.Uri
import kotlin.concurrent.thread

/**
 * 서비스 큐 작업자를 시작합니다.
 *
 * 서비스는 한 번에 작업자 하나만 갖습니다. 뒤늦게 들어온 공유 Intent는 큐에
 * 붙고, 기존 작업자가 그 큐를 끝까지 비웁니다.
 */
internal fun AutoSaveProtectionService.startWorker() {
    // 포그라운드 서비스는 큐 작업자 하나를 소유하고, 새 공유 Intent는 같은 큐에 추가됩니다.
    thread(name = "HaanGhilMuulnaatAutoSave") {
        while (true) {
            if (cancelRequested) break
            val item = synchronized(queueLock) { queue.poll() } ?: break
            processQueueItem(item)
        }
        finishWorker()
    }
}

/**
 * 자동 저장 큐에서 URI 하나를 처리합니다.
 *
 * 비트맵 로드, 유지 가능한 강도 탐색, 보호본 생성, MediaStore 저장이 모두 성공해야
 * 저장된 항목으로 집계합니다.
 */
private fun AutoSaveProtectionService.processQueueItem(item: Uri) {
    val itemNumber = completedCount() + 1
    val total = totalCount.coerceAtLeast(itemNumber)
    notifyLoading(itemNumber, total)

    val loaded = ImageStore.loadBitmapFromUri(this, item)
    if (loaded == null) {
        skippedCount += 1
        notifySkippedLoad(itemNumber, total)
        return
    }
    if (cancelRequested) {
        skippedCount += 1
        return
    }

    // 최소 강도 탐색과 갤러리 저장이 모두 성공한 뒤에만 저장 성공으로 셉니다.
    val minStrength = findStrengthForItem(loaded, itemNumber, total)
    if (cancelRequested) {
        skippedCount += 1
        return
    }
    if (minStrength == null) {
        skippedCount += 1
        notifyNoStrength(itemNumber, total)
        return
    }

    notifySaving(itemNumber, total, minStrength)
    val faceRegions = FaceRegionDetector.detectRegions(loaded)
    val protected = perturbationModule.applyProtection(loaded, minStrength, faceRegions)
    if (cancelRequested) {
        skippedCount += 1
        return
    }

    val saveResult = ImageStore.saveImageToGallery(this, protected)
    if (saveResult.success) savedCount += 1 else skippedCount += 1
    notifyItemSaved(itemNumber, total, saveResult.success)
}

/**
 * 비용이 큰 최소 강도 탐색을 서비스 단위 취소 조건과 함께 실행합니다.
 */
private fun AutoSaveProtectionService.findStrengthForItem(
    loaded: android.graphics.Bitmap,
    itemNumber: Int,
    total: Int,
): Int? {
    return StrengthAdvisor.findRecommendedStrength(
        original = loaded,
        perturbationModule = perturbationModule,
        defenseEvaluator = defenseEvaluator,
        onStep = { step ->
            updateProgressNotification(
                progress = completedCount(),
                total = totalCount.coerceAtLeast(1),
                title = getString(R.string.notification_autosave_running_title),
                message = getString(R.string.notification_autosave_searching, itemNumber, total, step.mid),
                showCancel = true,
            )
        },
        shouldCancel = { cancelRequested },
    )
}
