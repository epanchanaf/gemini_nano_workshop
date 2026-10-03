package com.sdkecuador.gemininano.data.genai.repository

import android.content.Context
import android.content.pm.PackageManager
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.mapper.toRepositoryStatus
import com.sdkecuador.gemininano.data.genai.model.AiCoreRuntimeInfo
import com.sdkecuador.gemininano.data.genai.model.AiCoreRuntimeStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.guava.await

class AiCoreStatusRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clientFactory: GenAiClientFactory
) {
    suspend fun getRuntimeInfo(): AiCoreRuntimeInfo = aiCall(AiOperation.STATUS_CHECK) {
        val summarizer = clientFactory.createSummarizer()
        val imageDescriber = clientFactory.createImageDescriber()
        val generativeModel = clientFactory.createGenerativeModel()

        try {
            AiCoreRuntimeInfo(
                appVersion = context.packageManager.appVersion(context.packageName),
                aiCoreVersion = context.packageManager.appVersion(AI_CORE_PACKAGE),
                runtimes = listOf(
                    AiCoreRuntimeStatus(
                        "Gemini Nano",
                        "Modelo de generación en el dispositivo",
                        generativeModel.checkStatus().toRepositoryStatus()
                    ),
                    AiCoreRuntimeStatus(
                        "ML Kit: resumir",
                        "SDK GenAI 1.0.0-beta1",
                        summarizer.checkFeatureStatus().await().toRepositoryStatus()
                    ),
                    AiCoreRuntimeStatus(
                        "ML Kit: describir imágenes",
                        "SDK GenAI 1.0.0-beta1",
                        imageDescriber.checkFeatureStatus().await().toRepositoryStatus()
                    )
                )
            )
        } finally {
            generativeModel.close()
            summarizer.close()
            imageDescriber.close()
        }
    }
}

private const val AI_CORE_PACKAGE = "com.google.android.aicore"

@Suppress("DEPRECATION")
private fun PackageManager.appVersion(packageName: String): String =
    runCatching {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            ?: "Versión no disponible"
    }.getOrElse { "No instalado" }
