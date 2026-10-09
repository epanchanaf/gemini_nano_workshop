package com.sdkecuador.gemininano.data.journal

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JournalRepository @Inject constructor(private val dao: JournalDao) {
    val entries: Flow<List<JournalEntry>> = dao.observeEntries()
    val palette: Flow<String> = dao.observeSettings().map { it?.palette ?: "DEFAULT" }

    suspend fun recentEntries(): List<JournalEntry> = dao.recentEntries()

    suspend fun add(spokenText: String, title: String, body: String, mood: String): Long =
        dao.insert(JournalEntry(spokenText = spokenText, title = title, body = body, mood = mood))

    suspend fun delete(id: Long): Boolean {
        val entry = dao.findEntry(id) ?: return false
        dao.delete(entry)
        return true
    }

    suspend fun changePalette(palette: String) = dao.saveSettings(JournalSettings(palette = palette))
}
