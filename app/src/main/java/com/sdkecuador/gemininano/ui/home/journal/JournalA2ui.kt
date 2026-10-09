package com.sdkecuador.gemininano.ui.home.journal

import androidx.a2ui.compose.runtime.A2uiMessageParser
import androidx.a2ui.compose.ui.A2uiMessageProcessor
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.processor.processInput
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.a2ui.A2uiSurface
import androidx.compose.material3.a2ui.catalog.MaterialA2uiBasicCatalogV1Defaults
import androidx.compose.material3.a2ui.catalog.materialA2uiBasicCatalogV1
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkecuador.gemininano.data.journal.JournalEntry
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart
import org.json.JSONArray
import org.json.JSONObject

/** Emite mensajes A2UI v0.9.1 desde datos ya validados; nunca ejecuta UI generada como código. */
internal fun entrySurfaceMessages(entry: JournalEntry): List<String> {
    val id = "journal_${entry.id}"
    // AndroidX alpha01 registra el catálogo Basic con este identificador v0_9.
    val catalog = "https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json"
    val create = JSONObject().put("version", "v0.9.1").put(
        "createSurface",
        JSONObject().put("surfaceId", id).put("catalogId", catalog)
    )
    val components = JSONArray()
        .put(JSONObject().put("id", "root").put("component", "Column")
            .put("children", JSONArray().put("title").put("date").put("body")))
        .put(JSONObject().put("id", "title").put("component", "Text")
            .put("text", entry.title).put("variant", "h4"))
        .put(JSONObject().put("id", "date").put("component", "Text")
            .put("text", DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.createdAt))))
        .put(JSONObject().put("id", "body").put("component", "Text")
            .put("text", entry.body))
    val update = JSONObject().put("version", "v0.9.1").put(
        "updateComponents", JSONObject().put("surfaceId", id).put("components", components)
    )
    return listOf(create.toString(), update.toString())
}

@Composable
fun JournalA2uiContent(entry: JournalEntry, modifier: Modifier = Modifier) {
    val catalog = remember {
        materialA2uiBasicCatalogV1(
            image = MaterialA2uiBasicCatalogV1Defaults.image { _, _, _, _, _ -> },
            video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
            audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
            urlOpener = {},
            messageFormatter = { pattern, _, _ -> pattern },
            localeProvider = A2uiLocaleProvider.Default
        )
    }
    val parser = remember { A2uiMessageParser() }
    val processor = remember(entry.id) { A2uiMessageProcessor(listOf(catalog)) }
    val surfaces by processor.activeSurfaces.collectAsStateWithLifecycle()

    LaunchedEffect(processor, entry) {
        launch(start = CoroutineStart.UNDISPATCHED) { processor.collectMessages() }
        entrySurfaceMessages(entry).forEach { processor.processInput(parser, it) }
    }
    Column(modifier.fillMaxWidth()) {
        if (surfaces.isEmpty()) Text("Preparando entrada…")
        surfaces.forEach { surface ->
            key(surface.id) { A2uiSurface(surfaceModel = surface) }
        }
    }
}
