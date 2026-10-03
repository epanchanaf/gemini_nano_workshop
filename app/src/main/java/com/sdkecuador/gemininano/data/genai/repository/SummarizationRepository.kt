package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.summarization.SummarizationRequest
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.mapper.toRepositoryStatus
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.guava.await

@ViewModelScoped
class SummarizationRepository @Inject constructor(
    clientFactory: GenAiClientFactory
) : AutoCloseable {
    private val summarizer = clientFactory.createSummarizer()
    private val generativeModel = clientFactory.createGenerativeModel()

    suspend fun featureStatus(): AiCoreFeatureStatus = aiCall(AiOperation.STATUS_CHECK) {
        summarizer.checkFeatureStatus().await().toRepositoryStatus()
    }

    suspend fun download(onProgress: (ModelDownloadProgress) -> Unit) {
        aiCall(AiOperation.DOWNLOAD) {
            summarizer.downloadFeature(modelDownloadCallback(onProgress)).await()
        }
    }

    suspend fun prepare(onProgress: (ModelDownloadProgress) -> Unit) {
        prepareModel(::featureStatus) { download(onProgress) }
    }

    fun validateInput(text: String) = validateTextInput(
        text = text,
        minimumCharacters = AiInputLimits.MIN_SUMMARY_CHARACTERS,
        maximumEstimatedTokens = AiInputLimits.MAX_SUMMARY_TOKENS
    )

    fun summarizeText(text: String): Flow<AiOperationState<String>> = aiOperation(
        validate = { validateInput(text) },
        prepare = ::prepare,
        generate = { onPartialResult -> summarize(text, onPartialResult) }
    )

    suspend fun summarize(text: String, onToken: (String) -> Unit): String = aiCall(AiOperation.GENERATION) {
        validateInput(text)
        requireAvailable(featureStatus())
        val summary = try {
            summarizer.runInference(SummarizationRequest.builder(text).build(), onToken).await().summary
        } catch (error: GenAiException) {
            if (error.errorCode == GenAiException.ErrorCode.RESPONSE_PROCESSING_ERROR) {
                summarizeWithPrompt(text)
            } else {
                throw error
            }
        }
        summary.requireResult()
    }

    private suspend fun summarizeWithPrompt(text: String): String {
        val status = aiCall(AiOperation.STATUS_CHECK) {
            generativeModel.checkStatus().toRepositoryStatus()
        }
        requireAvailable(status)
        return generativeModel.generateContent(
            GenerateContentRequest.Builder(
                TextPart("Resume el siguiente texto en tres viñetas concisas. Devuelve solo el resumen.\n\n$text")
            ).build()
        ).candidates.firstOrNull()?.text.orEmpty()
    }

    override fun close() {
        summarizer.close()
        generativeModel.close()
    }
}
