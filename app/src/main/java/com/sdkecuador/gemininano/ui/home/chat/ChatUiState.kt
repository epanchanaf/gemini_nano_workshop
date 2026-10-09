package com.sdkecuador.gemininano.ui.home.chat

import android.graphics.Bitmap

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            message = "¡Hola! Soy Gemini Nano. Pregúntame lo que quieras.",
            isFromUser = false,
            includeInContext = false
        )
    ),
    val input: String = "",
    val pendingImage: Bitmap? = null,
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
    val isFromUser: Boolean,
    val includeInContext: Boolean = true,
    val image: Bitmap? = null
)
