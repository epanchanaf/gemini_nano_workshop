package com.sdkecuador.gemininano.data.journal

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC, id DESC")
    fun observeEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries ORDER BY createdAt DESC, id DESC LIMIT 20")
    suspend fun recentEntries(): List<JournalEntry>

    @Query("SELECT * FROM journal_entries WHERE id = :id LIMIT 1")
    suspend fun findEntry(id: Long): JournalEntry?

    @Insert
    suspend fun insert(entry: JournalEntry): Long

    @Delete
    suspend fun delete(entry: JournalEntry)

    @Query("SELECT * FROM journal_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<JournalSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: JournalSettings)
}
