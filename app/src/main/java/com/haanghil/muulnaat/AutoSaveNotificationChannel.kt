package com.haanghil.muulnaat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Notification channel setup for Android O+.
 */
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
