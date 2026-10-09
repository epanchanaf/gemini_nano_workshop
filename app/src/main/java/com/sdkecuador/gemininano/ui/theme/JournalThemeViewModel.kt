package com.sdkecuador.gemininano.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdkecuador.gemininano.data.journal.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class JournalThemeViewModel @Inject constructor(journal: JournalRepository) : ViewModel() {
    val palette = journal.palette.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "DEFAULT")
}
