package com.haanghil.muulnaat

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

internal fun evaluateModelProbe(original: Bitmap, protected: Bitmap): ModelProbeResult {
    val faceOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .build()
    val faceDetector = FaceDetection.getClient(faceOptions)
    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    return try {
        val originalFaceTask = faceDetector.process(InputImage.fromBitmap(original, 0))
        val protectedFaceTask = faceDetector.process(InputImage.fromBitmap(protected, 0))
        val originalLabelTask = labeler.process(InputImage.fromBitmap(original, 0))
        val protectedLabelTask = labeler.process(InputImage.fromBitmap(protected, 0))

        val originalFaces = Tasks.await(originalFaceTask)
        val protectedFaces = Tasks.await(protectedFaceTask)
        val originalLabels = Tasks.await(originalLabelTask).map { it.text }
        val protectedLabels = Tasks.await(protectedLabelTask).map { it.text }
        val originalFeatureCount = countFacialFeatures(originalFaces)
        val protectedFeatureCount = countFacialFeatures(protectedFaces)
        val faceSuppression = ModelProbe.computeFaceSuppressionScore(
            originalFaces.size,
            protectedFaces.size,
        )
        val featureSuppression = ModelProbe.computeFacialFeatureSuppressionScore(
            originalFeatureCount,
            protectedFeatureCount,
        )
        val labelShift = ModelProbe.computeLabelShift(originalLabels, protectedLabels)
        val antiDetectionScore = ModelProbe.computeAntiDetectionScore(
            faceSuppression,
            featureSuppression,
            labelShift,
        )

        buildModelProbeResult(
            originalFaceCount = originalFaces.size,
            protectedFaceCount = protectedFaces.size,
            originalFeatureCount = originalFeatureCount,
            protectedFeatureCount = protectedFeatureCount,
            faceSuppression = faceSuppression,
            featureSuppression = featureSuppression,
            labelShift = labelShift,
            antiDetectionScore = antiDetectionScore,
        )
    } finally {
        faceDetector.close()
        labeler.close()
    }
}
