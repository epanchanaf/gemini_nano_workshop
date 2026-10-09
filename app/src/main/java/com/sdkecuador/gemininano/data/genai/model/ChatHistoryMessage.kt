package com.sdkecuador.gemininano.data.genai.model

import android.graphics.Bitmap

/** Un mensaje previo que se adjunta al siguiente prompt del chat. */
data class ChatHistoryMessage(
    val text: String,
    val isFromUser: Boolean,
    val image: Bitmap? = null
)
