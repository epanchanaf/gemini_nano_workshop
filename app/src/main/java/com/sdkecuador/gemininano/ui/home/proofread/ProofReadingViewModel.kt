package com.sdkecuador.gemininano.ui.home.proofread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdkecuador.gemininano.bytesToMB
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount
import com.sdkecuador.gemininano.data.genai.repository.ProofreadingRepository
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
class ProofReadingViewModel @Inject constructor(
    private val repository: ProofreadingRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProofReadingState())
    val state: StateFlow<ProofReadingState> = _state.asStateFlow()

    private var job: Job? = null

    fun onInputChange(input: String) {
        if (input.estimatedTokenCount() > AiInputLimits.MAX_SHORT_TEXT_TOKENS) return
        _state.update { it.copy(input = input, suggestion = "", errorMessage = null) }
    }

    fun submitProofreading() {
        val text = state.value.input.trim()
        if (state.value.isLoading) return
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            repository.correct(text).collect(::handleOperation)
        }
    }

    private fun handleOperation(operation: AiOperationState<String>) = when (operation) {
        AiOperationState.Preparing -> _state.update {
            it.copy(
                isLoading = true,
                suggestion = "",
                errorMessage = null,
                mbToDownload = 0.0,
                mbDownloaded = 0.0
            )
        }
        is AiOperationState.Downloading -> updateDownload(operation.progress)
        is AiOperationState.Generating -> operation.partialResult?.let { partial ->
            _state.update { it.copy(suggestion = it.suggestion + partial) }
        }
        is AiOperationState.Success -> _state.update {
            it.copy(suggestion = operation.result, isLoading = false)
        }
        is AiOperationState.Failure -> setError(operation.error.toUserMessage())
    }

    fun applySuggestion() {
        _state.update { it.copy(input = it.suggestion, suggestion = "") }
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
