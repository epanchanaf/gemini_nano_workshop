package com.sdkecuador.gemininano.data.journal

import java.text.Normalizer
import java.util.Locale

/** Usa un índice válido de Nano o una coincidencia inequívoca con el texto de la orden. */
fun resolveDeleteTarget(targetIndex: Int, spokenText: String, entries: List<JournalEntry>): JournalEntry? {
    if (targetIndex in 1..entries.size) return entries[targetIndex - 1]
    if (entries.size == 1) return entries.first()
    val command = spokenText.normalizedForMatching()
    val matching = entries.filter { entry ->
        val title = entry.title.normalizedForMatching()
        title.length >= 5 && command.contains(title)
    }
    return matching.singleOrNull()
}

private fun String.normalizedForMatching(): String = Normalizer
    .normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .replace(Regex("[^a-z0-9 ]"), " ")
    .replace(Regex(" +"), " ")
    .trim()
