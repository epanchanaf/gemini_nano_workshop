package com.sdkecuador.gemininano.ui.home.imagedesc

data class ImageDescState(
    val description: String = "",
    val isLoading: Boolean = false,
    val mbToDownload: Double = 0.0,
    val mbDownloaded: Double = 0.0,
    val errorMessage: String? = null
) {
    val downloadProgress: String
        get() = "Descargando $mbDownloaded de $mbToDownload MB..."
}
