package com.sdkecuador.gemininano.data.journal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val spokenText: String,
    val title: String,
    val body: String,
    val mood: String
)

@Entity(tableName = "journal_settings")
data class JournalSettings(
    @PrimaryKey val id: Int = 1,
    val palette: String = "DEFAULT"
)
