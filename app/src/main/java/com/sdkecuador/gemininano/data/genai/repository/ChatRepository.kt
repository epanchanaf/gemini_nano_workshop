package com.sdkecuador.gemininano.data.genai.repository

import android.graphics.Bitmap
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.mapper.toRepositoryStatus
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.ChatHistoryMessage
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.InputProblem
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.model.isSupportedImageSize
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

const val DEFAULT_IMAGE_PROMPT = "Describe esta imagen en español de forma breve y objetiva."

@ViewModelScoped
class ChatRepository @Inject constructor(
    clientFactory: GenAiClientFactory
) : AutoCloseable {
    private val model = clientFactory.createGenerativeModel()

    suspend fun featureStatus(): AiCoreFeatureStatus = aiCall(AiOperation.STATUS_CHECK) {
        model.checkStatus().toRepositoryStatus()
    }

    fun downloadProgress(): Flow<ModelDownloadProgress> =
        flow { emitAll(model.download()) }
            .toModelDownloadProgress()
            .withAiErrors(AiOperation.DOWNLOAD)

    suspend fun prepare(onProgress: (ModelDownloadProgress) -> Unit) {
        prepareModel(::featureStatus) { downloadProgress().collect(onProgress) }
    }

    fun validateInput(prompt: String, image: Bitmap? = null) {
        if (image == null || prompt.isNotBlank()) {
            validateTextInput(prompt, maximumEstimatedTokens = AiInputLimits.MAX_SHORT_TEXT_TOKENS)
        }
        if (image != null && (image.isRecycled || !isSupportedImageSize(image.width, image.height))) {
            throw AiException.InvalidInput(InputProblem.INVALID_IMAGE)
        }
    }

    /**
     * Paso 5. Valida el mensaje y coordina preparación, descarga, generación y errores como un Flow.
     * Siguiente paso: `ChatViewModel.kt -> ChatViewModel.handleOperation()` recibe cada estado (paso 6).
     */
    fun send(
        prompt: String,
        history: List<ChatHistoryMessage> = emptyList(),
        image: Bitmap? = null
    ): Flow<AiOperationState<String>> = aiOperation(
        validate = { validateInput(prompt, image) },
        prepare = ::prepare
    ) { onPartialResult ->
        buildString {
            streamResponse(prompt, history, image).collect { chunk ->
                append(chunk)
                onPartialResult(chunk)
            }
        }.requireResult()
    }

    /**
     * Paso 8. Valida el límite real, pide la respuesta en streaming y emite cada fragmento generado.
     * Siguiente paso: `ChatViewModel.kt -> ChatViewModel.handleOperation()` concatena el fragmento (paso 9).
     */
    fun streamResponse(
        prompt: String,
        history: List<ChatHistoryMessage> = emptyList(),
        image: Bitmap? = null
    ): Flow<String> = flow {
        validateInput(prompt, image)
        requireAvailable(featureStatus())
        val activeImage = image ?: history.lastOrNull { it.isFromUser && it.image != null }?.image
        if (activeImage != null) validateInput(DEFAULT_IMAGE_PROMPT, activeImage)
        val message = (prompt.takeIf { it.isNotBlank() } ?: DEFAULT_IMAGE_PROMPT)
            .withHistory(history, activeImage != null && image == null)
        val request = if (activeImage == null) {
            GenerateContentRequest.Builder(TextPart(message))
        } else {
            GenerateContentRequest.Builder(ImagePart(activeImage), TextPart(message))
        }
            .apply { maxOutputTokens = AiInputLimits.MAX_CHAT_OUTPUT_TOKENS }.build()
        validatePromptTokens(request)
        if (activeImage == null) {
            emitAll(model.generateContentStream(request).map { it.candidates.firstOrNull()?.text.orEmpty() })
        } else {
            emit(model.generateContent(request).candidates.firstOrNull()?.text.orEmpty())
        }
    }.requireResult().withAiErrors(AiOperation.GENERATION)

    private fun String.withHistory(history: List<ChatHistoryMessage>, hasPreviousImage: Boolean): String {
        if (history.isEmpty()) return this
        return buildString {
            appendLine("Responde al último mensaje del usuario usando esta conversación previa:")
            if (hasPreviousImage) appendLine("La imagen adjunta es la imagen más reciente de esta conversación.")
            history.forEach { message ->
                append(if (message.isFromUser) "Usuario: " else "Asistente: ")
                if (message.image != null) append("[imagen adjunta] ")
                appendLine(message.text)
            }
            append("Usuario: ").appendLine(this@withHistory)
            append("Asistente:")
        }
    }

    private suspend fun validatePromptTokens(request: GenerateContentRequest) {
        val inputTokens = model.countTokens(request).totalTokens
        val maximumInputTokens = (model.getTokenLimit() - AiInputLimits.MAX_CHAT_OUTPUT_TOKENS)
            .coerceAtLeast(0)
        if (inputTokens > maximumInputTokens) {
            throw AiException.InvalidInput(
                problem = InputProblem.TEXT_TOO_LONG,
                maximumTokens = maximumInputTokens,
                actualTokens = inputTokens
            )
        }
    }

    suspend fun baseModelName(): String? = aiCall(AiOperation.STATUS_CHECK) {
        model.getBaseModelName()
    }

    override fun close() = model.close()
}
