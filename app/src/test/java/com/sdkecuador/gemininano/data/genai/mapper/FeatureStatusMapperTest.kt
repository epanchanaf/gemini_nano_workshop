package com.sdkecuador.gemininano.data.genai.mapper

import com.google.mlkit.genai.common.FeatureStatus
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureStatusMapperTest {
    @Test
    fun toRepositoryStatus_mapsKnownFeatureStates() {
        assertEquals(AiCoreFeatureStatus.AVAILABLE, FeatureStatus.AVAILABLE.toRepositoryStatus())
        assertEquals(AiCoreFeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADABLE.toRepositoryStatus())
        assertEquals(AiCoreFeatureStatus.DOWNLOADING, FeatureStatus.DOWNLOADING.toRepositoryStatus())
    }

    @Test
    fun toRepositoryStatus_mapsUnknownStatesToUnavailable() {
        assertEquals(AiCoreFeatureStatus.UNAVAILABLE, Int.MIN_VALUE.toRepositoryStatus())
    }
}
