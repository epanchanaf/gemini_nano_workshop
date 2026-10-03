package com.sdkecuador.gemininano.ui.home.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdkecuador.gemininano.bytesToMB
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount
import com.sdkecuador.gemininano.data.genai.repository.ChatRepository
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

    fun onInputChange(input: String) {
        if (input.estimatedTokenCount() > AiInputLimits.MAX_SHORT_TEXT_TOKENS) return
        _state.update { it.copy(input = input, errorMessage = null) }
    }

    fun onSend() {
        val text = state.value.input.trim()
        if (state.value.isLoading) return
        job?.cancel()
        submittedMessage = text
        job = viewModelScope.launch(Dispatchers.IO) {
            repository.send(text).collect(::handleOperation)
        }
    }

    private fun handleOperation(operation: AiOperationState<String>) = when (operation) {
        AiOperationState.Preparing -> _state.update {
            it.copy(
                input = "",
                isLoading = true,
                errorMessage = null,
                mbToDownload = 0.0,
                mbDownloaded = 0.0,
                messages = it.messages + ChatMessage(message = submittedMessage.orEmpty(), isFromUser = true)
            )
        }
        is AiOperationState.Downloading -> updateDownload(operation.progress)
        is AiOperationState.Generating -> {
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
        is AiOperationState.Success -> _state.update {
            it.copy(isLoading = false, mbToDownload = 0.0, mbDownloaded = 0.0)
        }
        is AiOperationState.Failure -> {
            submittedMessage = null
            setError(operation.error.toUserMessage())
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

    private fun setError(message: String) {
        _state.update {
            it.copy(isLoading = false, errorMessage = message, mbToDownload = 0.0, mbDownloaded = 0.0)
        }
    }

    override fun onCleared() {
        job?.cancel()
        repository.close()
        super.onCleared()
    }
}
