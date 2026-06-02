package com.haanghil.muulnaat

import android.net.Uri
import kotlin.concurrent.thread

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

private fun AutoSaveProtectionService.notifyLoading(itemNumber: Int, total: Int) {
    updateProgressNotification(
        progress = completedCount(),
        total = total,
        title = getString(R.string.notification_autosave_running_title),
        message = getString(R.string.notification_autosave_loading, itemNumber, total),
        showCancel = true,
    )
}

private fun AutoSaveProtectionService.notifySkippedLoad(itemNumber: Int, total: Int) {
    updateProgressNotification(
        progress = completedCount(),
        total = totalCount.coerceAtLeast(1),
        title = getString(R.string.notification_autosave_running_title),
        message = getString(R.string.notification_autosave_load_failed, itemNumber, total),
        showCancel = true,
    )
}

private fun AutoSaveProtectionService.notifyNoStrength(itemNumber: Int, total: Int) {
    updateProgressNotification(
        progress = completedCount(),
        total = totalCount.coerceAtLeast(1),
        title = getString(R.string.notification_autosave_running_title),
        message = getString(R.string.notification_autosave_no_strength, itemNumber, total),
        showCancel = true,
    )
}

private fun AutoSaveProtectionService.notifySaving(itemNumber: Int, total: Int, minStrength: Int) {
    updateProgressNotification(
        progress = completedCount(),
        total = totalCount.coerceAtLeast(1),
        title = getString(R.string.notification_autosave_running_title),
        message = getString(R.string.notification_autosave_saving, itemNumber, total, minStrength),
        showCancel = true,
    )
}

private fun AutoSaveProtectionService.notifyItemSaved(itemNumber: Int, total: Int, success: Boolean) {
    updateProgressNotification(
        progress = completedCount(),
        total = totalCount.coerceAtLeast(1),
        title = getString(R.string.notification_autosave_running_title),
        message = if (success) {
            getString(R.string.notification_autosave_item_saved, itemNumber, total)
        } else {
            getString(R.string.notification_autosave_item_failed, itemNumber, total)
        },
        showCancel = true,
    )
}
