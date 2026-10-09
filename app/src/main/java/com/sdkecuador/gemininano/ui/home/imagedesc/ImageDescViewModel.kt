package com.sdkecuador.gemininano.ui.home.imagedesc

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.GenAiException
import com.sdkecuador.gemininano.bytesToMB
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.repository.ImageDescriptionRepository
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
class ImageDescViewModel @Inject constructor(
    private val repository: ImageDescriptionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ImageDescState())
    val state: StateFlow<ImageDescState> = _state.asStateFlow()

    private var job: Job? = null

    fun describe(bitmap: Bitmap) {
        job?.cancel()
        job = viewModelScope.launch(Dispatchers.IO) {
            repository.describeImage(bitmap).collect(::handleOperation)
        }
    }

    private fun handleOperation(operation: AiOperationState<String>) = when (operation) {
        AiOperationState.Preparing -> _state.update {
            it.copy(
                isLoading = true,
                description = "",
                errorMessage = null,
                mbToDownload = 0.0,
                mbDownloaded = 0.0
            )
        }
        is AiOperationState.Downloading -> updateDownload(operation.progress)
        is AiOperationState.Generating -> Unit
        is AiOperationState.Success -> _state.update {
            it.copy(
                description = operation.result,
                isLoading = false,
                mbToDownload = 0.0,
                mbDownloaded = 0.0
            )
        }
        is AiOperationState.Failure -> {
            Log.e("ImageDescription", "Fallo al describir la imagen", operation.error)
            setError(operation.error.toImageDescriptionMessage())
        }
    }

    fun clearDescription() {
        job?.cancel()
        _state.update { it.copy(description = "", errorMessage = null, isLoading = false) }
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

private fun AiException.toImageDescriptionMessage(): String {
    val sdkError = generateSequence(cause) { it.cause }
        .filterIsInstance<GenAiException>()
        .firstOrNull()
    return if (sdkError == null) toUserMessage()
    else "${toUserMessage()} Código ML Kit: ${sdkError.errorCode}."
}
