package com.sdkecuador.gemininano.data.genai.client

import com.google.mlkit.genai.imagedescription.ImageDescriber
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.proofreading.Proofreader
import com.google.mlkit.genai.rewriting.Rewriter
import com.google.mlkit.genai.summarization.Summarizer
import com.sdkecuador.gemininano.data.genai.model.RewriteStyle

/**
 * Crea clientes sin compartirlos ni almacenarlos en caché.
 * Quien solicita un cliente es responsable de cerrarlo al terminar de usarlo.
 */
interface GenAiClientFactory {
    fun createGenerativeModel(): GenerativeModel

    fun createSummarizer(): Summarizer

    fun createProofreader(): Proofreader

    fun createRewriter(style: RewriteStyle): Rewriter

    fun createImageDescriber(): ImageDescriber
}
