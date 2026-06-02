package com.haanghil.muulnaat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

internal const val AUTO_SAVE_CHANNEL_ID = "protection_jobs"
internal const val AUTO_SAVE_NOTIFICATION_ID = 2101
private const val OPEN_APP_REQUEST_CODE = 2102
private const val CANCEL_REQUEST_CODE = 2103

internal fun AutoSaveProtectionService.completedCount(): Int = savedCount + skippedCount

internal fun AutoSaveProtectionService.updateProgressNotification(
    progress: Int,
    total: Int,
    title: String,
    message: String,
    showCancel: Boolean,
) {
    getSystemService(NotificationManager::class.java).notify(
        AUTO_SAVE_NOTIFICATION_ID,
        buildProgressNotification(progress, total, title, message, showCancel)
    )
}

internal fun AutoSaveProtectionService.buildProgressNotification(
    progress: Int,
    total: Int,
    title: String,
    message: String,
    showCancel: Boolean,
) = NotificationCompat.Builder(this, AUTO_SAVE_CHANNEL_ID)
    .setSmallIcon(android.R.drawable.stat_sys_upload_done)
    .setContentTitle(title)
    .setContentText(message)
    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
    .setContentIntent(openAppPendingIntent(title, message))
    .setOngoing(showCancel)
    .setOnlyAlertOnce(true)
    .setProgress(total, progress.coerceIn(0, total), false)
    .apply {
        if (showCancel) {
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_autosave_cancel_action),
                cancelPendingIntent()
            )
        }
    }
    .build()

internal fun AutoSaveProtectionService.finishWorker() {
    val wasCanceled = cancelRequested
    synchronized(queueLock) {
        if (wasCanceled) {
            skippedCount += queue.size
            queue.clear()
        }
        workerRunning = false
    }

    val title = getString(
        if (wasCanceled) R.string.notification_autosave_canceled_title else R.string.notification_autosave_complete_title
    )
    val message = if (wasCanceled) {
        getString(R.string.notification_autosave_canceled_body, savedCount, skippedCount)
    } else {
        getString(R.string.notification_autosave_complete_body, savedCount, skippedCount)
    }
    val notification = buildProgressNotification(
        progress = completedCount().coerceAtLeast(totalCount),
        total = totalCount.coerceAtLeast(1),
        title = title,
        message = message,
        showCancel = false,
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        stopForeground(ServiceCompatStop.detachFlag())
    } else {
        @Suppress("DEPRECATION")
        stopForeground(false)
    }
    getSystemService(NotificationManager::class.java).notify(AUTO_SAVE_NOTIFICATION_ID, notification)
    stopSelf()
}

internal fun AutoSaveProtectionService.createNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val channel = NotificationChannel(
        AUTO_SAVE_CHANNEL_ID,
        getString(R.string.notification_autosave_channel_name),
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = getString(R.string.notification_autosave_channel_description)
    }
    getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

private fun AutoSaveProtectionService.openAppPendingIntent(title: String, message: String): PendingIntent {
    val intent = Intent(this, MainActivity::class.java).apply {
        putExtra(ShareContract.EXTRA_STATUS_MESSAGE, title)
        putExtra(ShareContract.EXTRA_PROGRESS_MESSAGE, message)
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
        this,
        OPEN_APP_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private fun AutoSaveProtectionService.cancelPendingIntent(): PendingIntent {
    val intent = Intent(this, AutoSaveProtectionService::class.java).apply {
        action = ShareContract.ACTION_CANCEL_AUTO_SAVE
    }
    return PendingIntent.getService(
        this,
        CANCEL_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

private object ServiceCompatStop {
    fun detachFlag(): Int = android.app.Service.STOP_FOREGROUND_DETACH
}
