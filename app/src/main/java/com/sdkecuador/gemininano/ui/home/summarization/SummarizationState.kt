package com.sdkecuador.gemininano.ui.home.summarization

data class SummarizationState(
    val input: String = "",
    val summary: String = "",
    val isLoading: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val mbToDownload: Double = 0.0,
    val mbDownloaded: Double = 0.0,
    val errorMessage: String? = null
) {
    val downloadProgress: String
        get() = if (mbToDownload > 0) {
            "Descargando el recurso de resumen: $mbDownloaded de $mbToDownload MB..."
        } else {
            "Preparando la descarga del recurso de resumen..."
        }

    val actionButtonLabel: String
        get() = when {
            isDownloadingModel -> "Descargando..."
            isLoading -> "Resumiendo..."
            else -> "Resumir"
        }
}
