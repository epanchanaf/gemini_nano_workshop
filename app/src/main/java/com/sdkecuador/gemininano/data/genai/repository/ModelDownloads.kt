package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.GenAiException
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Traduce los eventos de Generation al progreso que muestran las pantallas. */
internal fun Flow<DownloadStatus>.toModelDownloadProgress(): Flow<ModelDownloadProgress> =
    map { status ->
        when (status) {
            is DownloadStatus.DownloadStarted ->
                ModelDownloadProgress(bytesToDownload = status.bytesToDownload)
            is DownloadStatus.DownloadProgress ->
                ModelDownloadProgress(bytesDownloaded = status.totalBytesDownloaded)
            DownloadStatus.DownloadCompleted -> ModelDownloadProgress()
            is DownloadStatus.DownloadFailed -> throw status.e
        }
    }

/** Adapta el progreso de las APIs por capacidad; el repositorio espera el resultado con await(). */
internal fun modelDownloadCallback(
    onProgress: (ModelDownloadProgress) -> Unit
): DownloadCallback = object : DownloadCallback {
    override fun onDownloadStarted(bytesToDownload: Long) {
        onProgress(ModelDownloadProgress(bytesToDownload = bytesToDownload))
    }

    override fun onDownloadProgress(totalBytesDownloaded: Long) {
        onProgress(ModelDownloadProgress(bytesDownloaded = totalBytesDownloaded))
    }

    override fun onDownloadFailed(e: GenAiException) = Unit

    override fun onDownloadCompleted() = Unit
}
