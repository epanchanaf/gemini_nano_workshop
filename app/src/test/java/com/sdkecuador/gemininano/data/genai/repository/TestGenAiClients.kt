package com.sdkecuador.gemininano.data.genai.repository

import com.google.mlkit.genai.imagedescription.ImageDescriber
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.proofreading.Proofreader
import com.google.mlkit.genai.rewriting.Rewriter
import com.google.mlkit.genai.summarization.Summarizer
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.model.RewriteStyle
import java.lang.reflect.Proxy

internal class PromptOnlyFactory(
    private val createModel: () -> GenerativeModel
) : GenAiClientFactory {
    override fun createGenerativeModel(): GenerativeModel = createModel()
    override fun createSummarizer(): Summarizer = error("No se esperaba un Summarizer")
    override fun createProofreader(): Proofreader = error("No se esperaba un Proofreader")
    override fun createRewriter(style: RewriteStyle): Rewriter = error("No se esperaba un Rewriter")
    override fun createImageDescriber(): ImageDescriber = error("No se esperaba un ImageDescriber")
}

// El SDK expone una interfaz extensa; este doble falla ante cualquier llamada no prevista.
internal fun modelStub(onCall: (String) -> Any?): GenerativeModel = Proxy.newProxyInstance(
    GenerativeModel::class.java.classLoader,
    arrayOf(GenerativeModel::class.java)
) { _, method, _ -> onCall(method.name) } as GenerativeModel
