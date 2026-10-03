package com.sdkecuador.gemininano.ui.home.rewrite

import com.sdkecuador.gemininano.data.genai.model.RewriteStyle

enum class RewriteType(val displayName: String, val style: RewriteStyle) {
    REPHRASE("Parafrasear", RewriteStyle.REPHRASE),
    SHORTEN("Acortar", RewriteStyle.SHORTEN),
    ELABORATE("Ampliar", RewriteStyle.ELABORATE),
    FRIENDLY("Amigable", RewriteStyle.FRIENDLY),
    PROFESSIONAL("Profesional", RewriteStyle.PROFESSIONAL)
}

data class RewriteState(
    val input: String = "El equipo necesita resolver este problema urgentemente antes del gran lanzamiento del producto.",
    val suggestion: String = "",
    val isLoading: Boolean = false,
    val mbToDownload: Double = 0.0,
    val mbDownloaded: Double = 0.0,
    val errorMessage: String? = null,
    val selectedRewriteType: RewriteType = RewriteType.REPHRASE
) {
    val downloadProgress: String
        get() = "Descargando $mbDownloaded de $mbToDownload MB..."
}
