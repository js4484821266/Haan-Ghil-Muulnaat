package com.haanghil.muulnaat

import android.app.NotificationManager
import android.os.Build

/**
 * 포그라운드 서비스 작업자를 마무리합니다.
 *
 * 취소된 작업은 남은 큐를 비우고 해당 항목을 건너뜀으로 집계합니다.
 * 정상 완료 시에는 최종 저장/건너뜀 개수를 그대로 유지합니다.
 */
internal fun AutoSaveProtectionService.finishWorker() {
    val wasCanceled = cancelRequested
    synchronized(queueLock) {
        if (wasCanceled) {
            skippedCount += queue.size
            queue.clear()
        }
        workerRunning = false
    }

    val notification = buildProgressNotification(
        progress = completedCount().coerceAtLeast(totalCount),
        total = totalCount.coerceAtLeast(1),
        title = finalTitle(wasCanceled),
        message = finalMessage(wasCanceled),
        showCancel = false,
    )
    detachForeground()
    getSystemService(NotificationManager::class.java).notify(AUTO_SAVE_NOTIFICATION_ID, notification)
    stopSelf()
}

private fun AutoSaveProtectionService.finalTitle(wasCanceled: Boolean): String {
    val titleRes = if (wasCanceled) {
        R.string.notification_autosave_canceled_title
    } else {
        R.string.notification_autosave_complete_title
    }
    return getString(titleRes)
}

private fun AutoSaveProtectionService.finalMessage(wasCanceled: Boolean): String {
    val messageRes = if (wasCanceled) {
        R.string.notification_autosave_canceled_body
    } else {
        R.string.notification_autosave_complete_body
    }
    return getString(messageRes, savedCount, skippedCount)
}

private fun AutoSaveProtectionService.detachForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        stopForeground(android.app.Service.STOP_FOREGROUND_DETACH)
    } else {
        @Suppress("DEPRECATION")
        stopForeground(false)
    }
}
