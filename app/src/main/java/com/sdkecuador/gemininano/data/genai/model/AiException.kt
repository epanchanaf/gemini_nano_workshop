package com.sdkecuador.gemininano.data.genai.model

/** Errores del contrato de datos. La causa original se conserva para diagnóstico. */
sealed class AiException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidInput(
        val problem: InputProblem,
        val minimumCharacters: Int = 0,
        val actualCharacters: Int = 0,
        val maximumTokens: Int = 0,
        val actualTokens: Int = 0,
        cause: Throwable? = null
    ) : AiException("Invalid input: $problem", cause)

    class ModelUnavailable(
        val reason: ModelUnavailableReason,
        cause: Throwable? = null
    ) : AiException("Model unavailable: $reason", cause)

    class DownloadFailed(cause: Throwable) : AiException("Model download failed", cause)

    class EmptyResult : AiException("The model returned no text")

    class StatusCheckFailed(cause: Throwable) : AiException("Model status check failed", cause)

    class GenerationFailed(cause: Throwable) : AiException("Content generation failed", cause)
}

enum class InputProblem {
    EMPTY_TEXT,
    TEXT_TOO_SHORT,
    TEXT_TOO_LONG,
    INVALID_IMAGE
}

enum class ModelUnavailableReason {
    UNSUPPORTED,
    NOT_DOWNLOADED,
    DOWNLOADING,
    NEEDS_SYSTEM_UPDATE,
    NOT_READY
}
