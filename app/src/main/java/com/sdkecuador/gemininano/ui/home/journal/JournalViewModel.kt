package com.sdkecuador.gemininano.ui.home.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.sdkecuador.gemininano.data.genai.repository.SpeechRecognitionRepository
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount
import com.sdkecuador.gemininano.data.journal.JournalAiRepository
import com.sdkecuador.gemininano.data.journal.JournalRepository
import com.sdkecuador.gemininano.data.journal.resolveDeleteTarget
import com.sdkecuador.gemininano.ui.home.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journal: JournalRepository,
    private val ai: JournalAiRepository,
    private val speech: SpeechRecognitionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(JournalUiState())
    val state: StateFlow<JournalUiState> = _state.asStateFlow()
    private var speechJob: Job? = null
    private var processingJob: Job? = null
    private val processWhenCompleted = AtomicBoolean(false)

    init {
        viewModelScope.launch { journal.entries.collect { entries -> _state.update { it.copy(entries = entries) } } }
    }

    fun startListening() {
        if (state.value.isListening || state.value.isPreparing || state.value.isProcessing) return
        processWhenCompleted.set(false)
        _state.update { it.copy(transcript = "", isPreparing = true, message = null, error = null) }
        speechJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                speech.transcribeFromMicrophone().collect(::onSpeechResponse)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(isPreparing = false, isListening = false, error = "No se pudo transcribir el audio en este dispositivo.") }
            }
        }
    }

    fun stopListening() {
        if (!state.value.isListening && !state.value.isPreparing) return
        processWhenCompleted.set(true)
        _state.update { it.copy(isListening = false, isPreparing = false) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                speech.stop()
            } catch (_: Exception) {
                processWhenCompleted.set(false)
                _state.update { it.copy(error = "No se pudo detener el dictado. Puedes procesar el texto capturado.") }
            }
        }
    }

    fun microphonePermissionDenied() {
        _state.update { it.copy(error = "Permite el acceso al micrófono para dictar una entrada.") }
    }

    fun onTranscriptChange(text: String) {
        if (state.value.isListening || state.value.isProcessing || text.estimatedTokenCount() > AiInputLimits.MAX_SHORT_TEXT_TOKENS) return
        _state.update { it.copy(transcript = text, error = null) }
    }

    private fun onSpeechResponse(response: SpeechRecognizerResponse) {
        when (response) {
            is SpeechRecognizerResponse.PartialTextResponse ->
                _state.update { it.copy(transcript = response.text, isPreparing = false, isListening = !processWhenCompleted.get()) }
            is SpeechRecognizerResponse.FinalTextResponse ->
                _state.update { it.copy(transcript = response.text, isPreparing = false, isListening = !processWhenCompleted.get()) }
            SpeechRecognizerResponse.CompletedResponse -> {
                _state.update { it.copy(isPreparing = false, isListening = false) }
                if (processWhenCompleted.getAndSet(false)) processTranscript()
            }
            is SpeechRecognizerResponse.ErrorResponse ->
                _state.update { it.copy(isPreparing = false, isListening = false, error = "El dictado se interrumpió. Puedes intentarlo de nuevo.") }
        }
    }

    fun processTranscript() {
        val spokenText = state.value.transcript.trim()
        if (spokenText.isBlank() || state.value.isProcessing || processingJob?.isActive == true) {
            if (spokenText.isBlank()) _state.update { it.copy(error = "No se detectó ninguna instrucción hablada.") }
            return
        }
        processWhenCompleted.set(false)
        processingJob = viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isProcessing = true, error = null, message = "Gemini Nano está interpretando el dictado…") }
            try {
                ai.prepare { }
                val recent = journal.recentEntries()
                val decision = ai.interpret(spokenText, recent)
                when (decision.action) {
                    "CREATE" -> {
                        val body = decision.body.trim().ifBlank { spokenText }
                        val title = decision.title.trim().ifBlank { body.take(48) }
                        val mood = decision.mood.takeIf { it in MOODS } ?: "NEUTRO"
                        journal.add(spokenText, title, body, mood)
                        _state.update { it.copy(transcript = "", message = "Entrada guardada en el dispositivo.") }
                    }
                    "DELETE" -> {
                        val target = resolveDeleteTarget(decision.targetIndex, spokenText, recent)
                        if (target == null) {
                            _state.update { it.copy(message = "No identifiqué una entrada única. Di su título o posición.") }
                        } else {
                            _state.update { it.copy(pendingDelete = target, message = "Confirma la eliminación de «${target.title}».") }
                        }
                    }
                    "THEME" -> {
                        val palette = decision.palette.takeIf { it in PALETTES } ?: "DEFAULT"
                        journal.changePalette(palette)
                        _state.update { it.copy(transcript = "", message = "Color de la app actualizado.") }
                    }
                    else -> error("Gemini Nano devolvió una acción desconocida.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val message = if (error is AiException) error.toUserMessage()
                    else "No se pudo interpretar la instrucción en este dispositivo. Inténtalo de nuevo."
                _state.update { it.copy(error = message, message = null) }
            } finally {
                _state.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun requestDelete(entry: com.sdkecuador.gemininano.data.journal.JournalEntry) {
        _state.update { it.copy(pendingDelete = entry) }
    }

    fun cancelDelete() { _state.update { it.copy(pendingDelete = null) } }

    fun confirmDelete() {
        val entry = state.value.pendingDelete ?: return
        _state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch(Dispatchers.IO) {
            if (journal.delete(entry.id)) _state.update { it.copy(transcript = "", message = "Entrada eliminada.") }
        }
    }

    override fun onCleared() {
        speechJob?.cancel()
        processingJob?.cancel()
        speech.close()
        ai.close()
        super.onCleared()
    }

    companion object {
        private val MOODS = setOf("CANSADO", "ALEGRE", "APRENDIZAJE", "TRANQUILO", "TRISTE", "NEUTRO")
        private val PALETTES = setOf("DEFAULT", "FOREST", "SUNSET", "OCEAN", "LAVENDER")
    }
}
