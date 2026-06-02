package com.haanghil.muulnaat

import android.net.Uri
import kotlin.concurrent.thread

/**
 * Entry point for visible batch processing.
 *
 * The foreground service handles silent auto-save. This path is for batches that
 * still mirror each item on the main screen so the user can see what happened.
 */
internal fun MainActivity.processImageBatch(uris: List<Uri>) {
    if (uris.isEmpty()) return

    resetBatchUi(uris.size)
    // Batch mode reuses the same protection pipeline while keeping progress visible on the main screen.
    thread {
        var savedCount = 0
        var skippedCount = 0

        uris.forEachIndexed { index, uri ->
            val result = processBatchItem(index + 1, uris.size, uri)
            if (result) savedCount += 1 else skippedCount += 1
        }

        runOnUiThread {
            showSearchProgress(getString(R.string.result_batch_complete, savedCount, skippedCount))
            binding.resultText.text = getString(R.string.result_batch_complete, savedCount, skippedCount)
            setBusy(false)
        }
    }
}

/**
 * Clears single-image state before a batch starts.
 *
 * The batch will reuse the same preview widgets for each item, so stale images
 * and metrics must be removed up front.
 */
private fun MainActivity.resetBatchUi(total: Int) {
    state.originalBitmap = null
    state.protectedBitmap = null
    state.optimalStrength = null
    state.lastAppliedStrength = null
    state.lastEvaluationStrength = null
    clearResultCards()
    binding.noisyImage.setImageDrawable(null)
    binding.recoveredImage.setImageDrawable(null)
    binding.perturbationSummaryText.text = getString(R.string.perturbation_summary_placeholder)
    binding.recommendedStrengthText.text = getString(R.string.recommended_strength_default)
    showSearchProgress(getString(R.string.result_batch_start, total))
    setBusy(true, getString(R.string.result_batch_start, total))
}

/**
 * Runs one batch item from load through save.
 *
 * Returns true only when the protected PNG is actually written to the gallery.
 */
private fun MainActivity.processBatchItem(itemNumber: Int, total: Int, uri: Uri): Boolean {
    runOnUiThread {
        binding.resultText.text = getString(R.string.result_batch_item_processing, itemNumber, total)
        showSearchProgress(getString(R.string.result_batch_item_processing, itemNumber, total))
    }

    val loaded = ImageStore.loadBitmapFromUri(this, uri)
    if (loaded == null) {
        runOnUiThread {
            binding.resultText.text = getString(R.string.result_batch_item_load_failed, itemNumber, total)
        }
        return false
    }

    // The latest item is mirrored in the UI so batch processing remains inspectable instead of opaque.
    runOnUiThread {
        prepareLoadedImage(loaded)
        setBusy(true, getString(R.string.result_batch_item_processing, itemNumber, total))
    }

    val minStrength = findBatchStrength(loaded, itemNumber, total) ?: return false
    val protected = perturbationModule.applyProtection(loaded, minStrength)
    val defenseReport = if (binding.autoRecoverySwitch.isChecked) {
        defenseEvaluator.evaluateAfterAttack(loaded, protected)
    } else {
        null
    }
    val saveResult = saveImageToGallery(protected)

    runOnUiThread {
        renderBatchResult(loaded, protected, minStrength, defenseReport, saveResult, itemNumber, total)
    }
    return saveResult.success
}
