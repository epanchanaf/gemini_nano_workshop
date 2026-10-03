package com.sdkecuador.gemininano.ui.home.summarization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdkecuador.gemininano.bytesToMB
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount
import com.sdkecuador.gemininano.data.genai.repository.SummarizationRepository
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
class SummarizationViewModel @Inject constructor(
    private val repository: SummarizationRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SummarizationState())
    val state: StateFlow<SummarizationState> = _state.asStateFlow()

    private var job: Job? = null

    fun onInputChange(input: String) {
        if (input.estimatedTokenCount() > AiInputLimits.MAX_SUMMARY_TOKENS) return
        _state.update { it.copy(input = input, errorMessage = null) }
    }

    fun summarize() {
        val text = state.value.input.trim()
        if (state.value.isLoading) return
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            repository.summarizeText(text).collect(::handleOperation)
        }
    }

    private fun handleOperation(operation: AiOperationState<String>) = when (operation) {
        AiOperationState.Preparing -> _state.update {
            it.copy(
                isLoading = true,
                isDownloadingModel = false,
                summary = "",
                errorMessage = null,
                mbToDownload = 0.0,
                mbDownloaded = 0.0
            )
        }
        is AiOperationState.Downloading -> updateDownload(operation.progress)
        is AiOperationState.Generating -> operation.partialResult?.let { partial ->
            _state.update { it.copy(summary = it.summary + partial, isDownloadingModel = false) }
        }
        is AiOperationState.Success -> _state.update {
            it.copy(summary = operation.result, isLoading = false, isDownloadingModel = false)
        }
        is AiOperationState.Failure -> setError(operation.error.toUserMessage())
    }

    fun clearSummary() {
        job?.cancel()
        _state.update {
            it.copy(
                summary = "",
                errorMessage = null,
                isLoading = false,
                isDownloadingModel = false
            )
        }
    }

    private fun updateDownload(progress: ModelDownloadProgress) {
        _state.update {
            it.copy(
                isDownloadingModel = true,
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
            it.copy(isLoading = false, isDownloadingModel = false, errorMessage = message)
        }
    }

    override fun onCleared() {
        job?.cancel()
        repository.close()
        super.onCleared()
    }
}
