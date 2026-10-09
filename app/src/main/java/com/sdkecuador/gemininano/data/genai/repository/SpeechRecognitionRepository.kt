package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import dagger.hilt.android.scopes.ViewModelScoped
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Encapsula la sesión de dictado de ML Kit. El modo Basic funciona en dispositivos
 * compatibles con Android 12+ y evita enviar el audio a un servidor.
 */
@ViewModelScoped
class SpeechRecognitionRepository @Inject constructor(
    clientFactory: GenAiClientFactory
) : AutoCloseable {
    private val recognizer: SpeechRecognizer = clientFactory.createSpeechRecognizer(
        speechRecognizerOptions {
            locale = Locale.forLanguageTag("es-ES")
            preferredMode = SpeechRecognizerOptions.Mode.MODE_BASIC
        }
    )

    fun transcribeFromMicrophone(): Flow<SpeechRecognizerResponse> = flow {
        ensureModelIsReady()
        val request = speechRecognizerRequest {
            audioSource = AudioSource.fromMic()
        }
        emitAll(recognizer.startRecognition(request))
    }

    suspend fun stop() = recognizer.stopRecognition()

    private suspend fun ensureModelIsReady() {
        when (recognizer.checkStatus()) {
            FeatureStatus.AVAILABLE -> Unit
            FeatureStatus.DOWNLOADABLE -> recognizer.download().collect { }
            else -> error("El reconocimiento de voz no está disponible en este dispositivo.")
        }
    }

    override fun close() = recognizer.close()
}
