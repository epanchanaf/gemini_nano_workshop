package com.sdkecuador.gemininano.data.genai.client

import android.content.Context
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions
import com.google.mlkit.genai.imagedescription.ImageDescription
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.proofreading.Proofreader
import com.google.mlkit.genai.proofreading.ProofreaderOptions
import com.google.mlkit.genai.proofreading.Proofreading
import com.google.mlkit.genai.rewriting.Rewriter
import com.google.mlkit.genai.rewriting.RewriterOptions
import com.google.mlkit.genai.rewriting.Rewriting
import com.google.mlkit.genai.summarization.Summarization
import com.google.mlkit.genai.summarization.Summarizer
import com.google.mlkit.genai.summarization.SummarizerOptions
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.sdkecuador.gemininano.data.genai.model.RewriteStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MlKitGenAiClientFactory @Inject constructor(
    @param:ApplicationContext private val context: Context
) : GenAiClientFactory {
    override fun createGenerativeModel(): GenerativeModel = Generation.getClient()

    override fun createSummarizer(): Summarizer = Summarization.getClient(
        SummarizerOptions.builder(context)
            .setInputType(SummarizerOptions.InputType.ARTICLE)
            .setOutputType(SummarizerOptions.OutputType.THREE_BULLETS)
            .setLanguage(SummarizerOptions.Language.ENGLISH)
            .build()
    )

    override fun createProofreader(): Proofreader = Proofreading.getClient(
        ProofreaderOptions.builder(context)
            .setInputType(ProofreaderOptions.InputType.KEYBOARD)
            .setLanguage(ProofreaderOptions.Language.SPANISH)
            .build()
    )

    override fun createRewriter(style: RewriteStyle): Rewriter = Rewriting.getClient(
        RewriterOptions.builder(context)
            .setOutputType(style.toMlKitOutputType())
            .setLanguage(RewriterOptions.Language.SPANISH)
            .build()
    )

    override fun createImageDescriber(): ImageDescriptionClient = MlKitImageDescriptionClient(
        ImageDescription.getClient(ImageDescriberOptions.builder(context).build())
    )

    override fun createSpeechRecognizer(options: SpeechRecognizerOptions): SpeechRecognizer =
        SpeechRecognition.getClient(options)
}

private fun RewriteStyle.toMlKitOutputType(): Int = when (this) {
    RewriteStyle.REPHRASE -> RewriterOptions.OutputType.REPHRASE
    RewriteStyle.SHORTEN -> RewriterOptions.OutputType.SHORTEN
    RewriteStyle.ELABORATE -> RewriterOptions.OutputType.ELABORATE
    RewriteStyle.FRIENDLY -> RewriterOptions.OutputType.FRIENDLY
    RewriteStyle.PROFESSIONAL -> RewriterOptions.OutputType.PROFESSIONAL
}
