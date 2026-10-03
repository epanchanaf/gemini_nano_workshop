package com.sdkecuador.gemininano.ui.home.proofread

data class ProofReadingState(
    val input: String = "El eqquipo nesesita areglar urjentemente este problema antes del gran lansamiento.",
    val suggestion: String = "",
    val isLoading: Boolean = false,
    val mbToDownload: Double = 0.0,
    val mbDownloaded: Double = 0.0,
    val errorMessage: String? = null
) {
    val downloadProgress: String
        get() = "Descargando $mbDownloaded de $mbToDownload MB..."
}
