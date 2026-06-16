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

    // ── computeFacialFeatureSuppressionScore ────────────────────────────────

    @Test
    fun `featureSuppression is 0 when originalFeatureCount is 0`() {
        assertEquals(0.0, ModelProbe.computeFacialFeatureSuppressionScore(0, 0), 0.0001)
    }

    @Test
    fun `featureSuppression is 1 when every facial feature is removed`() {
        assertEquals(1.0, ModelProbe.computeFacialFeatureSuppressionScore(8, 0), 0.0001)
    }

    @Test
    fun `featureSuppression is 0_5 when half facial features remain`() {
        assertEquals(0.5, ModelProbe.computeFacialFeatureSuppressionScore(8, 4), 0.0001)
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
        val score = ModelProbe.computeAntiDetectionScore(1.0, 1.0, 0.0)
        assertEquals(
            ModelProbe.FACE_SUPPRESSION_WEIGHT + ModelProbe.FACIAL_FEATURE_SUPPRESSION_WEIGHT,
            score,
            0.0001,
        )
    }

    @Test
    fun `antiDetectionScore is 0 when both inputs are 0`() {
        assertEquals(0.0, ModelProbe.computeAntiDetectionScore(0.0, 0.0, 0.0), 0.0001)
    }

    // ── Scoring scenarios (Conceptual tests for logic in evaluate) ────────────

    @Test
    fun `score would pass but facial feature remains should fail logic`() {
        val pass = ModelProbe.shouldPass(
            antiDetectionScore = 0.8,
            originalFaceCount = 1,
            originalFeatureCount = 8,
            protectedFeatureCount = 1,
        )

        assertFalse("Should fail if any facial feature remains", pass)
    }

    @Test
    fun `score would pass and facial features are removed should pass logic`() {
        val pass = ModelProbe.shouldPass(
            antiDetectionScore = 0.5,
            originalFaceCount = 1,
            originalFeatureCount = 8,
            protectedFeatureCount = 0,
        )

        assertTrue("Should pass if feature baseline exists and no attacked feature remains", pass)
    }

    @Test
    fun `missing original feature baseline should not pass`() {
        val pass = ModelProbe.shouldPass(
            antiDetectionScore = 1.0,
            originalFaceCount = 1,
            originalFeatureCount = 0,
            protectedFeatureCount = 0,
        )

        assertFalse("Should fail when original feature baseline is missing", pass)
    }

    @Test
    fun `missing original face baseline should not pass`() {
        val pass = ModelProbe.shouldPass(
            antiDetectionScore = 1.0,
            originalFaceCount = 0,
            originalFeatureCount = 8,
            protectedFeatureCount = 0,
        )

        assertFalse("Should fail when original face baseline is missing", pass)
    }

    @Test
    fun `removed features still fail when score is below threshold`() {
        val pass = ModelProbe.shouldPass(
            antiDetectionScore = 0.49,
            originalFaceCount = 1,
            originalFeatureCount = 8,
            protectedFeatureCount = 0,
        )

        assertFalse("Should fail when score is below threshold", pass)
    }
}
