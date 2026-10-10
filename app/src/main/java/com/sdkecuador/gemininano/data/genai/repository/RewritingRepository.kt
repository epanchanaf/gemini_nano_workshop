package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.rewriting.Rewriter
import com.google.mlkit.genai.rewriting.RewritingRequest
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.mapper.toRepositoryStatus
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.model.ModelUnavailableReason
import com.sdkecuador.gemininano.data.genai.model.RewriteStyle
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.guava.await

@ViewModelScoped
class RewritingRepository @Inject constructor(
    private val clientFactory: GenAiClientFactory
) : AutoCloseable {
    private var rewriter: Rewriter? = null

    private fun client(style: RewriteStyle): Rewriter {
        rewriter?.close()
        return clientFactory.createRewriter(style).also { rewriter = it }
    }

    suspend fun featureStatus(style: RewriteStyle): AiCoreFeatureStatus = aiCall(AiOperation.STATUS_CHECK) {
        client(style).checkFeatureStatus().await().toRepositoryStatus()
    }

    suspend fun download(onProgress: (ModelDownloadProgress) -> Unit) {
        val currentClient = preparedClient()
        aiCall(AiOperation.DOWNLOAD) {
            currentClient.downloadFeature(modelDownloadCallback(onProgress)).await()
        }
    }

    suspend fun prepare(style: RewriteStyle, onProgress: (ModelDownloadProgress) -> Unit) {
        prepareModel({ featureStatus(style) }) { download(onProgress) }
    }

    fun validateInput(text: String) = validateTextInput(
        text = text,
        maximumEstimatedTokens = AiInputLimits.MAX_PROOFREADING_REWRITING_TOKENS
    )

    fun rewriteText(text: String, style: RewriteStyle): Flow<AiOperationState<String>> = aiOperation(
        validate = { validateInput(text) },
        prepare = { onDownloadProgress -> prepare(style, onDownloadProgress) },
        generate = { onPartialResult -> rewrite(text, onPartialResult) }
    )

    suspend fun rewrite(text: String, onToken: (String) -> Unit): String = aiCall(AiOperation.GENERATION) {
        validateInput(text)
        val currentClient = preparedClient()
        val status = aiCall(AiOperation.STATUS_CHECK) {
            currentClient.checkFeatureStatus().await().toRepositoryStatus()
        }
        requireAvailable(status)
        currentClient.runInference(RewritingRequest.builder(text).build(), onToken).await()
            .results.firstOrNull()?.text.orEmpty().requireResult()
    }

    private fun preparedClient(): Rewriter = rewriter
        ?: throw AiException.ModelUnavailable(ModelUnavailableReason.NOT_READY)

    override fun close() = rewriter?.close() ?: Unit
}
