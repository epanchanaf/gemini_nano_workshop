package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.common.DownloadCallback
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest
import com.google.mlkit.genai.imagedescription.ImageDescriptionResult
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.proofreading.Proofreader
import com.google.mlkit.genai.rewriting.Rewriter
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.client.ImageDescriptionClient
import com.sdkecuador.gemininano.data.genai.model.RewriteStyle
import java.lang.reflect.Proxy

internal class PromptOnlyFactory(
    private val createModel: () -> GenerativeModel
) : GenAiClientFactory {
    override fun createGenerativeModel(): GenerativeModel = createModel()
    override fun createSummarizer(): Summarizer = error("No se esperaba un Summarizer")
    override fun createProofreader(): Proofreader = error("No se esperaba un Proofreader")
    override fun createRewriter(style: RewriteStyle): Rewriter = error("No se esperaba un Rewriter")
    override fun createImageDescriber(): ImageDescriptionClient =
        error("No se esperaba un ImageDescriptionClient")
    override fun createSpeechRecognizer(options: SpeechRecognizerOptions): SpeechRecognizer =
        error("No se esperaba un SpeechRecognizer")
}

internal class ImageOnlyFactory(
    private val createDescriber: () -> ImageDescriptionClient
) : GenAiClientFactory {
    override fun createGenerativeModel(): GenerativeModel = error("No se esperaba un GenerativeModel")
    override fun createSummarizer(): Summarizer = error("No se esperaba un Summarizer")
    override fun createProofreader(): Proofreader = error("No se esperaba un Proofreader")
    override fun createRewriter(style: RewriteStyle): Rewriter = error("No se esperaba un Rewriter")
    override fun createImageDescriber(): ImageDescriptionClient = createDescriber()
    override fun createSpeechRecognizer(options: SpeechRecognizerOptions): SpeechRecognizer =
        error("No se esperaba un SpeechRecognizer")
}

// El SDK expone una interfaz extensa; este doble falla ante cualquier llamada no prevista.
internal fun modelStub(onCall: (String) -> Any?): GenerativeModel = Proxy.newProxyInstance(
    GenerativeModel::class.java.classLoader,
    arrayOf(GenerativeModel::class.java)
) { _, method, _ -> onCall(method.name) } as GenerativeModel

internal fun imageDescriberStub(onCall: (String) -> Any?): ImageDescriptionClient =
    object : ImageDescriptionClient {
        override suspend fun checkFeatureStatus(): Int = onCall("checkFeatureStatus") as Int

        override suspend fun downloadFeature(callback: DownloadCallback) {
            onCall("downloadFeature")
        }

        override suspend fun runInference(request: ImageDescriptionRequest): ImageDescriptionResult =
            onCall("runInference") as ImageDescriptionResult

        override fun close() {
            onCall("close")
        }
    }
