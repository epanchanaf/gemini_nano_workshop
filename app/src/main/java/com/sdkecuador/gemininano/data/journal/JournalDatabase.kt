package com.sdkecuador.gemininano.data.journal

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [JournalEntry::class, JournalSettings::class], version = 1, exportSchema = false)
abstract class JournalDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
}
