package com.haanghil.muulnaat

import android.net.Uri
import kotlin.concurrent.thread

/**
 * Starts the service queue worker.
 *
 * The service owns exactly one worker at a time; later share intents append to
 * the queue and the existing worker drains them.
 */
internal fun AutoSaveProtectionService.startWorker() {
    // The foreground service owns one queue worker; new share intents append to the same queue.
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
 * Processes one URI from the auto-save queue.
 *
 * The item is skipped unless every step succeeds: load bitmap, find a held
 * strength, generate protection, and write to MediaStore.
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

    // A saved image is counted only after both minimum-strength search and gallery write succeed.
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
    val protected = perturbationModule.applyProtection(loaded, minStrength)
    if (cancelRequested) {
        skippedCount += 1
        return
    }

    val saveResult = ImageStore.saveImageToGallery(this, protected)
    if (saveResult.success) savedCount += 1 else skippedCount += 1
    notifyItemSaved(itemNumber, total, saveResult.success)
}

/**
 * Runs the expensive minimum-strength search with service-level cancellation.
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
