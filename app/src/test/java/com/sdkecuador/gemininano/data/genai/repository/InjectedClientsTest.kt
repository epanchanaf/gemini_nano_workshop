package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.CountTokensResponse
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class InjectedClientsTest {
    @Test
    fun repositoriesReadStatusWithoutInitializingMlKit() = runBlocking {
        val factory = PromptOnlyFactory {
            modelStub { method ->
                when (method) {
                    "checkStatus" -> FeatureStatus.DOWNLOADABLE
                    "close" -> null
                    else -> error("Llamada inesperada: $method")
                }
            }
        }
        val chat = ChatRepository(factory)
        val images = ImageDescriptionRepository(factory)
        try {
            assertEquals(AiCoreFeatureStatus.DOWNLOADABLE, chat.featureStatus())
            assertEquals(AiCoreFeatureStatus.DOWNLOADABLE, images.featureStatus())
        } finally {
            chat.close()
            images.close()
        }
    }

    @Test
    fun closingOneRepositoryDoesNotCloseAnotherRepositoryClient() = runBlocking {
        val closedClients = mutableListOf<Int>()
        var nextId = 0
        val factory = PromptOnlyFactory {
            val id = nextId++
            modelStub { method ->
                when (method) {
                    "checkStatus" -> {
                        check(id !in closedClients)
                        FeatureStatus.AVAILABLE
                    }
                    "close" -> {
                        closedClients.add(id)
                        null
                    }
                    else -> error("Llamada inesperada: $method")
                }
            }
        }
        val chat = ChatRepository(factory)
        val images = ImageDescriptionRepository(factory)

        chat.close()

        assertEquals(listOf(0), closedClients)
        assertEquals(AiCoreFeatureStatus.AVAILABLE, images.featureStatus())
        images.close()
        assertEquals(listOf(0, 1), closedClients)
    }

    @Test
    fun streamingPropagatesCancellationFromInjectedClient() {
        val cancellation = CancellationException("Generación cancelada")
        val repository = ChatRepository(PromptOnlyFactory {
            modelStub { method ->
                when (method) {
                    "checkStatus" -> FeatureStatus.AVAILABLE
                    "countTokens" -> CountTokensResponse(1)
                    "getTokenLimit" -> 2_048
                    "generateContentStream" -> flow<GenerateContentResponse> { throw cancellation }
                    "close" -> null
                    else -> error("Llamada inesperada: $method")
                }
            }
        })
        try {
            val thrown = assertThrows(CancellationException::class.java) {
                runBlocking { repository.streamResponse("Hola").toList() }
            }
            assertSame(cancellation, thrown)
        } finally {
            repository.close()
        }
    }
}
