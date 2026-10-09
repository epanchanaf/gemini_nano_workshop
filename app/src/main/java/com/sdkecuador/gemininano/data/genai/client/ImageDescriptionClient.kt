package com.sdkecuador.gemininano.data.genai.client

import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import com.google.mlkit.genai.imagedescription.ImageDescriptionResult

/** Contrato pequeño para la API especializada de descripción de imágenes. */
interface ImageDescriptionClient : AutoCloseable {
    suspend fun checkFeatureStatus(): Int

    suspend fun downloadFeature(callback: DownloadCallback)

    suspend fun runInference(request: ImageDescriptionRequest): ImageDescriptionResult
}
