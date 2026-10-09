package com.sdkecuador.gemininano.data.genai.repository

import android.graphics.Bitmap
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
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

@ViewModelScoped
class ImageDescriptionRepository @Inject constructor(
    clientFactory: GenAiClientFactory
) : AutoCloseable {
    private val imageDescriber = clientFactory.createImageDescriber()

    suspend fun featureStatus(): AiCoreFeatureStatus = aiCall(AiOperation.STATUS_CHECK) {
        imageDescriber.checkFeatureStatus().toRepositoryStatus()
    }

    suspend fun download(onProgress: (ModelDownloadProgress) -> Unit) {
        aiCall(AiOperation.DOWNLOAD) {
            imageDescriber.downloadFeature(modelDownloadCallback(onProgress))
        }
    }

    suspend fun prepare(onProgress: (ModelDownloadProgress) -> Unit) {
        prepareModel(::featureStatus) { download(onProgress) }
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
        prepare = ::prepare,
        generate = { describe(bitmap) }
    )

    suspend fun describe(bitmap: Bitmap): String = aiCall(AiOperation.GENERATION) {
        validateInput(bitmap)
        requireAvailable(featureStatus())
        imageDescriber.runInference(ImageDescriptionRequest.builder(bitmap).build())
            .description.requireResult()
    }

    override fun close() = imageDescriber.close()
}
