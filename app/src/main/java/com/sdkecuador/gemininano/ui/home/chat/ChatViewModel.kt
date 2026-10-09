package com.sdkecuador.gemininano.ui.home.chat

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdkecuador.gemininano.bytesToMB
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.ChatHistoryMessage
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount
import com.sdkecuador.gemininano.data.genai.repository.ChatRepository
import com.sdkecuador.gemininano.data.genai.repository.DEFAULT_IMAGE_PROMPT
import com.sdkecuador.gemininano.ui.home.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var job: Job? = null
    private var submittedMessage: String? = null
    private var submittedImage: Bitmap? = null
    private var submittedTurnStartIndex: Int? = null

    /**
     * Paso 2. Valida y guarda el borrador que llega desde `Chat.kt -> MessageInput()`.
     * Siguiente paso: `Chat.kt -> ChatScreen()` observa `state` y recompone la UI (paso 3).
     */
    fun onInputChange(input: String) {
        if (input.estimatedTokenCount() > AiInputLimits.MAX_SHORT_TEXT_TOKENS) return
        _state.update { it.copy(input = input, errorMessage = null) }
    }

    fun onImageSelected(image: Bitmap) {
        _state.update { it.copy(pendingImage = image, errorMessage = null) }
    }

    fun onRemoveImage() {
        _state.update { it.copy(pendingImage = null) }
    }

    fun onImageSelectionFailed() {
        _state.update { it.copy(errorMessage = "No se pudo abrir la imagen. Selecciona otra.") }
    }

    /**
     * Paso 4. Toma el mensaje confirmado por la UI y empieza a recopilar la operación de IA.
     * Siguiente paso: `ChatRepository.kt -> ChatRepository.send()` (paso 5).
     */
    fun onSend() {
        val current = state.value
        val image = current.pendingImage
        val text = current.input.trim().ifBlank { if (image != null) DEFAULT_IMAGE_PROMPT else "" }
        if (current.isLoading || text.isBlank()) return
        job?.cancel()
        submittedMessage = text
        submittedImage = image
        submittedTurnStartIndex = null
        val history = current.messages
            .filter { it.includeInContext }
            .map { ChatHistoryMessage(text = it.message, isFromUser = it.isFromUser, image = it.image) }
        job = viewModelScope.launch(Dispatchers.IO) {
            repository.send(text, history, image).collect(::handleOperation)
        }
    }

    /**
     * Paso 6. Reduce los estados de preparación y descarga; agrega el mensaje del usuario y
     * actualiza el estado visual.
     * Siguiente paso: `Chat.kt -> ChatContent()` recompone la interfaz (paso 7).
     */
    private fun handleOperation(operation: AiOperationState<String>) = when (operation) {
        AiOperationState.Preparing -> {
            submittedTurnStartIndex = _state.value.messages.size
            _state.update {
                it.copy(
                    input = "",
                    pendingImage = null,
                    isLoading = true,
                    errorMessage = null,
                    mbToDownload = 0.0,
                    mbDownloaded = 0.0,
                    messages = it.messages + ChatMessage(
                        message = submittedMessage.orEmpty(),
                        isFromUser = true,
                        image = submittedImage
                    )
                )
            }
        }
        is AiOperationState.Downloading -> updateDownload(operation.progress)
        is AiOperationState.Generating -> {
            // Paso 9. Crea o actualiza la burbuja del asistente con cada fragmento recibido.
            // Siguiente paso: Chat.kt -> MessageList() muestra y desplaza el texto (paso 10).
            if (operation.partialResult == null) {
                _state.update { it.copy(messages = it.messages + ChatMessage("", false)) }
            } else if (operation.partialResult.isNotEmpty()) {
                _state.update { current ->
                    val messages = current.messages.toMutableList()
                    val last = messages.last()
                    messages[messages.lastIndex] = last.copy(message = last.message + operation.partialResult)
                    current.copy(messages = messages)
                }
            }
            Unit
        }
        // Paso 11. Finaliza la carga o transforma el fallo en un mensaje para la UI.
        // Siguiente paso: Chat.kt -> ChatContent() muestra el resultado y reactiva la entrada (paso 12).
        is AiOperationState.Success -> {
            _state.update {
                it.copy(isLoading = false, mbToDownload = 0.0, mbDownloaded = 0.0)
            }
            submittedMessage = null
            submittedImage = null
            submittedTurnStartIndex = null
        }
        is AiOperationState.Failure -> {
            submittedMessage = null
            submittedImage = null
            setError(operation.error.toUserMessage(), failedTurnStart = submittedTurnStartIndex)
            submittedTurnStartIndex = null
        }
    }

    private fun updateDownload(progress: ModelDownloadProgress) {
        _state.update {
            it.copy(
                mbToDownload = if (progress.bytesToDownload > 0) {
                    progress.bytesToDownload.bytesToMB()
                } else {
                    it.mbToDownload
                },
                mbDownloaded = progress.bytesDownloaded.bytesToMB()
            )
        }
    }

    private fun setError(message: String, failedTurnStart: Int? = null) {
        _state.update { current ->
            val messages = if (failedTurnStart != null) {
                current.messages.mapIndexed { index, chatMessage ->
                    if (index >= failedTurnStart) {
                        chatMessage.copy(includeInContext = false)
                    } else {
                        chatMessage
                    }
                }
            } else {
                current.messages
            }
            current.copy(
                messages = messages,
                isLoading = false,
                errorMessage = message,
                mbToDownload = 0.0,
                mbDownloaded = 0.0
            )
        }
    }

    override fun onCleared() {
        job?.cancel()
        repository.close()
        super.onCleared()
    }
}
