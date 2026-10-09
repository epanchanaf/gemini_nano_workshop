package com.sdkecuador.gemininano.data.genai.client

import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import com.google.mlkit.genai.imagedescription.ImageDescriptionResult
import com.google.mlkit.genai.imagedescription.ImageDescriber
import kotlinx.coroutines.guava.await

internal class MlKitImageDescriptionClient(
    private val imageDescriber: ImageDescriber
) : ImageDescriptionClient {
    override suspend fun checkFeatureStatus(): Int = imageDescriber.checkFeatureStatus().await()

    override suspend fun downloadFeature(callback: DownloadCallback) {
        imageDescriber.downloadFeature(callback).await()
    }

    override suspend fun runInference(request: ImageDescriptionRequest): ImageDescriptionResult =
        imageDescriber.runInference(request).await()

    override fun close() = imageDescriber.close()
}
