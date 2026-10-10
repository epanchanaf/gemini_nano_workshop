package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.common.GenAiException
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount
import com.sdkecuador.gemininano.data.genai.model.InputProblem
import com.sdkecuador.gemininano.data.genai.model.ModelUnavailableReason
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow

internal enum class AiOperation {
    STATUS_CHECK,
    DOWNLOAD,
    GENERATION
}

/**
 * Ejecuta la operación completa y transforma los detalles del SDK en un único contrato de estado.
 * `onPartialResult` puede ser invocado por callbacks no suspendibles de ML Kit.
 */
internal fun <T> aiOperation(
    validate: () -> Unit,
    prepare: suspend (onDownloadProgress: (ModelDownloadProgress) -> Unit) -> Unit,
    generate: suspend (onPartialResult: (T) -> Unit) -> T
): Flow<AiOperationState<T>> = channelFlow {
    try {
        validate()
        send(AiOperationState.Preparing)
        prepare { progress -> trySend(AiOperationState.Downloading(progress)) }
        send(AiOperationState.Generating())
        val result = generate { partial -> trySend(AiOperationState.Generating(partial)) }
        send(AiOperationState.Success(result))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        send(AiOperationState.Failure(error.toAiException(AiOperation.GENERATION)))
    }
}

internal suspend fun <T> aiCall(operation: AiOperation, block: suspend () -> T): T = try {
    block()
} catch (error: Exception) {
    throw error.toAiException(operation)
}

/** catch solo intercepta errores del productor; los errores del consumidor del Flow se propagan. */
internal fun <T> Flow<T>.withAiErrors(operation: AiOperation): Flow<T> = catch { error ->
    if (error !is Exception) throw error
    throw error.toAiException(operation)
}

internal fun Exception.toAiException(operation: AiOperation): AiException {
    if (this is CancellationException) throw this
    if (this is AiException) return this
    if (this is GenAiException && errorCode == GenAiException.ErrorCode.CANCELLED) {
        throw CancellationException("ML Kit operation cancelled", this)
    }
    // Un fallo al descargar se clasifica por la operación, aunque el SDK use un código genérico.
    if (operation == AiOperation.DOWNLOAD) return AiException.DownloadFailed(this)
    if (this is GenAiException) {
        when (errorCode) {
            GenAiException.ErrorCode.REQUEST_TOO_SMALL ->
                return AiException.InvalidInput(InputProblem.TEXT_TOO_SHORT, cause = this)
            GenAiException.ErrorCode.REQUEST_TOO_LARGE ->
                return AiException.InvalidInput(InputProblem.TEXT_TOO_LONG, cause = this)
            GenAiException.ErrorCode.INVALID_INPUT_IMAGE ->
                return AiException.InvalidInput(InputProblem.INVALID_IMAGE, cause = this)
            GenAiException.ErrorCode.NOT_SUPPORTED, GenAiException.ErrorCode.AICORE_INCOMPATIBLE ->
                return AiException.ModelUnavailable(ModelUnavailableReason.UNSUPPORTED, this)
            GenAiException.ErrorCode.NOT_AVAILABLE ->
                return AiException.ModelUnavailable(ModelUnavailableReason.NOT_READY, this)
            GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE ->
                return AiException.ModelUnavailable(ModelUnavailableReason.NEEDS_SYSTEM_UPDATE, this)
        }
    }
    return when (operation) {
        AiOperation.STATUS_CHECK -> AiException.StatusCheckFailed(this)
        AiOperation.DOWNLOAD -> AiException.DownloadFailed(this)
        AiOperation.GENERATION -> AiException.GenerationFailed(this)
    }
}

internal fun validateTextInput(
    text: String,
    minimumCharacters: Int = 1,
    maximumEstimatedTokens: Int = Int.MAX_VALUE
) {
    val length = text.trim().length
    if (length == 0) throw AiException.InvalidInput(InputProblem.EMPTY_TEXT)
    if (length < minimumCharacters) {
        throw AiException.InvalidInput(InputProblem.TEXT_TOO_SHORT, minimumCharacters, length)
    }
    val estimatedTokens = text.estimatedTokenCount()
    if (estimatedTokens > maximumEstimatedTokens) {
        throw AiException.InvalidInput(
            problem = InputProblem.TEXT_TOO_LONG,
            maximumTokens = maximumEstimatedTokens,
            actualTokens = estimatedTokens
        )
    }
}

internal fun requireAvailable(status: AiCoreFeatureStatus) {
    val reason = when (status) {
        AiCoreFeatureStatus.AVAILABLE -> return
        AiCoreFeatureStatus.DOWNLOADABLE -> ModelUnavailableReason.NOT_DOWNLOADED
        AiCoreFeatureStatus.DOWNLOADING -> ModelUnavailableReason.DOWNLOADING
        AiCoreFeatureStatus.UNAVAILABLE -> ModelUnavailableReason.UNSUPPORTED
    }
    throw AiException.ModelUnavailable(reason)
}

/** Centraliza qué estados permiten continuar y cuándo hay que descargar. */
internal suspend fun prepareModel(
    statusProvider: suspend () -> AiCoreFeatureStatus,
    download: suspend () -> Unit
) {
    when (val status = statusProvider()) {
        AiCoreFeatureStatus.DOWNLOADABLE -> {
            download()
            requireAvailable(statusProvider())
        }
        else -> requireAvailable(status)
    }
}

internal fun String.requireResult(): String {
    if (isBlank()) throw AiException.EmptyResult()
    return this
}

/** Un fragmento vacío es válido; una generación completa sin texto útil es un error. */
internal fun Flow<String>.requireResult(): Flow<String> = flow {
    var hasText = false
    collect { chunk ->
        if (chunk.isNotBlank()) hasText = true
        emit(chunk)
    }
    if (!hasText) throw AiException.EmptyResult()
}
