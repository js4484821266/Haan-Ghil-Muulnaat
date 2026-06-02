package com.haanghil.muulnaat

import android.app.NotificationManager
import android.os.Build

/**
 * Finalizes the foreground service worker.
 *
 * Cancelled work clears the remaining queue and reports those items as skipped;
 * normal completion leaves the final saved/skipped counts intact.
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
