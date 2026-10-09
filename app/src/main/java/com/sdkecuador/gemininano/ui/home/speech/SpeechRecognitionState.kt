package com.sdkecuador.gemininano.ui.home.speech

data class SpeechRecognitionState(
    val transcript: String = "",
    val isPreparing: Boolean = false,
    val isListening: Boolean = false,
    val errorMessage: String? = null
)
