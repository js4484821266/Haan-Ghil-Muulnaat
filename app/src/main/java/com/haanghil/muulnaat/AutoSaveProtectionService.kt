package com.haanghil.muulnaat

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import java.util.ArrayDeque

/**
 * Foreground service shell for background image protection.
 *
 * The service owns queue state and Android lifecycle callbacks. Queue mutation,
 * worker execution, notifications, and start-intent construction live in small
 * neighboring files.
 */
class AutoSaveProtectionService : Service() {
    internal val queue = ArrayDeque<Uri>()
    internal val queueLock = Any()
    internal val perturbationModule: PerturbationModule = NoiseEngine
    internal val defenseEvaluator: DefenseEvaluator = RestorationAttackProbe()

    @Volatile
    internal var workerRunning = false

    @Volatile
    internal var cancelRequested = false

    internal var totalCount = 0
    internal var savedCount = 0
    internal var skippedCount = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ShareContract.ACTION_CANCEL_AUTO_SAVE -> requestCancel()
            ShareContract.ACTION_START_AUTO_SAVE -> enqueueWork(intent, startId)
            else -> stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    private fun requestCancel() {
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

    private fun enqueueWork(intent: Intent, startId: Int) {
        val uris = intent.sharedUris()
        if (uris.isEmpty()) {
            stopSelfResult(startId)
            return
        }

        synchronized(queueLock) {
            if (!workerRunning && queue.isEmpty()) resetCounts()
            queue.addAll(uris)
            totalCount += uris.size
        }
        cancelRequested = false

        if (!workerRunning) {
            workerRunning = true
            startForeground(
                AUTO_SAVE_NOTIFICATION_ID,
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

    private fun resetCounts() {
        totalCount = 0
        savedCount = 0
        skippedCount = 0
    }

    companion object
}
