package com.sdkecuador.gemininano.data.journal

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JournalModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): JournalDatabase =
        Room.databaseBuilder(context, JournalDatabase::class.java, "personal_journal.db").build()

    @Provides
    fun dao(database: JournalDatabase): JournalDao = database.journalDao()
}
