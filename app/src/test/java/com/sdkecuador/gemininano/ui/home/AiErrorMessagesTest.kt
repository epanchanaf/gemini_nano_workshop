package com.sdkecuador.gemininano.ui.home

import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.InputProblem
import com.sdkecuador.gemininano.data.genai.model.ModelUnavailableReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AiErrorMessagesTest {
    @Test
    fun sdkDiagnosticTextIsNeverExposedToTheUser() {
        val diagnostic = "SDK_INTERNAL_PRIVATE_DIAGNOSTIC"
        val cause = IllegalStateException(diagnostic)
        val errors = listOf(
            AiException.InvalidInput(InputProblem.INVALID_IMAGE, cause = cause),
            AiException.ModelUnavailable(ModelUnavailableReason.NOT_READY, cause),
            AiException.DownloadFailed(cause),
            AiException.StatusCheckFailed(cause),
            AiException.GenerationFailed(cause)
        )
        for (error in errors) assertFalse(error.toUserMessage().contains(diagnostic))
    }

    @Test
    fun articleValidationRetainsTheUsefulMissingCharactersMessage() {
        val error = AiException.InvalidInput(InputProblem.TEXT_TOO_SHORT, minimumCharacters = 400, actualCharacters = 399)
        assertEquals(
            "El modo Artículo requiere al menos 400 caracteres. Agrega 1 carácter más.",
            error.toUserMessage()
        )
    }
}
