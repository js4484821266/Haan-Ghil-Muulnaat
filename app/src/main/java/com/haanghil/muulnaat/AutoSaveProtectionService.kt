package com.haanghil.muulnaat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.ArrayDeque
import kotlin.concurrent.thread

class AutoSaveProtectionService : Service() {
    private val queue = ArrayDeque<Uri>()
    private val queueLock = Any()
    private val perturbationModule: PerturbationModule = NoiseEngine
    private val defenseEvaluator: DefenseEvaluator = RestorationAttackProbe()

    @Volatile
    private var workerRunning = false

    @Volatile
    private var cancelRequested = false

    private var totalCount = 0
    private var savedCount = 0
    private var skippedCount = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ShareContract.ACTION_CANCEL_AUTO_SAVE -> {
                cancelRequested = true
                synchronized(queueLock) {
                    skippedCount += queue.size
                    queue.clear()
                }
                updateProgressNotification(
                    progress = completedCount(),
                    total = totalCount.coerceAtLeast(1),
                    title = getString(R.string.notification_autosave_canceling_title),
                    message = getString(R.string.notification_autosave_canceling_body),
                    showCancel = false,
                )
            }
            ShareContract.ACTION_START_AUTO_SAVE -> {
                val uris = intent.sharedUris()
                if (uris.isEmpty()) {
                    stopSelfResult(startId)
                    return START_NOT_STICKY
                }

                synchronized(queueLock) {
                    if (!workerRunning && queue.isEmpty()) {
                        totalCount = 0
                        savedCount = 0
                        skippedCount = 0
                    }
                    queue.addAll(uris)
                    totalCount += uris.size
                }
                cancelRequested = false

                if (!workerRunning) {
                    workerRunning = true
                    startForeground(
                        NOTIFICATION_ID,
                        buildProgressNotification(
                            progress = completedCount(),
                            total = totalCount,
                            title = getString(R.string.notification_autosave_running_title),
                            message = getString(R.string.notification_autosave_starting, totalCount),
                            showCancel = true,
                        )
                    )
                    startWorker()
                } else {
                    updateProgressNotification(
                        progress = completedCount(),
                        total = totalCount,
                        title = getString(R.string.notification_autosave_running_title),
                        message = getString(R.string.notification_autosave_queued, totalCount),
                        showCancel = true,
                    )
                }
            }
            else -> stopSelfResult(startId)
        }

        return START_NOT_STICKY
    }

    private fun startWorker() {
        thread(name = "HaanGhilMuulnaatAutoSave") {
            while (true) {
                if (cancelRequested) break

                val item = synchronized(queueLock) {
                    queue.poll()
                } ?: break

                val itemNumber = completedCount() + 1
                val total = totalCount.coerceAtLeast(itemNumber)
                updateProgressNotification(
                    progress = completedCount(),
                    total = total,
                    title = getString(R.string.notification_autosave_running_title),
                    message = getString(R.string.notification_autosave_loading, itemNumber, total),
                    showCancel = true,
                )

                val loaded = ImageStore.loadBitmapFromUri(this, item)
                if (loaded == null) {
                    skippedCount += 1
                    updateProgressNotification(
                        progress = completedCount(),
                        total = totalCount.coerceAtLeast(1),
                        title = getString(R.string.notification_autosave_running_title),
                        message = getString(R.string.notification_autosave_load_failed, itemNumber, total),
                        showCancel = true,
                    )
                    continue
                }

                if (cancelRequested) {
                    skippedCount += 1
                    break
                }

                val minStrength = StrengthAdvisor.findRecommendedStrength(
                    original = loaded,
                    perturbationModule = perturbationModule,
                    defenseEvaluator = defenseEvaluator,
                    onStep = { step ->
                        updateProgressNotification(
                            progress = completedCount(),
                            total = totalCount.coerceAtLeast(1),
                            title = getString(R.string.notification_autosave_running_title),
                            message = getString(
                                R.string.notification_autosave_searching,
                                itemNumber,
                                totalCount.coerceAtLeast(itemNumber),
                                step.mid,
                            ),
                            showCancel = true,
                        )
                    },
                    shouldCancel = { cancelRequested },
                )

                if (cancelRequested) {
                    skippedCount += 1
                    break
                }

                if (minStrength == null) {
                    skippedCount += 1
                    updateProgressNotification(
                        progress = completedCount(),
                        total = totalCount.coerceAtLeast(1),
                        title = getString(R.string.notification_autosave_running_title),
                        message = getString(R.string.notification_autosave_no_strength, itemNumber, total),
                        showCancel = true,
                    )
                    continue
                }

                updateProgressNotification(
                    progress = completedCount(),
                    total = totalCount.coerceAtLeast(1),
                    title = getString(R.string.notification_autosave_running_title),
                    message = getString(R.string.notification_autosave_saving, itemNumber, total, minStrength),
                    showCancel = true,
                )

                val protected = perturbationModule.applyProtection(loaded, minStrength)
                if (cancelRequested) {
                    skippedCount += 1
                    break
                }

                val saveResult = ImageStore.saveImageToGallery(this, protected)
                if (saveResult.success) {
                    savedCount += 1
                } else {
                    skippedCount += 1
                }

                updateProgressNotification(
                    progress = completedCount(),
                    total = totalCount.coerceAtLeast(1),
                    title = getString(R.string.notification_autosave_running_title),
                    message = if (saveResult.success) {
                        getString(R.string.notification_autosave_item_saved, itemNumber, total)
                    } else {
                        getString(R.string.notification_autosave_item_failed, itemNumber, total)
                    },
                    showCancel = true,
                )
            }

            finishWorker()
        }
    }

    private fun finishWorker() {
        val wasCanceled = cancelRequested
        synchronized(queueLock) {
            if (wasCanceled) {
                skippedCount += queue.size
                queue.clear()
            }
            workerRunning = false
        }

        val title = if (wasCanceled) {
            getString(R.string.notification_autosave_canceled_title)
        } else {
            getString(R.string.notification_autosave_complete_title)
        }
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
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
        stopSelf()
    }

    private fun completedCount(): Int = savedCount + skippedCount

    private fun updateProgressNotification(
        progress: Int,
        total: Int,
        title: String,
        message: String,
        showCancel: Boolean,
    ) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            buildProgressNotification(
                progress = progress,
                total = total,
                title = title,
                message = message,
                showCancel = showCancel,
            )
        )
    }

    private fun buildProgressNotification(
        progress: Int,
        total: Int,
        title: String,
        message: String,
        showCancel: Boolean,
    ) = NotificationCompat.Builder(this, CHANNEL_ID)
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

    private fun openAppPendingIntent(title: String, message: String): PendingIntent {
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

    private fun cancelPendingIntent(): PendingIntent {
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_autosave_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_autosave_channel_description)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedUris(): List<Uri> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(ShareContract.EXTRA_URIS, Uri::class.java).orEmpty()
        } else {
            getParcelableArrayListExtra<Uri>(ShareContract.EXTRA_URIS).orEmpty()
        }
    }

    companion object {
        private const val CHANNEL_ID = "protection_jobs"
        private const val NOTIFICATION_ID = 2101
        private const val OPEN_APP_REQUEST_CODE = 2102
        private const val CANCEL_REQUEST_CODE = 2103

        fun start(context: Context, uris: List<Uri>) {
            val intent = Intent(context, AutoSaveProtectionService::class.java).apply {
                action = ShareContract.ACTION_START_AUTO_SAVE
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(ShareContract.EXTRA_URIS, ArrayList(uris))
                if (uris.isNotEmpty()) {
                    clipData = ClipData.newUri(context.contentResolver, "shared image", uris.first()).also { data ->
                        uris.drop(1).forEach { uri ->
                            data.addItem(ClipData.Item(uri))
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
