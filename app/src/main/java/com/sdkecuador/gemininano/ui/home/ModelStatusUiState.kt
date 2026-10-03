package com.sdkecuador.gemininano.ui.home

data class RuntimeStatus(
    val name: String,
    val detail: String,
    val status: String,
    val isReady: Boolean = false
)

data class ModelStatusUiState(
    val isLoading: Boolean = true,
    val appVersion: String = "",
    val aiCoreVersion: String = "",
    val runtimes: List<RuntimeStatus> = emptyList(),
    val errorMessage: String? = null
)
