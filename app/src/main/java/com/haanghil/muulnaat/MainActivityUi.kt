package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.view.View
import androidx.appcompat.app.AlertDialog
import kotlin.math.abs

internal fun MainActivity.clearResultCards() {
    binding.protectionStatusValue.text = getString(R.string.status_na)
    binding.modelFaceCountValue.text = getString(R.string.metric_face_count_placeholder)
    binding.modelLabelShiftValue.text = getString(R.string.metric_label_shift_placeholder)
    binding.modelScoreValue.text = getString(R.string.metric_score_placeholder)
    binding.qualityPsnrValue.text = getString(R.string.metric_psnr_placeholder)
    binding.qualityDeltaValue.text = getString(R.string.metric_delta_placeholder)
    binding.qualityEdgeDeltaValue.text = getString(R.string.metric_edge_delta_placeholder)
}

internal fun MainActivity.statusLabel(status: ProtectionStatus): String = when (status) {
    ProtectionStatus.PASS -> getString(R.string.status_pass)
    ProtectionStatus.BROKEN -> getString(R.string.status_broken)
    ProtectionStatus.HELD -> getString(R.string.status_held)
}

internal fun MainActivity.setBusy(isBusy: Boolean, message: String? = null) {
    binding.busyProgressBar.visibility = if (isBusy) View.VISIBLE else View.GONE
    binding.pickButton.isEnabled = !isBusy
    binding.autoRecoverySwitch.isEnabled = !isBusy
    binding.applyButton.isEnabled = !isBusy
    binding.attackButton.isEnabled = !isBusy
    binding.resetOptimalButton.isEnabled = !isBusy && state.optimalStrength != null
    binding.saveButton.isEnabled = !isBusy
    binding.strengthSeekBar.isEnabled = !isBusy
    if (message != null) binding.resultText.text = message
}

internal fun MainActivity.showSearchProgress(message: String) {
    binding.searchProgressText.visibility = View.VISIBLE
    binding.searchProgressText.text = message
    binding.resetOptimalButton.visibility = View.GONE
}

internal fun MainActivity.showOptimalActionButton() {
    binding.searchProgressText.visibility = View.GONE
    binding.resetOptimalButton.visibility = View.VISIBLE
}

internal fun MainActivity.setTechnicalDetailsVisible(visible: Boolean) {
    binding.technicalDetailsContainer.visibility = if (visible) View.VISIBLE else View.GONE
    binding.technicalDetailsToggle.text = if (visible) {
        getString(R.string.technical_details_shown)
    } else {
        getString(R.string.technical_details_hidden)
    }
}

internal fun MainActivity.statusLabelForSearchStep(step: NoiseSearcher.SearchStep): String {
    return if (step.passed) getString(R.string.status_held) else getString(R.string.status_broken)
}

internal fun MainActivity.renderDefenseResult(
    status: ProtectionStatus,
    evaluationMetrics: EvaluationMetrics,
    qualityMetrics: QualityMetrics,
) {
    binding.protectionStatusValue.text = statusLabel(status)
    binding.modelFaceCountValue.text =
        getString(R.string.metric_face_count_format, evaluationMetrics.faceCountOriginal, evaluationMetrics.faceCountAfterAttack)
    binding.modelLabelShiftValue.text = getString(R.string.metric_label_shift_format, evaluationMetrics.labelShift)
    binding.modelScoreValue.text = getString(R.string.metric_score_format, evaluationMetrics.antiDetectionScore)
    binding.qualityPsnrValue.text = getString(R.string.metric_psnr_format, qualityMetrics.psnr, psnrToPercent(qualityMetrics.psnr))
    binding.qualityDeltaValue.text = getString(R.string.metric_delta_format, qualityMetrics.meanAbsDelta)
    binding.qualityEdgeDeltaValue.text = getString(R.string.metric_edge_delta_format, qualityMetrics.edgeDelta)
}

internal fun MainActivity.showManualDialog() {
    AlertDialog.Builder(this)
        .setTitle(getString(R.string.help_dialog_title))
        .setMessage(getString(R.string.help_dialog_message))
        .setPositiveButton(getString(R.string.help_dialog_positive)) { dialog, _ -> dialog.dismiss() }
        .show()
}

internal fun MainActivity.renderProtectedImage(reference: Bitmap, protected: Bitmap) {
    binding.noisyImage.setImageBitmap(protected)
    val meanAbsDelta = computeMeanAbsoluteDifference(reference, protected)
    binding.perturbationSummaryText.text =
        getString(R.string.perturbation_magnitude_format, meanAbsDelta, (meanAbsDelta / 255.0) * 100.0)
}

internal fun MainActivity.renderRecoveredImage(recovered: Bitmap?) {
    binding.recoveredImage.setImageBitmap(recovered)
}

private fun psnrToPercent(psnr: Double): Double {
    val normalized = (psnr - 8.0) / (50.0 - 8.0)
    return (normalized * 100.0).coerceIn(0.0, 100.0)
}

private fun computeMeanAbsoluteDifference(reference: Bitmap, tested: Bitmap): Double {
    val width = minOf(reference.width, tested.width)
    val height = minOf(reference.height, tested.height)
    val refPixels = IntArray(width * height)
    val testedPixels = IntArray(width * height)
    reference.getPixels(refPixels, 0, width, 0, 0, width, height)
    tested.getPixels(testedPixels, 0, width, 0, 0, width, height)

    var totalDiff = 0.0
    for (i in refPixels.indices) {
        val ref = refPixels[i]
        val dst = testedPixels[i]
        val dr = abs(((ref shr 16) and 0xFF) - ((dst shr 16) and 0xFF))
        val dg = abs(((ref shr 8) and 0xFF) - ((dst shr 8) and 0xFF))
        val db = abs((ref and 0xFF) - (dst and 0xFF))
        totalDiff += (dr + dg + db) / 3.0
    }
    return totalDiff / testedPixels.size
}
