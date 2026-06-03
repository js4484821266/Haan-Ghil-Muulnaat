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
 * 로컬 ML Kit 기반 모델 평가기입니다.
 *
 * 이것은 보편적인 보안 판정기가 아닙니다. 얼굴 수 억제와 이미지 라벨 변화를 합쳐
 * 현재 이미지에 perturbation이 충분했는지 앱 안에서 판단하기 위한 실용적인 신호입니다.
 */
object ModelProbe {
    internal const val FACE_SUPPRESSION_WEIGHT = 0.35
    internal const val LABEL_SHIFT_WEIGHT = 0.65
    internal const val PASS_THRESHOLD = 0.35

    fun evaluate(original: Bitmap, protected: Bitmap): ModelProbeResult {
        // 강도 탐색 중 반복 실행되므로 의도적으로 빠른 모드를 씁니다.
        // 모든 얼굴을 끝까지 찾는 정확도보다 지연 시간이 더 중요합니다.
        val faceOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        val faceDetector = FaceDetection.getClient(faceOptions)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        return try {
            // 여기서는 ML Kit 작업을 동기적으로 기다립니다. 따라서 호출자는 이 메서드를
            // 작업자 스레드에서 실행해야 하며, MainActivity와 서비스가 그렇게 합니다.
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
            
            // 원본에서 얼굴이 감지됐다면 보호본에서는 얼굴 수가 줄어야 합니다.
            val faceReductionAchieved = if (originalFaces.isNotEmpty()) {
                protectedFaces.size < originalFaces.size
            } else {
                true
            }
            
            val pass = antiDetectionScore >= PASS_THRESHOLD && faceReductionAchieved

            // 후보 강도가 실패한 이유를 설명하고 임계값을 조정할 때 필요하므로
            // 판정 사유 문자열은 명시적으로 유지합니다.
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
            // FaceDetector와 ImageLabeler는 네이티브 리소스를 들고 있으므로,
            // 작업 실패나 예외 중단이 있어도 반드시 닫습니다.
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
        // 겹치는 라벨이 적을수록 perturbation 이후 의미 변화가 더 컸다는 뜻입니다.
        return 1.0 - (intersection.toDouble() / originalSet.size)
    }

    internal fun computeAntiDetectionScore(faceSuppression: Double, labelShift: Double): Double {
        return FACE_SUPPRESSION_WEIGHT * faceSuppression + LABEL_SHIFT_WEIGHT * labelShift
    }
}
