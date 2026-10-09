package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.CountTokensResponse
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.InputProblem
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.model.ModelUnavailableReason
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class RepositoryErrorsTest {
    @Test
    fun blankInputIsRejectedBeforeStatusQueryOrGeneration() {
        val repository = ChatRepository(PromptOnlyFactory {
            modelStub { method ->
                when (method) {
                    "close" -> null
                    else -> error("No debe llamarse al SDK: $method")
                }
            }
        })
        try {
            val thrown = assertThrows(AiException.InvalidInput::class.java) {
                runBlocking { repository.streamResponse(" \n\t").toList() }
            }
            assertEquals(InputProblem.EMPTY_TEXT, thrown.problem)
        } finally {
            repository.close()
        }
    }

    @Test
    fun unavailableAndDownloadingModelsDoNotStartAnotherDownload() {
        val cases = listOf(
            FeatureStatus.UNAVAILABLE to ModelUnavailableReason.UNSUPPORTED,
            FeatureStatus.DOWNLOADING to ModelUnavailableReason.DOWNLOADING
        )
        for ((status, reason) in cases) {
            val repository = ChatRepository(PromptOnlyFactory {
                modelStub { method ->
                    when (method) {
                        "checkStatus" -> status
                        "close" -> null
                        else -> error("No debe llamarse $method con estado $status")
                    }
                }
            })
            try {
                val thrown = assertThrows(AiException.ModelUnavailable::class.java) {
                    runBlocking { repository.prepare { error("No debe haber progreso") } }
                }
                assertEquals(reason, thrown.reason)
            } finally {
                repository.close()
            }
        }
    }

    @Test
    fun directGenerationRequiresADownloadedModel() {
        val repository = ChatRepository(PromptOnlyFactory {
            modelStub { method ->
                when (method) {
                    "checkStatus" -> FeatureStatus.DOWNLOADABLE
                    "close" -> null
                    else -> error("No debe generarse contenido antes de descargar")
                }
            }
        })
        try {
            val thrown = assertThrows(AiException.ModelUnavailable::class.java) {
                runBlocking { repository.streamResponse("Hola").toList() }
            }
            assertEquals(ModelUnavailableReason.NOT_DOWNLOADED, thrown.reason)
        } finally {
            repository.close()
        }
    }

    @Test
    fun statusQueryFailureIsNotReportedAsUnsupportedDevice() {
        val failure = IllegalStateException("AICore temporalmente inaccesible")
        val repository = ImageDescriptionRepository(ImageOnlyFactory {
            imageDescriberStub { method ->
                when (method) {
                    "checkFeatureStatus" -> throw failure
                    "close" -> null
                    else -> error("Llamada inesperada: $method")
                }
            }
        })
        try {
            val thrown = assertThrows(AiException.StatusCheckFailed::class.java) {
                runBlocking { repository.featureStatus() }
            }
            assertSame(failure, thrown.cause)
        } finally {
            repository.close()
        }
    }

    @Test
    fun downloadFailedEventPreservesCauseAndStopsPreparation() {
        val failure = GenAiException(IllegalStateException("Sin espacio"), GenAiException.ErrorCode.NOT_ENOUGH_DISK_SPACE)
        val repository = ChatRepository(PromptOnlyFactory {
            modelStub { method ->
                when (method) {
                    "checkStatus" -> FeatureStatus.DOWNLOADABLE
                    "download" -> flowOf<DownloadStatus>(DownloadStatus.DownloadFailed(failure))
                    "close" -> null
                    else -> error("No debe generarse contenido tras fallar la descarga")
                }
            }
        })
        try {
            val thrown = assertThrows(AiException.DownloadFailed::class.java) {
                runBlocking { repository.prepare {} }
            }
            assertSame(failure, thrown.cause)
        } finally {
            repository.close()
        }
    }

    @Test
    fun synchronousDownloadFailureIsAlsoTranslated() {
        val failure = IllegalStateException("No se pudo iniciar la descarga")
        val repository = ImageDescriptionRepository(ImageOnlyFactory {
            imageDescriberStub { method ->
                when (method) {
                    "downloadFeature" -> throw failure
                    "close" -> null
                    else -> error("Llamada inesperada: $method")
                }
            }
        })
        try {
            val thrown = assertThrows(AiException.DownloadFailed::class.java) {
                runBlocking { repository.download {} }
            }
            assertSame(failure, thrown.cause)
        } finally {
            repository.close()
        }
    }

    @Test
    fun successfulDownloadKeepsProgressAndAnEmptyGenerationIsAnError() {
        var downloaded = false
        val repository = ChatRepository(PromptOnlyFactory {
            modelStub { method ->
                when (method) {
                    "checkStatus" -> if (downloaded) FeatureStatus.AVAILABLE else FeatureStatus.DOWNLOADABLE
                    "download" -> flow {
                        emit(DownloadStatus.DownloadStarted(100))
                        emit(DownloadStatus.DownloadProgress(100))
                        downloaded = true
                        emit(DownloadStatus.DownloadCompleted)
                    }
                    "generateContentStream" -> emptyFlow<GenerateContentResponse>()
                    "countTokens" -> CountTokensResponse(1)
                    "getTokenLimit" -> 2_048
                    "close" -> null
                    else -> error("Llamada inesperada: $method")
                }
            }
        })
        try {
            val progress = mutableListOf<ModelDownloadProgress>()
            runBlocking { repository.prepare { progress.add(it) } }
            assertEquals(100L, progress.first().bytesToDownload)
            assertEquals(100L, progress[1].bytesDownloaded)
            assertThrows(AiException.EmptyResult::class.java) {
                runBlocking { repository.streamResponse("Hola").toList() }
            }
        } finally {
            repository.close()
        }
    }

    @Test
    fun sdkInputErrorDuringStreamingIsTranslated() {
        val failure = GenAiException(IllegalArgumentException("Entrada demasiado larga"), GenAiException.ErrorCode.REQUEST_TOO_LARGE)
        val repository = ChatRepository(PromptOnlyFactory {
            modelStub { method ->
                when (method) {
                    "checkStatus" -> FeatureStatus.AVAILABLE
                    "countTokens" -> CountTokensResponse(1)
                    "getTokenLimit" -> 2_048
                    "generateContentStream" -> flow<GenerateContentResponse> { throw failure }
                    "close" -> null
                    else -> error("Llamada inesperada: $method")
                }
            }
        })
        try {
            val thrown = assertThrows(AiException.InvalidInput::class.java) {
                runBlocking { repository.streamResponse("Hola").toList() }
            }
            assertEquals(InputProblem.TEXT_TOO_LONG, thrown.problem)
            assertSame(failure, thrown.cause)
        } finally {
            repository.close()
        }
    }

    @Test
    fun promptUsesTheModelTokenLimitBeforeStartingGeneration() {
        val repository = ChatRepository(PromptOnlyFactory {
            modelStub { method ->
                when (method) {
                    "checkStatus" -> FeatureStatus.AVAILABLE
                    "countTokens" -> CountTokensResponse(769)
                    "getTokenLimit" -> 1_024
                    "generateContentStream" -> error("No debe generar fuera del límite del modelo")
                    "close" -> null
                    else -> error("Llamada inesperada: $method")
                }
            }
        })
        try {
            val thrown = assertThrows(AiException.InvalidInput::class.java) {
                runBlocking { repository.streamResponse("Hola").toList() }
            }
            assertEquals(InputProblem.TEXT_TOO_LONG, thrown.problem)
            assertEquals(768, thrown.maximumTokens)
            assertEquals(769, thrown.actualTokens)
        } finally {
            repository.close()
        }
    }
}
