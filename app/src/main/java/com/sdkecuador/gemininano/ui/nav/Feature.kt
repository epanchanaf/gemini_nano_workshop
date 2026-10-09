package com.sdkecuador.gemininano.ui.nav

import androidx.annotation.DrawableRes
import com.sdkecuador.gemininano.R

sealed class Feature(
    val route: String,
    val title: String,
    val subtitle: String,
    @param:DrawableRes val iconRes: Int
) {
    object Summarization : Feature(
        "summarization",
        "Resumen",
        "Resume artículos o conversaciones",
        R.drawable.ic_summarize
    )

    object Proofreading : Feature(
        "proofreading",
        "Corrección",
        "Corrige errores en mensajes cortos",
        R.drawable.ic_proofread
    )

    object Rewrite : Feature(
        "rewrite",
        "Reescritura",
        "Cambia el tono o estilo de los mensajes",
        R.drawable.ic_rewrite
    )

    object ImageDescription : Feature(
        "image_description",
        "Descripción de imágenes",
        "Genera texto alternativo para una imagen",
        R.drawable.ic_image
    )

    object Chat : Feature(
        "chat",
        "Chat",
        "Genera contenido de texto con instrucciones",
        R.drawable.ic_ai
    )

    object SpeechRecognition : Feature(
        "speech_recognition",
        "Reconocimiento de voz",
        "Transcribe tu voz a texto de forma local",
        R.drawable.ic_mic
    )

    object Journal : Feature(
        "journal",
        "Diario por voz",
        "Guarda vivencias y cambia colores con Gemini Nano",
        R.drawable.ic_mic
    )

    companion object {
        val featureScreens: List<Feature>
            get() = listOf(
                Summarization,
                Proofreading,
                Rewrite,
                ImageDescription,
                Chat,
                SpeechRecognition,
                Journal
            )
    }
}
