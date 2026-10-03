package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.TextPart
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.mapper.toRepositoryStatus
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.InputProblem
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@ViewModelScoped
class ChatRepository @Inject constructor(
    clientFactory: GenAiClientFactory
) : AutoCloseable {
    private val generativeModel = clientFactory.createGenerativeModel()

    suspend fun featureStatus(): AiCoreFeatureStatus = aiCall(AiOperation.STATUS_CHECK) {
        generativeModel.checkStatus().toRepositoryStatus()
    }

    fun downloadProgress(): Flow<ModelDownloadProgress> =
        flow { emitAll(generativeModel.download()) }
            .toModelDownloadProgress()
            .withAiErrors(AiOperation.DOWNLOAD)

    suspend fun prepare(onProgress: (ModelDownloadProgress) -> Unit) {
        prepareModel(::featureStatus) { downloadProgress().collect(onProgress) }
    }

    fun validateInput(prompt: String) = validateTextInput(
        text = prompt,
        maximumEstimatedTokens = AiInputLimits.MAX_SHORT_TEXT_TOKENS
    )

    fun send(prompt: String): Flow<AiOperationState<String>> = aiOperation(
        validate = { validateInput(prompt) },
        prepare = ::prepare
    ) { onPartialResult ->
        buildString {
            streamResponse(prompt).collect { chunk ->
                append(chunk)
                onPartialResult(chunk)
            }
        }.requireResult()
    }

    fun streamResponse(prompt: String): Flow<String> = flow {
        validateInput(prompt)
        requireAvailable(featureStatus())
        val request = GenerateContentRequest.Builder(TextPart(prompt))
            .apply { maxOutputTokens = AiInputLimits.MAX_CHAT_OUTPUT_TOKENS }
            .build()
        validatePromptTokens(request)
        emitAll(
            generativeModel.generateContentStream(request)
                .map { it.candidates.firstOrNull()?.text.orEmpty() }
        )
    }.requireResult().withAiErrors(AiOperation.GENERATION)

    private suspend fun validatePromptTokens(request: GenerateContentRequest) {
        val inputTokens = generativeModel.countTokens(request).totalTokens
        val maximumInputTokens = (
            generativeModel.getTokenLimit() - AiInputLimits.MAX_CHAT_OUTPUT_TOKENS
        ).coerceAtLeast(0)
        if (inputTokens > maximumInputTokens) {
            throw AiException.InvalidInput(
                problem = InputProblem.TEXT_TOO_LONG,
                maximumTokens = maximumInputTokens,
                actualTokens = inputTokens
            )
        }
    }

    suspend fun baseModelName(): String? = aiCall(AiOperation.STATUS_CHECK) {
        generativeModel.getBaseModelName()
    }

    override fun close() = generativeModel.close()
}
