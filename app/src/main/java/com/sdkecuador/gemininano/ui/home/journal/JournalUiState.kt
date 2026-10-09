package com.sdkecuador.gemininano.ui.home.journal

import com.sdkecuador.gemininano.data.journal.JournalEntry

data class JournalUiState(
    val entries: List<JournalEntry> = emptyList(),
    val transcript: String = "",
    val isListening: Boolean = false,
    val isPreparing: Boolean = false,
    val isProcessing: Boolean = false,
    val pendingDelete: JournalEntry? = null,
    val message: String? = null,
    val error: String? = null
)
