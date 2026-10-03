package com.sdkecuador.gemininano.data.genai.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiInputLimitsTest {
    @Test
    fun textTokenEstimateRoundsUpEveryFourCharacters() {
        assertEquals(0, "   ".estimatedTokenCount())
        assertEquals(1, "abcd".estimatedTokenCount())
        assertEquals(2, "abcde".estimatedTokenCount())
    }

    @Test
    fun imageLimitsRejectOversizedDimensionsAndPixelCounts() {
        assertTrue(isSupportedImageSize(2_000, 2_000))
        assertFalse(isSupportedImageSize(2_049, 100))
        assertFalse(isSupportedImageSize(2_000, 2_001))
    }

    @Test
    fun imageScaleKeepsImagesWithinTheVisibleLimit() {
        val scale = imageScaleForInputLimit(4_096, 2_048)
        assertEquals(0.5f, scale, 0.001f)
    }
}
