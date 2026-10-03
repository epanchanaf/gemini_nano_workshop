package com.sdkecuador.gemininano.data.genai.mapper

import com.google.mlkit.genai.common.FeatureStatus
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus

internal fun Int.toRepositoryStatus(): AiCoreFeatureStatus = when (this) {
    FeatureStatus.AVAILABLE -> AiCoreFeatureStatus.AVAILABLE
    FeatureStatus.DOWNLOADABLE -> AiCoreFeatureStatus.DOWNLOADABLE
    FeatureStatus.DOWNLOADING -> AiCoreFeatureStatus.DOWNLOADING
    else -> AiCoreFeatureStatus.UNAVAILABLE
}
