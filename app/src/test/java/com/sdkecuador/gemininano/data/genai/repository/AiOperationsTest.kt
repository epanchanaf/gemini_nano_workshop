package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.common.GenAiException
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.InputProblem
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.model.ModelUnavailableReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class AiOperationsTest {
    @Test
    fun operationEmitsPreparationDownloadGenerationAndSuccessInOrder() = runBlocking {
        val states = aiOperation(
            validate = {},
            prepare = { onProgress ->
                onProgress(ModelDownloadProgress(bytesToDownload = 100))
                onProgress(ModelDownloadProgress(bytesDownloaded = 40))
            },
            generate = { onPartialResult ->
                onPartialResult("Hola")
                "Hola mundo"
            }
        ).toList()

        assertEquals(AiOperationState.Preparing, states[0])
        assertEquals(AiOperationState.Downloading(ModelDownloadProgress(bytesToDownload = 100)), states[1])
        assertEquals(AiOperationState.Downloading(ModelDownloadProgress(bytesDownloaded = 40)), states[2])
        assertEquals(AiOperationState.Generating<String>(), states[3])
        assertEquals(AiOperationState.Generating("Hola"), states[4])
        assertEquals(AiOperationState.Success("Hola mundo"), states[5])
    }

    @Test
    fun operationEmitsFailureWithoutStartingPreparationWhenValidationFails() = runBlocking {
        val failure = AiException.InvalidInput(InputProblem.EMPTY_TEXT)
        val states = aiOperation<String>(
            validate = { throw failure },
            prepare = { error("No debe preparar una entrada inválida") },
            generate = { error("No debe generar una entrada inválida") }
        ).toList()

        assertEquals(listOf(AiOperationState.Failure(failure)), states)
    }

    @Test
    fun operationStopsBeforeGenerationWhenDownloadFails() = runBlocking {
        val failure = AiException.DownloadFailed(IllegalStateException("Sin espacio"))
        var generated = false
        val states = aiOperation(
            validate = {},
            prepare = { throw failure },
            generate = {
                generated = true
                "No debe generarse"
            }
        ).toList()

        assertEquals(AiOperationState.Preparing, states.first())
        assertEquals(AiOperationState.Failure(failure), states.last())
        assertEquals(false, generated)
    }

    @Test
    fun articleValidationUsesTrimmedLengthAndReportsMissingCharacters() {
        val thrown = assertThrows(AiException.InvalidInput::class.java) {
            validateTextInput("  ${"a".repeat(399)}  ", minimumCharacters = 400)
        }
        assertEquals(InputProblem.TEXT_TOO_SHORT, thrown.problem)
        assertEquals(399, thrown.actualCharacters)
        assertEquals(400, thrown.minimumCharacters)
        validateTextInput("a".repeat(400), minimumCharacters = 400)
    }

    @Test
    fun textValidationRejectsEstimatedTokensAboveTheConfiguredLimit() {
        val thrown = assertThrows(AiException.InvalidInput::class.java) {
            validateTextInput(
                text = "a".repeat(AiInputLimits.MAX_SHORT_TEXT_TOKENS * 4 + 1),
                maximumEstimatedTokens = AiInputLimits.MAX_SHORT_TEXT_TOKENS
            )
        }
        assertEquals(InputProblem.TEXT_TOO_LONG, thrown.problem)
        assertEquals(AiInputLimits.MAX_SHORT_TEXT_TOKENS, thrown.maximumTokens)
        assertEquals(AiInputLimits.MAX_SHORT_TEXT_TOKENS + 1, thrown.actualTokens)
    }

    @Test
    fun finalBlankResultsAndWhitespaceOnlyStreamsAreErrors() {
        assertThrows(AiException.EmptyResult::class.java) { " \n".requireResult() }
        assertThrows(AiException.EmptyResult::class.java) {
            runBlocking { flowOf("", " ", "\n").requireResult().toList() }
        }
    }

    @Test
    fun emptyIntermediateChunksDoNotInvalidateUsefulStreamingText() = runBlocking {
        val chunks = listOf("", "Hola", " ", "mundo", "")
        assertEquals(chunks, flowOf(*chunks.toTypedArray()).requireResult().toList())
        assertEquals("Hola", "Hola".requireResult())
    }

    @Test
    fun sdkErrorsDistinguishInputAndModelAvailability() {
        for ((code, problem) in listOf(
            GenAiException.ErrorCode.REQUEST_TOO_SMALL to InputProblem.TEXT_TOO_SHORT,
            GenAiException.ErrorCode.REQUEST_TOO_LARGE to InputProblem.TEXT_TOO_LONG,
            GenAiException.ErrorCode.INVALID_INPUT_IMAGE to InputProblem.INVALID_IMAGE
        )) {
            val failure = GenAiException(IllegalStateException("SDK"), code)
            val mapped = failure.toAiException(AiOperation.GENERATION) as AiException.InvalidInput
            assertEquals(problem, mapped.problem)
            assertSame(failure, mapped.cause)
        }
        for ((code, reason) in listOf(
            GenAiException.ErrorCode.NOT_SUPPORTED to ModelUnavailableReason.UNSUPPORTED,
            GenAiException.ErrorCode.AICORE_INCOMPATIBLE to ModelUnavailableReason.UNSUPPORTED,
            GenAiException.ErrorCode.NOT_AVAILABLE to ModelUnavailableReason.NOT_READY,
            GenAiException.ErrorCode.NEEDS_SYSTEM_UPDATE to ModelUnavailableReason.NEEDS_SYSTEM_UPDATE
        )) {
            val failure = GenAiException(IllegalStateException("SDK"), code)
            val mapped = failure.toAiException(AiOperation.GENERATION) as AiException.ModelUnavailable
            assertEquals(reason, mapped.reason)
            assertSame(failure, mapped.cause)
        }
    }

    @Test
    fun downloadErrorsRemainDownloadFailuresForGenericSdkCodes() {
        val failure = GenAiException(IllegalStateException("Descarga"), GenAiException.ErrorCode.NOT_AVAILABLE)
        val mapped = failure.toAiException(AiOperation.DOWNLOAD)
        assertEquals(AiException.DownloadFailed::class.java, mapped.javaClass)
        assertSame(failure, mapped.cause)
    }

    @Test
    fun genericErrorsAreClassifiedByOperationAndOwnErrorsKeepTheirIdentity() {
        val failure = IllegalStateException("SDK")
        for ((operation, expectedType) in listOf(
            AiOperation.STATUS_CHECK to AiException.StatusCheckFailed::class.java,
            AiOperation.DOWNLOAD to AiException.DownloadFailed::class.java,
            AiOperation.GENERATION to AiException.GenerationFailed::class.java
        )) {
            val thrown = assertThrows(AiException::class.java) {
                runBlocking { aiCall(operation) { throw failure } }
            }
            assertEquals(expectedType, thrown.javaClass)
            assertSame(failure, thrown.cause)
        }
        val ownError = AiException.EmptyResult()
        assertSame(ownError, ownError.toAiException(AiOperation.GENERATION))
    }

    @Test
    fun cancellationIsNeverConvertedIntoAnAiFailure() {
        val cancellation = CancellationException("Cancelado")
        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { aiCall(AiOperation.DOWNLOAD) { throw cancellation } }
        }
        assertSame(cancellation, thrown)
        val sdkCancellation = GenAiException(cancellation, GenAiException.ErrorCode.CANCELLED)
        val translated = assertThrows(CancellationException::class.java) {
            sdkCancellation.toAiException(AiOperation.GENERATION)
        }
        assertSame(sdkCancellation, translated.cause)
    }

    @Test
    fun downstreamFailuresAreNotMisclassifiedAsGenerationErrors() {
        val failure = IllegalStateException("Error del consumidor de UI")
        val thrown = assertThrows(IllegalStateException::class.java) {
            runBlocking {
                flowOf("Hola").withAiErrors(AiOperation.GENERATION).collect { throw failure }
            }
        }
        assertSame(failure, thrown)
    }
}
