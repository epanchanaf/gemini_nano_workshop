package com.sdkecuador.gemininano.data.journal

import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import com.sdkecuador.gemininano.data.genai.client.GenAiClientFactory
import com.sdkecuador.gemininano.data.genai.mapper.toRepositoryStatus
import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.InputProblem
import com.sdkecuador.gemininano.data.genai.model.ModelDownloadProgress
import com.sdkecuador.gemininano.data.genai.repository.AiOperation
import com.sdkecuador.gemininano.data.genai.repository.aiCall
import com.sdkecuador.gemininano.data.genai.repository.prepareModel
import com.sdkecuador.gemininano.data.genai.repository.toModelDownloadProgress
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import org.json.JSONObject

@ViewModelScoped
class JournalAiRepository @Inject constructor(clientFactory: GenAiClientFactory) : AutoCloseable {
    private val model = clientFactory.createGenerativeModel()

    suspend fun prepare(onProgress: (ModelDownloadProgress) -> Unit) {
        prepareModel(
            statusProvider = { aiCall(AiOperation.STATUS_CHECK) { model.checkStatus().toRepositoryStatus() } },
            download = {
                flow { emitAll(model.download()) }.toModelDownloadProgress().collect(onProgress)
            }
        )
    }

    suspend fun interpret(spokenText: String, entries: List<JournalEntry>): JournalDecision {
        val entryList = entries.mapIndexed { index, entry ->
            val date = Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            "${index + 1}. $date · ${entry.title.take(48)} · ${entry.body.take(70)}"
        }.joinToString("\n")
        val prompt = """
            Eres el asistente local de un diario personal. Interpreta SOLO la orden hablada.
            CREATE: redacta una entrada fiel al relato, en primera persona; no inventes hechos.
            DELETE: elige targetIndex de la lista únicamente si la orden identifica una entrada sin ambigüedad. Si no, usa cero.
            THEME: cambia la paleta de toda la app. Bosque/naturaleza=FOREST, República Dominicana/bandera dominicana/Quisqueya=DOMINICAN_REPUBLIC, lluvia/día lluvioso/tormenta=RAIN, mar=OCEAN, atardecer=SUNSET, flores/lila=LAVENDER. Si no corresponde, DEFAULT.
            Para CREATE, usa CANSADO si expresa agotamiento; APRENDIZAJE si celebra conocimiento; elige otra emoción cuando corresponda.
            Fecha actual: ${LocalDate.now()}
            Entradas existentes (más reciente primero):
            ${entryList.ifBlank { "Ninguna" }}
            Orden hablada: ${spokenText.trim()}
        """.trimIndent()
        val request = GenerateContentRequest.Builder(TextPart(prompt)).apply {
            maxOutputTokens = 420
        }.build()
        val availableInputTokens = (model.getTokenLimit() - 420).coerceAtLeast(0)
        val inputTokens = model.countTokens(request).totalTokens
        if (inputTokens > availableInputTokens) {
            throw AiException.InvalidInput(
                problem = InputProblem.TEXT_TOO_LONG,
                maximumTokens = availableInputTokens,
                actualTokens = inputTokens
            )
        }
        if (model.isStructuredOutputFeatureAvailable()) {
            val typedRequest = generateTypedContentRequest(request, JournalDecision::class)
            return model.generateContent(typedRequest).candidates.firstOrNull()?.response
                ?: error("Gemini Nano no devolvió una instrucción válida.")
        }
        // Algunos dispositivos compatibles con Prompt API todavía no ofrecen Structured Output.
        val fallbackRequest = GenerateContentRequest.Builder(
            TextPart("$prompt\nDevuelve únicamente JSON con action, title, body, mood, targetIndex y palette.")
        ).apply { maxOutputTokens = 420 }.build()
        val raw = model.generateContent(fallbackRequest).candidates.firstOrNull()?.text.orEmpty()
        val first = raw.indexOf('{')
        val last = raw.lastIndexOf('}')
        if (first < 0 || last <= first) error("Gemini Nano no devolvió datos interpretables.")
        val json = JSONObject(raw.substring(first, last + 1))
        return JournalDecision(
            action = json.optString("action", "CREATE"),
            title = json.optString("title", ""),
            body = json.optString("body", ""),
            mood = json.optString("mood", "NEUTRO"),
            targetIndex = json.optInt("targetIndex", 0),
            palette = json.optString("palette", "DEFAULT")
        )
    }

    override fun close() = model.close()
}
