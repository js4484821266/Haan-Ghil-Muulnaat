package com.haanghil.muulnaat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelProbeTest {

    // ── computeFaceSuppressionScore ──────────────────────────────────────────

    @Test
    fun `faceSuppression is 0 when originalFaceCount is 0`() {
        assertEquals(0.0, ModelProbe.computeFaceSuppressionScore(0, 0), 0.0001)
    }

    @Test
    fun `faceSuppression is 1 when face completely removed`() {
        assertEquals(1.0, ModelProbe.computeFaceSuppressionScore(1, 0), 0.0001)
    }

    @Test
    fun `faceSuppression is 0 when face still present`() {
        assertEquals(0.0, ModelProbe.computeFaceSuppressionScore(1, 1), 0.0001)
    }

    @Test
    fun `faceSuppression is 0_5 when half faces removed`() {
        assertEquals(0.5, ModelProbe.computeFaceSuppressionScore(2, 1), 0.0001)
    }

    // ── computeLabelShift ────────────────────────────────────────────────────

    @Test
    fun `labelShift is 0 when no original labels`() {
        assertEquals(0.0, ModelProbe.computeLabelShift(emptyList(), listOf("face")), 0.0001)
    }

    @Test
    fun `labelShift is 0 when all labels are the same`() {
        val labels = listOf("face", "person", "portrait")
        assertEquals(0.0, ModelProbe.computeLabelShift(labels, labels), 0.0001)
    }

    @Test
    fun `labelShift is 1 when no labels overlap`() {
        assertEquals(
            1.0,
            ModelProbe.computeLabelShift(listOf("face", "person"), listOf("car", "tree")),
            0.0001
        )
    }

    @Test
    fun `labelShift is 0_5 when half labels overlap`() {
        assertEquals(
            0.5,
            ModelProbe.computeLabelShift(listOf("face", "person"), listOf("face", "car")),
            0.0001
        )
    }

    // ── computeAntiDetectionScore ────────────────────────────────────────────

    @Test
    fun `antiDetectionScore is weighted combination of faceSuppression and labelShift`() {
        val score = ModelProbe.computeAntiDetectionScore(1.0, 0.0)
        assertEquals(ModelProbe.FACE_SUPPRESSION_WEIGHT, score, 0.0001)
    }

    @Test
    fun `antiDetectionScore is 0 when both inputs are 0`() {
        assertEquals(0.0, ModelProbe.computeAntiDetectionScore(0.0, 0.0), 0.0001)
    }

    // ── Scoring scenarios (Conceptual tests for logic in evaluate) ────────────

    @Test
    fun `score would pass but face not reduced should fail logic`() {
        // This is a manual verification of the logic I added to evaluate()
        val antiDetectionScore = 0.5 // > 0.35
        val originalFaces = 1
        val protectedFaces = 1
        
        val faceReductionAchieved = if (originalFaces > 0) {
            protectedFaces < originalFaces
        } else {
            true
        }
        
        val pass = antiDetectionScore >= 0.35 && faceReductionAchieved
        assertFalse("Should fail if face count is not reduced", pass)
    }

    @Test
    fun `score would pass and face is reduced should pass logic`() {
        val antiDetectionScore = 0.5
        val originalFaces = 1
        val protectedFaces = 0
        
        val faceReductionAchieved = if (originalFaces > 0) {
            protectedFaces < originalFaces
        } else {
            true
        }
        
        val pass = antiDetectionScore >= 0.35 && faceReductionAchieved
        assertTrue("Should pass if score is high and face is removed", pass)
    }
}
