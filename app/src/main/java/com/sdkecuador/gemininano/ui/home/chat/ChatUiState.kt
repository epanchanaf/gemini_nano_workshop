package com.sdkecuador.gemininano.ui.home.chat

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage("¡Hola! Soy Gemini Nano. Pregúntame lo que quieras.", false)
    ),
    val input: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val mbToDownload: Double = 0.0,
    val mbDownloaded: Double = 0.0
) {
    val downloadProgress: String
        get() = "Descargando modelo: ${"%.2f".format(mbDownloaded)} de ${"%.2f".format(mbToDownload)} MB..."
}

data class ChatMessage(
    val message: String,
    val isFromUser: Boolean
)
