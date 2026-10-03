package com.sdkecuador.gemininano.data.genai.repository

import android.graphics.Bitmap
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.mapper.toRepositoryStatus
import com.sdkecuador.gemininano.data.genai.model.AiCoreFeatureStatus
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.AiOperationState
import com.sdkecuador.gemininano.data.genai.model.InputProblem
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.model.isSupportedImageSize
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

@ViewModelScoped
class ImageDescriptionRepository @Inject constructor(
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

    fun validateInput(bitmap: Bitmap) {
        if (
            bitmap.isRecycled ||
                !isSupportedImageSize(bitmap.width, bitmap.height)
        ) {
            throw AiException.InvalidInput(InputProblem.INVALID_IMAGE)
        }
    }

    fun describeImage(bitmap: Bitmap): Flow<AiOperationState<String>> = aiOperation(
        validate = { validateInput(bitmap) },
        prepare = ::prepare
    ) { describe(bitmap) }

    suspend fun describe(bitmap: Bitmap): String = aiCall(AiOperation.GENERATION) {
        validateInput(bitmap)
        requireAvailable(featureStatus())
        generativeModel.generateContent(
            GenerateContentRequest.Builder(
                ImagePart(bitmap),
                TextPart("Describe esta imagen en una oración breve y objetiva.")
            ).build()
        ).candidates.firstOrNull()?.text.orEmpty().requireResult()
    }

    override fun close() = generativeModel.close()
}
