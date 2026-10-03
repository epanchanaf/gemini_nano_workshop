package com.sdkecuador.gemininano.data.genai.model

/** Estados emitidos durante una operación completa: validación, descarga e inferencia. */
sealed interface AiOperationState<out T> {
    data object Preparing : AiOperationState<Nothing>

    data class Downloading(
        val progress: ModelDownloadProgress
    ) : AiOperationState<Nothing>

    /** `partialResult` permite pintar una respuesta progresiva sin crear otro flujo. */
    data class Generating<T>(
        val partialResult: T? = null
    ) : AiOperationState<T>

    data class Success<T>(
        val result: T
    ) : AiOperationState<T>

    data class Failure(
        val error: AiException
    ) : AiOperationState<Nothing>
}
