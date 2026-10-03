package com.sdkecuador.gemininano.data.genai.model

data class AiCoreRuntimeStatus(
    val name: String,
    val detail: String,
    val featureStatus: AiCoreFeatureStatus
)

data class AiCoreRuntimeInfo(
    val appVersion: String,
    val aiCoreVersion: String,
    val runtimes: List<AiCoreRuntimeStatus>
)
