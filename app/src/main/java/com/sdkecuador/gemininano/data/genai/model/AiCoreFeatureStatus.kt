package com.sdkecuador.gemininano.data.genai.model

/** Estado independiente del SDK para una capacidad que se ejecuta mediante AICore. */
enum class AiCoreFeatureStatus {
    AVAILABLE,
    DOWNLOADABLE,
    DOWNLOADING,
    UNAVAILABLE
}
