package com.haanghil.muulnaat

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.haanghil.muulnaat.databinding.ActivityMainBinding
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val perturbationModule: PerturbationModule = NoiseEngine
    private val defenseEvaluator: DefenseEvaluator = RestorationAttackProbe()

    private var originalBitmap: Bitmap? = null
    private var protectedBitmap: Bitmap? = null
    private var optimalStrength: Int? = null
    private var lastAppliedStrength: Int? = null
    private var lastEvaluationStrength: Int? = null
    private var pendingStoragePermissionAction: (() -> Unit)? = null

    private val writeStoragePermissionLauncher =
        registerForActivityResult(RequestPermission()) { granted ->
            val pendingAction = pendingStoragePermissionAction
            pendingStoragePermissionAction = null
            if (granted) {
                pendingAction?.invoke()
            } else {
                Toast.makeText(this, getString(R.string.toast_storage_permission_denied), Toast.LENGTH_SHORT).show()
                binding.resultText.text = getString(R.string.toast_storage_permission_denied)
                setBusy(false)
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri == null) {
                binding.resultText.text = getString(R.string.result_no_image_selected)
                return@registerForActivityResult
            }

            processSingleImageUri(uri, autoSaveAfterProtection = false)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.helpButton.setOnClickListener {
            showManualDialog()
        }

        setTechnicalDetailsVisible(false)
        binding.technicalDetailsToggle.setOnClickListener {
            val show = binding.technicalDetailsContainer.visibility != View.VISIBLE
            setTechnicalDetailsVisible(show)
        }

        binding.pickButton.setOnClickListener {
            pickImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.strengthSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.strengthLabel.text = getString(R.string.noise_strength_value, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        binding.applyButton.setOnClickListener {
            val source = originalBitmap
            if (source == null) {
                Toast.makeText(this, getString(R.string.toast_pick_image_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val strength = binding.strengthSeekBar.progress
            val shouldClear = lastEvaluationStrength != null && lastEvaluationStrength != strength
            val isAutoRecoveryOn = binding.autoRecoverySwitch.isChecked
            runProtectionFlow(
                source = source,
                strength = strength,
                autoRecovery = isAutoRecoveryOn,
                clearResultsBeforeRun = shouldClear,
                startMessage = getString(R.string.result_applying_protection)
            )
        }

        binding.attackButton.setOnClickListener {
            val source = protectedBitmap
            val original = originalBitmap
            if (source == null || original == null) {
                Toast.makeText(this, getString(R.string.toast_apply_protection_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.resultText.text = getString(R.string.result_running_recovery)
            setBusy(true, getString(R.string.result_running_recovery))
            thread {
                val defenseReport = defenseEvaluator.evaluateAfterAttack(original, source)

                runOnUiThread {
                    renderRecoveredImage(defenseReport.attackedBitmap)
                    renderDefenseResult(
                        status = defenseReport.status,
                        evaluationMetrics = defenseReport.evaluationMetrics,
                        qualityMetrics = defenseReport.qualityMetrics
                    )
                    lastEvaluationStrength = lastAppliedStrength ?: binding.strengthSeekBar.progress
                    val localizedStatus = statusLabel(defenseReport.status)
                    binding.resultText.text = getString(R.string.result_recovery_complete, localizedStatus)
                    setBusy(false)
                }
            }
        }

        binding.resetOptimalButton.setOnClickListener {
            val rememberedStrength = optimalStrength
            if (rememberedStrength == null) {
                Toast.makeText(this, getString(R.string.toast_no_optimal_strength), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.strengthSeekBar.progress = rememberedStrength
            binding.strengthLabel.text = getString(R.string.noise_strength_value, rememberedStrength)
            val source = originalBitmap
            if (source == null) {
                Toast.makeText(this, getString(R.string.toast_pick_image_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val isAutoRecoveryOn = binding.autoRecoverySwitch.isChecked
            val msg = if (isAutoRecoveryOn) {
                getString(R.string.result_reset_optimal_auto, rememberedStrength)
            } else {
                getString(R.string.result_reset_optimal, rememberedStrength)
            }

            runProtectionFlow(
                source = source,
                strength = rememberedStrength,
                autoRecovery = isAutoRecoveryOn,
                clearResultsBeforeRun = true,
                startMessage = msg
            )
        }

        binding.saveButton.setOnClickListener {
            if (protectedBitmap == null) {
                Toast.makeText(this, getString(R.string.toast_apply_protection_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            runWithStoragePermissionIfNeeded {
                saveProtectedImageToGallery()
            }
        }

        handleForwardedShareIntent(intent)
        handleBackgroundStatusIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleForwardedShareIntent(intent)
        handleBackgroundStatusIntent(intent)
    }

    private fun handleForwardedShareIntent(intent: Intent?) {
        if (intent == null) return

        val shareMode = intent.getStringExtra(ShareContract.EXTRA_MODE) ?: return
        val sharedUris = intent.getSharedUrisExtra()
        intent.removeExtra(ShareContract.EXTRA_MODE)
        intent.removeExtra(ShareContract.EXTRA_URIS)

        if (sharedUris.isEmpty()) {
            binding.resultText.text = getString(R.string.result_no_image_selected)
            return
        }

        when (shareMode) {
            ShareContract.MODE_READY_TO_SAVE -> {
                processSingleImageUri(sharedUris.first(), autoSaveAfterProtection = false)
            }
            ShareContract.MODE_AUTO_SAVE -> {
                runWithStoragePermissionIfNeeded {
                    processSingleImageUri(sharedUris.first(), autoSaveAfterProtection = true)
                }
            }
            ShareContract.MODE_AUTO_SAVE_BATCH -> {
                runWithStoragePermissionIfNeeded {
                    processImageBatch(sharedUris)
                }
            }
        }
    }

    private fun handleBackgroundStatusIntent(intent: Intent?) {
        if (intent == null) return

        val statusMessage = intent.getStringExtra(ShareContract.EXTRA_STATUS_MESSAGE)
        val progressMessage = intent.getStringExtra(ShareContract.EXTRA_PROGRESS_MESSAGE)
        if (statusMessage == null && progressMessage == null) return

        binding.resultText.text = progressMessage ?: statusMessage
        if (progressMessage != null) {
            showSearchProgress(progressMessage)
        }
        intent.removeExtra(ShareContract.EXTRA_STATUS_MESSAGE)
        intent.removeExtra(ShareContract.EXTRA_PROGRESS_MESSAGE)
    }

    private fun processSingleImageUri(uri: Uri, autoSaveAfterProtection: Boolean) {
        val loaded = ImageStore.loadBitmapFromUri(this, uri)
        if (loaded == null) {
            binding.resultText.text = getString(R.string.result_load_failed)
            return
        }

        prepareLoadedImage(loaded)
        startOptimalStrengthFlow(
            source = loaded,
            autoSaveAfterProtection = autoSaveAfterProtection,
        )
    }

    private fun prepareLoadedImage(loaded: Bitmap) {
        originalBitmap = loaded
        protectedBitmap = null
        optimalStrength = null
        lastAppliedStrength = null
        lastEvaluationStrength = null
        binding.originalImage.setImageBitmap(loaded)
        binding.noisyImage.setImageDrawable(null)
        binding.recoveredImage.setImageDrawable(null)
        binding.perturbationSummaryText.text = getString(R.string.perturbation_summary_placeholder)
        binding.recommendedStrengthText.text = getString(R.string.recommended_strength_default)
        showSearchProgress(getString(R.string.result_scanning_optimal))
        binding.resetOptimalButton.isEnabled = false
        clearResultCards()
    }

    private fun startOptimalStrengthFlow(
        source: Bitmap,
        autoSaveAfterProtection: Boolean,
    ) {
        binding.resultText.text = getString(R.string.result_image_loaded_scanning)
        setBusy(true, getString(R.string.result_scanning_optimal))
        thread {
            val minStrength = StrengthAdvisor.findRecommendedStrength(
                original = source,
                perturbationModule = perturbationModule,
                defenseEvaluator = defenseEvaluator,
                onStep = { step ->
                    runOnUiThread {
                        binding.strengthSeekBar.progress = step.mid
                        binding.strengthLabel.text = getString(R.string.noise_strength_value, step.mid)
                        showSearchProgress(
                            getString(
                                R.string.search_progress_step,
                                step.iteration,
                                step.low,
                                step.mid,
                                step.high,
                                statusLabelForSearchStep(step),
                            )
                        )
                    }
                },
            )
            runOnUiThread {
                optimalStrength = minStrength
                binding.recommendedStrengthText.text = StrengthAdvisor.recommendationText(this, minStrength)
                if (minStrength == null) {
                    showSearchProgress(getString(R.string.result_scan_none))
                    binding.resetOptimalButton.isEnabled = false
                    binding.resultText.text = getString(R.string.result_scan_none)
                    setBusy(false)
                } else {
                    showOptimalActionButton()
                    binding.resetOptimalButton.isEnabled = true
                    binding.strengthSeekBar.progress = minStrength
                    binding.strengthLabel.text = getString(R.string.noise_strength_value, minStrength)
                    val isAutoRecoveryOn = binding.autoRecoverySwitch.isChecked
                    val msg = if (isAutoRecoveryOn) {
                        getString(R.string.result_auto_applying_optimal, minStrength)
                    } else {
                        getString(R.string.result_scan_found, minStrength)
                    }
                    runProtectionFlow(
                        source = source,
                        strength = minStrength,
                        autoRecovery = isAutoRecoveryOn,
                        clearResultsBeforeRun = true,
                        startMessage = msg,
                        onComplete = { protected ->
                            if (autoSaveAfterProtection) {
                                saveImageToGalleryAsync(protected)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun processImageBatch(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val autoRecovery = binding.autoRecoverySwitch.isChecked
        originalBitmap = null
        protectedBitmap = null
        optimalStrength = null
        lastAppliedStrength = null
        lastEvaluationStrength = null
        clearResultCards()
        binding.noisyImage.setImageDrawable(null)
        binding.recoveredImage.setImageDrawable(null)
        binding.perturbationSummaryText.text = getString(R.string.perturbation_summary_placeholder)
        binding.recommendedStrengthText.text = getString(R.string.recommended_strength_default)
        showSearchProgress(getString(R.string.result_batch_start, uris.size))
        setBusy(true, getString(R.string.result_batch_start, uris.size))

        thread {
            var savedCount = 0
            var skippedCount = 0

            uris.forEachIndexed { index, uri ->
                val itemNumber = index + 1
                runOnUiThread {
                    binding.resultText.text = getString(R.string.result_batch_item_processing, itemNumber, uris.size)
                    showSearchProgress(getString(R.string.result_batch_item_processing, itemNumber, uris.size))
                }

                val loaded = ImageStore.loadBitmapFromUri(this, uri)
                if (loaded == null) {
                    skippedCount += 1
                    runOnUiThread {
                        binding.resultText.text = getString(R.string.result_batch_item_load_failed, itemNumber, uris.size)
                    }
                    return@forEachIndexed
                }

                runOnUiThread {
                    prepareLoadedImage(loaded)
                    setBusy(true, getString(R.string.result_batch_item_processing, itemNumber, uris.size))
                }

                val minStrength = StrengthAdvisor.findRecommendedStrength(
                    original = loaded,
                    perturbationModule = perturbationModule,
                    defenseEvaluator = defenseEvaluator,
                    onStep = { step ->
                        runOnUiThread {
                            binding.strengthSeekBar.progress = step.mid
                            binding.strengthLabel.text = getString(R.string.noise_strength_value, step.mid)
                            showSearchProgress(
                                getString(
                                    R.string.search_progress_step,
                                    step.iteration,
                                    step.low,
                                    step.mid,
                                    step.high,
                                    statusLabelForSearchStep(step),
                                )
                            )
                        }
                    },
                )

                if (minStrength == null) {
                    skippedCount += 1
                    runOnUiThread {
                        optimalStrength = null
                        binding.recommendedStrengthText.text = StrengthAdvisor.recommendationText(this, null)
                        showSearchProgress(getString(R.string.result_scan_none))
                        binding.resultText.text = getString(R.string.result_batch_item_scan_failed, itemNumber, uris.size)
                    }
                    return@forEachIndexed
                }

                val protected = perturbationModule.applyProtection(loaded, minStrength)
                val defenseReport = if (autoRecovery) {
                    defenseEvaluator.evaluateAfterAttack(loaded, protected)
                } else {
                    null
                }
                val saveResult = saveImageToGallery(protected)
                if (saveResult.success) {
                    savedCount += 1
                } else {
                    skippedCount += 1
                }

                runOnUiThread {
                    originalBitmap = loaded
                    protectedBitmap = protected
                    optimalStrength = minStrength
                    lastAppliedStrength = minStrength
                    binding.recommendedStrengthText.text = StrengthAdvisor.recommendationText(this, minStrength)
                    binding.strengthSeekBar.progress = minStrength
                    binding.strengthLabel.text = getString(R.string.noise_strength_value, minStrength)
                    showOptimalActionButton()
                    renderProtectedImage(loaded, protected)
                    if (defenseReport == null) {
                        renderRecoveredImage(null)
                    } else {
                        renderRecoveredImage(defenseReport.attackedBitmap)
                        renderDefenseResult(
                            status = defenseReport.status,
                            evaluationMetrics = defenseReport.evaluationMetrics,
                            qualityMetrics = defenseReport.qualityMetrics
                        )
                        lastEvaluationStrength = minStrength
                    }
                    binding.resultText.text = if (saveResult.success) {
                        getString(R.string.result_batch_item_saved, itemNumber, uris.size, saveResult.filename)
                    } else {
                        getString(R.string.result_batch_item_save_failed, itemNumber, uris.size)
                    }
                }
            }

            runOnUiThread {
                showSearchProgress(getString(R.string.result_batch_complete, savedCount, skippedCount))
                binding.resultText.text = getString(R.string.result_batch_complete, savedCount, skippedCount)
                setBusy(false)
            }
        }
    }

    private fun statusLabelForSearchStep(step: NoiseSearcher.SearchStep): String {
        return if (step.passed) {
            getString(R.string.status_held)
        } else {
            getString(R.string.status_broken)
        }
    }

    private fun runWithStoragePermissionIfNeeded(action: () -> Unit) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val permission = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            val hasPermission =
                ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                pendingStoragePermissionAction = action
                writeStoragePermissionLauncher.launch(permission)
                return
            }
        }

        action()
    }

    @Suppress("DEPRECATION")
    private fun Intent.getSharedUrisExtra(): List<Uri> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(ShareContract.EXTRA_URIS, Uri::class.java).orEmpty()
        } else {
            getParcelableArrayListExtra<Uri>(ShareContract.EXTRA_URIS).orEmpty()
        }
    }

    private fun clearResultCards() {
        binding.protectionStatusValue.text = getString(R.string.status_na)
        binding.modelFaceCountValue.text = getString(R.string.metric_face_count_placeholder)
        binding.modelLabelShiftValue.text = getString(R.string.metric_label_shift_placeholder)
        binding.modelScoreValue.text = getString(R.string.metric_score_placeholder)
        binding.qualityPsnrValue.text = getString(R.string.metric_psnr_placeholder)
        binding.qualityDeltaValue.text = getString(R.string.metric_delta_placeholder)
        binding.qualityEdgeDeltaValue.text = getString(R.string.metric_edge_delta_placeholder)
    }

    private fun statusLabel(status: ProtectionStatus): String {
        return when (status) {
            ProtectionStatus.PASS -> getString(R.string.status_pass)
            ProtectionStatus.BROKEN -> getString(R.string.status_broken)
            ProtectionStatus.HELD -> getString(R.string.status_held)
        }
    }

    private fun setBusy(isBusy: Boolean, message: String? = null) {
        binding.busyProgressBar.visibility = if (isBusy) View.VISIBLE else View.GONE
        binding.pickButton.isEnabled = !isBusy
        binding.autoRecoverySwitch.isEnabled = !isBusy
        binding.applyButton.isEnabled = !isBusy
        binding.attackButton.isEnabled = !isBusy
        binding.resetOptimalButton.isEnabled = !isBusy && optimalStrength != null
        binding.saveButton.isEnabled = !isBusy
        binding.strengthSeekBar.isEnabled = !isBusy
        if (message != null) {
            binding.resultText.text = message
        }
    }

    private fun showSearchProgress(message: String) {
        binding.searchProgressText.visibility = View.VISIBLE
        binding.searchProgressText.text = message
        binding.resetOptimalButton.visibility = View.GONE
    }

    private fun showOptimalActionButton() {
        binding.searchProgressText.visibility = View.GONE
        binding.resetOptimalButton.visibility = View.VISIBLE
    }

    private fun setTechnicalDetailsVisible(visible: Boolean) {
        binding.technicalDetailsContainer.visibility = if (visible) View.VISIBLE else View.GONE
        binding.technicalDetailsToggle.text = if (visible) {
            getString(R.string.technical_details_shown)
        } else {
            getString(R.string.technical_details_hidden)
        }
    }

    private fun renderDefenseResult(
        status: ProtectionStatus,
        evaluationMetrics: EvaluationMetrics,
        qualityMetrics: QualityMetrics,
    ) {
        val psnrPercent = psnrToPercent(qualityMetrics.psnr)

        binding.protectionStatusValue.text = statusLabel(status)

        binding.modelFaceCountValue.text =
            getString(R.string.metric_face_count_format, evaluationMetrics.faceCountOriginal, evaluationMetrics.faceCountAfterAttack)
        binding.modelLabelShiftValue.text =
            getString(R.string.metric_label_shift_format, evaluationMetrics.labelShift)
        binding.modelScoreValue.text =
            getString(R.string.metric_score_format, evaluationMetrics.antiDetectionScore)

        binding.qualityPsnrValue.text =
            getString(R.string.metric_psnr_format, qualityMetrics.psnr, psnrPercent)
        binding.qualityDeltaValue.text =
            getString(R.string.metric_delta_format, qualityMetrics.meanAbsDelta)
        binding.qualityEdgeDeltaValue.text =
            getString(R.string.metric_edge_delta_format, qualityMetrics.edgeDelta)
    }

    private fun psnrToPercent(psnr: Double): Double {
        val minPsnr = 8.0
        val maxPsnr = 50.0
        val normalized = (psnr - minPsnr) / (maxPsnr - minPsnr)
        return (normalized * 100.0).coerceIn(0.0, 100.0)
    }

    private fun runProtectionFlow(
        source: Bitmap,
        strength: Int,
        autoRecovery: Boolean,
        clearResultsBeforeRun: Boolean,
        startMessage: String,
        onComplete: ((Bitmap) -> Unit)? = null,
    ) {
        if (clearResultsBeforeRun) {
            clearResultCards()
        }

        binding.resultText.text = startMessage
        setBusy(true, startMessage)

        thread {
            val output = perturbationModule.applyProtection(source, strength)
            val defenseReport = if (autoRecovery) defenseEvaluator.evaluateAfterAttack(source, output) else null

            runOnUiThread {
                protectedBitmap = output
                lastAppliedStrength = strength
                renderProtectedImage(source, output)

                if (defenseReport == null) {
                    renderRecoveredImage(null)
                    binding.resultText.text = getString(R.string.result_protection_applied, strength)
                } else {
                    renderRecoveredImage(defenseReport.attackedBitmap)
                    renderDefenseResult(
                        status = defenseReport.status,
                        evaluationMetrics = defenseReport.evaluationMetrics,
                        qualityMetrics = defenseReport.qualityMetrics
                    )
                    lastEvaluationStrength = strength
                    val localizedStatus = statusLabel(defenseReport.status)
                    binding.resultText.text = getString(R.string.result_auto_recovery_complete, strength, localizedStatus)
                }

                setBusy(false)
                onComplete?.invoke(output)
            }
        }
    }

    private fun showManualDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.help_dialog_title))
            .setMessage(getString(R.string.help_dialog_message))
            .setPositiveButton(getString(R.string.help_dialog_positive)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun renderProtectedImage(reference: Bitmap, protected: Bitmap) {
        binding.noisyImage.setImageBitmap(protected)
        val meanAbsDelta = computeMeanAbsoluteDifference(reference, protected)
        val percent = (meanAbsDelta / 255.0) * 100.0
        binding.perturbationSummaryText.text = getString(R.string.perturbation_magnitude_format, meanAbsDelta, percent)
    }

    private fun renderRecoveredImage(recovered: Bitmap?) {
        binding.recoveredImage.setImageBitmap(recovered)
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

            val dr = kotlin.math.abs(((ref shr 16) and 0xFF) - ((dst shr 16) and 0xFF))
            val dg = kotlin.math.abs(((ref shr 8) and 0xFF) - ((dst shr 8) and 0xFF))
            val db = kotlin.math.abs((ref and 0xFF) - (dst and 0xFF))

            val diff = (dr + dg + db) / 3.0
            totalDiff += diff
        }

        return totalDiff / testedPixels.size
    }

    private fun saveProtectedImageToGallery() {
        val image = protectedBitmap ?: return

        saveImageToGalleryAsync(image)
    }

    private fun saveImageToGalleryAsync(image: Bitmap) {
        setBusy(true, getString(R.string.result_saving_protected_image))
        thread {
            val result = saveImageToGallery(image)
            runOnUiThread {
                renderSaveResult(result)
                setBusy(false)
            }
        }
    }

    private fun saveImageToGallery(image: Bitmap): GallerySaveResult {
        return ImageStore.saveImageToGallery(this, image)
    }

    private fun renderSaveResult(result: GallerySaveResult) {
        if (result.success) {
            Toast.makeText(this, getString(R.string.toast_saved_to_gallery), Toast.LENGTH_SHORT).show()
            binding.resultText.text = getString(R.string.result_saved_image, result.filename)
            return
        }

        binding.resultText.text = when (result.failure) {
            GallerySaveFailure.CREATE_ENTRY -> getString(R.string.result_save_failed_entry)
            GallerySaveFailure.WRITE_DATA -> getString(R.string.result_save_failed_write)
            GallerySaveFailure.ERROR -> getString(
                R.string.result_save_error,
                result.errorMessage ?: getString(R.string.error_unknown)
            )
            null -> getString(R.string.result_save_error, getString(R.string.error_unknown))
        }
    }

}


