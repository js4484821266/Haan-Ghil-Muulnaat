package com.haanghil.muulnaat

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.Locale

data class ModelProbeResult(
    val faceCountOriginal: Int,
    val faceCountProtected: Int,
    val faceSuppressionScore: Double,
    val labelShift: Double,
    val antiDetectionScore: Double,
    val passed: Boolean,
    val decisionReason: String,
    val details: String
)

/**
 * Local ML Kit-based model probe.
 *
 * This is not a universal security oracle. It is a practical, on-device signal
 * that combines face-count suppression with image-label drift so the app can
 * decide whether a perturbation was strong enough for the current image.
 */
object ModelProbe {
    internal const val FACE_SUPPRESSION_WEIGHT = 0.35
    internal const val LABEL_SHIFT_WEIGHT = 0.65
    internal const val PASS_THRESHOLD = 0.35

    fun evaluate(original: Bitmap, protected: Bitmap): ModelProbeResult {
        // Fast mode is intentional: this probe runs repeatedly during strength
        // search, so latency matters more than exhaustive detector accuracy.
        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        val faceDetector = FaceDetection.getClient(faceOptions)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        return try {
            // ML Kit tasks are awaited synchronously here. Callers must keep this
            // method on a worker thread, which MainActivity and the service do.
            val originalFaceTask = faceDetector.process(InputImage.fromBitmap(original, 0))
            val protectedFaceTask = faceDetector.process(InputImage.fromBitmap(protected, 0))
            val originalLabelTask = labeler.process(InputImage.fromBitmap(original, 0))
            val protectedLabelTask = labeler.process(InputImage.fromBitmap(protected, 0))

            val originalFaces = Tasks.await(originalFaceTask)
            val protectedFaces = Tasks.await(protectedFaceTask)
            val originalLabels = Tasks.await(originalLabelTask).map { it.text }
            val protectedLabels = Tasks.await(protectedLabelTask).map { it.text }

            val faceSuppression = computeFaceSuppressionScore(originalFaces.size, protectedFaces.size)
            val labelShift = computeLabelShift(originalLabels, protectedLabels)
            val antiDetectionScore = computeAntiDetectionScore(faceSuppression, labelShift)
            
            // Require face count reduction if faces were detected in the original image.
            val faceReductionAchieved = if (originalFaces.isNotEmpty()) {
                protectedFaces.size < originalFaces.size
            } else {
                true
            }
            
            val pass = antiDetectionScore >= PASS_THRESHOLD && faceReductionAchieved

            // Keep the reason string explicit because it is useful while tuning
            // thresholds and explaining why a candidate strength failed.
            val reason = when {
                pass && originalFaces.isNotEmpty() && protectedFaces.isEmpty() ->
                    "PASS: face suppressed and classifier disrupted (score=${String.format(Locale.US, "%.2f", antiDetectionScore)})"
                pass ->
                    "PASS: classifier disruption sufficient (score=${String.format(Locale.US, "%.2f", antiDetectionScore)})"
                !faceReductionAchieved && originalFaces.isNotEmpty() ->
                    "FAIL: face count not reduced (original: ${originalFaces.size}, protected: ${protectedFaces.size})"
                else ->
                    "FAIL: insufficient classifier disruption (score=${String.format(Locale.US, "%.2f", antiDetectionScore)}, need>=${String.format(Locale.US, "%.2f", PASS_THRESHOLD)})"
            }

            val details = buildString {
                append("Original faces: ${originalFaces.size}; ")
                append("Protected faces: ${protectedFaces.size}; ")
                append("FaceSuppression: ${String.format(Locale.US, "%.2f", faceSuppression)}; ")
                append("LabelShift: ${String.format(Locale.US, "%.2f", labelShift)}; ")
                append("AntiDetectionScore: ${String.format(Locale.US, "%.2f", antiDetectionScore)}\n")
                append("Decision: $reason")
            }

            ModelProbeResult(
                faceCountOriginal = originalFaces.size,
                faceCountProtected = protectedFaces.size,
                faceSuppressionScore = faceSuppression,
                labelShift = labelShift,
                antiDetectionScore = antiDetectionScore,
                passed = pass,
                decisionReason = reason,
                details = details
            )
        } finally {
            // FaceDetector and ImageLabeler hold native resources; close them
            // even when a task fails or is interrupted by an exception.
            faceDetector.close()
            labeler.close()
        }
    }

    internal fun computeFaceSuppressionScore(originalFaceCount: Int, protectedFaceCount: Int): Double {
        if (originalFaceCount <= 0) return 0.0
        return ((originalFaceCount - protectedFaceCount).toDouble() / originalFaceCount).coerceIn(0.0, 1.0)
    }

    internal fun computeLabelShift(originalLabels: List<String>, protectedLabels: List<String>): Double {
        if (originalLabels.isEmpty()) return 0.0
        val originalSet = originalLabels.toSet()
        val intersection = originalSet.intersect(protectedLabels.toSet()).size
        // A smaller overlap means stronger semantic drift after perturbation.
        return 1.0 - (intersection.toDouble() / originalSet.size)
    }

    internal fun computeAntiDetectionScore(faceSuppression: Double, labelShift: Double): Double {
        return FACE_SUPPRESSION_WEIGHT * faceSuppression + LABEL_SHIFT_WEIGHT * labelShift
    }
}
