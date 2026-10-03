package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.proofreading.ProofreadingRequest
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
class ProofreadingRepository @Inject constructor(
    clientFactory: GenAiClientFactory
) : AutoCloseable {
    private val proofreader = clientFactory.createProofreader()

    suspend fun featureStatus(): AiCoreFeatureStatus = aiCall(AiOperation.STATUS_CHECK) {
        proofreader.checkFeatureStatus().await().toRepositoryStatus()
    }

    suspend fun download(onProgress: (ModelDownloadProgress) -> Unit) {
        aiCall(AiOperation.DOWNLOAD) {
            proofreader.downloadFeature(modelDownloadCallback(onProgress)).await()
        }
    }

    suspend fun prepare(onProgress: (ModelDownloadProgress) -> Unit) {
        prepareModel(::featureStatus) { download(onProgress) }
    }

    fun validateInput(text: String) = validateTextInput(
        text = text,
        maximumEstimatedTokens = AiInputLimits.MAX_SHORT_TEXT_TOKENS
    )

    fun correct(text: String): Flow<AiOperationState<String>> = aiOperation(
        validate = { validateInput(text) },
        prepare = ::prepare,
        generate = { onPartialResult -> proofread(text, onPartialResult) }
    )

    suspend fun proofread(text: String, onToken: (String) -> Unit): String = aiCall(AiOperation.GENERATION) {
        validateInput(text)
        requireAvailable(featureStatus())
        proofreader.runInference(ProofreadingRequest.builder(text).build(), onToken).await()
            .results.firstOrNull()?.text.orEmpty().requireResult()
    }

    override fun close() = proofreader.close()
}
