package com.haanghil.muulnaat

import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceLandmark

private val FEATURE_LANDMARK_TYPES = listOf(
    FaceLandmark.LEFT_EYE,
    FaceLandmark.RIGHT_EYE,
    FaceLandmark.NOSE_BASE,
    FaceLandmark.MOUTH_LEFT,
    FaceLandmark.MOUTH_RIGHT,
    FaceLandmark.MOUTH_BOTTOM,
)

private val FEATURE_CONTOUR_TYPES = listOf(
    FaceContour.LEFT_EYE,
    FaceContour.RIGHT_EYE,
    FaceContour.LEFT_EYEBROW_TOP,
    FaceContour.LEFT_EYEBROW_BOTTOM,
    FaceContour.RIGHT_EYEBROW_TOP,
    FaceContour.RIGHT_EYEBROW_BOTTOM,
    FaceContour.NOSE_BRIDGE,
    FaceContour.NOSE_BOTTOM,
    FaceContour.UPPER_LIP_TOP,
    FaceContour.UPPER_LIP_BOTTOM,
    FaceContour.LOWER_LIP_TOP,
    FaceContour.LOWER_LIP_BOTTOM,
)

/**
 * 눈, 눈썹, 코, 입 관련 landmark/contour 그룹이 감지됐는지 센다.
 * 값 하나는 점 하나가 아니라 얼굴 특징 그룹 하나에 가깝다.
 */
internal fun countFacialFeatures(faces: List<Face>): Int {
    return faces.sumOf { face ->
        val landmarkCount = FEATURE_LANDMARK_TYPES.count { type ->
            face.getLandmark(type) != null
        }
        val contourCount = FEATURE_CONTOUR_TYPES.count { type ->
            face.getContour(type)?.points?.isNotEmpty() == true
        }
        landmarkCount + contourCount
    }
}

internal fun hasFeatureBaseline(originalFaceCount: Int, originalFeatureCount: Int): Boolean {
    return originalFaceCount > 0 && originalFeatureCount > 0
}
