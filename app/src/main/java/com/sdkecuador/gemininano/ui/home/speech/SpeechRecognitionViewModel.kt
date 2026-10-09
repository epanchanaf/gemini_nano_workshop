package com.sdkecuador.gemininano.ui.home.speech

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.sdkecuador.gemininano.data.genai.repository.SpeechRecognitionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SpeechRecognitionViewModel @Inject constructor(
    private val repository: SpeechRecognitionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SpeechRecognitionState())
    val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    private var recognitionJob: Job? = null

    fun startListening() {
        if (state.value.isPreparing || state.value.isListening) return
        _state.update { it.copy(transcript = "", isPreparing = true, errorMessage = null) }
        recognitionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.transcribeFromMicrophone().collect(::handleResponse)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update {
                    it.copy(
                        isPreparing = false,
                        isListening = false,
                        errorMessage = "No se pudo iniciar el reconocimiento de voz. Verifica que AICore esté listo."
                    )
                }
            }
        }
    }

    fun stopListening() {
        if (!state.value.isPreparing && !state.value.isListening) return
        _state.update { it.copy(isPreparing = false, isListening = false) }
        viewModelScope.launch(Dispatchers.IO) { repository.stop() }
    }

    fun showMicrophonePermissionRequired() {
        _state.update {
            it.copy(errorMessage = "Se necesita permiso para usar el micrófono.")
        }
    }

    private fun handleResponse(response: SpeechRecognizerResponse) {
        when (response) {
            is SpeechRecognizerResponse.PartialTextResponse -> updateTranscript(response.text)
            is SpeechRecognizerResponse.FinalTextResponse -> updateTranscript(response.text)
            SpeechRecognizerResponse.CompletedResponse -> _state.update {
                it.copy(isPreparing = false, isListening = false)
            }
            is SpeechRecognizerResponse.ErrorResponse -> _state.update {
                it.copy(
                    isPreparing = false,
                    isListening = false,
                    errorMessage = "La transcripción se interrumpió. Inténtalo nuevamente."
                )
            }
        }
    }

    private fun updateTranscript(text: String) {
        _state.update { it.copy(transcript = text, isPreparing = false, isListening = true) }
    }

    override fun onCleared() {
        recognitionJob?.cancel()
        repository.close()
        super.onCleared()
    }
}
