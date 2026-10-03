package com.sdkecuador.gemininano.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.repository.AiCoreStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelStatusViewModel @Inject constructor(
    private val repository: AiCoreStatusRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ModelStatusUiState())
    val state: StateFlow<ModelStatusUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val info = repository.getRuntimeInfo()
            _state.value = ModelStatusUiState(
                isLoading = false,
                appVersion = info.appVersion,
                aiCoreVersion = info.aiCoreVersion,
                runtimes = info.runtimes.map {
                    RuntimeStatus(
                        name = it.name,
                        detail = it.detail,
                        status = it.featureStatus.toReadableStatus(),
                        isReady = it.featureStatus == AiCoreFeatureStatus.AVAILABLE
                    )
                }
            )
        } catch (e: CancellationException) {
            if (currentCoroutineContext().isActive) {
                _state.update { it.copy(isLoading = false) }
            }
            throw e
        } catch (e: AiException) {
            _state.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
        } catch (e: Exception) {
            _state.update {
                it.copy(isLoading = false, errorMessage = "Ocurrió un error inesperado. Inténtalo de nuevo.")
            }
        }
    }
}

private fun AiCoreFeatureStatus.toReadableStatus() = when (this) {
    AiCoreFeatureStatus.AVAILABLE -> "Descargado y listo"
    AiCoreFeatureStatus.DOWNLOADABLE -> "Sin descargar"
    AiCoreFeatureStatus.DOWNLOADING -> "Descargando"
    AiCoreFeatureStatus.UNAVAILABLE -> "No compatible con este dispositivo"
}
