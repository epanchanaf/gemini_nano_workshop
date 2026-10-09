package com.sdkecuador.gemininano.ui.home.journal

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkecuador.gemininano.data.journal.JournalEntry
import com.sdkecuador.gemininano.ui.components.FeatureTopBar

@Composable
fun JournalScreen(onBack: () -> Unit, viewModel: JournalViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) viewModel.startListening() else viewModel.microphonePermissionDenied()
    }
    val onRecord = {
        if (state.isListening || state.isPreparing) viewModel.stopListening()
        else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startListening()
        } else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    Scaffold(topBar = { FeatureTopBar("Diario por voz", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mi diario", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Dicta una vivencia, pide eliminar una entrada o cambia el color de la app. El procesamiento y las entradas permanecen en este dispositivo.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = state.transcript,
                                onValueChange = viewModel::onTranscriptChange,
                                label = { Text("Dictado") },
                                placeholder = { Text("Tu dictado aparecerá aquí…") },
                                enabled = !state.isListening && !state.isProcessing,
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            Button(onClick = onRecord, enabled = !state.isProcessing, modifier = Modifier.fillMaxWidth()) {
                                Text(if (state.isPreparing) "Preparando micrófono…" else if (state.isListening) "Detener y procesar" else "Iniciar dictado")
                            }
                            if (state.transcript.isNotBlank() && !state.isListening && !state.isProcessing) {
                                OutlinedButton(onClick = viewModel::processTranscript, modifier = Modifier.fillMaxWidth()) {
                                    Text("Procesar dictado")
                                }
                            }
                        }
                    }
                    if (state.isProcessing) Text("Procesando con Gemini Nano…")
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            item { Text("Entradas (${state.entries.size})", style = MaterialTheme.typography.titleLarge) }
            if (state.entries.isEmpty()) item { Text("Aún no tienes entradas. Empieza contando cómo fue tu día.") }
            items(state.entries, key = { it.id }) { entry ->
                JournalEntryCard(entry = entry, onDelete = { viewModel.requestDelete(entry) })
            }
        }
    }

    state.pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Eliminar entrada") },
            text = { Text("¿Eliminar «${entry.title}» del diario?") },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = viewModel::cancelDelete) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun JournalEntryCard(entry: JournalEntry, onDelete: () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val background = moodColor(entry.mood, dark)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            JournalA2uiContent(entry)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(moodLabel(entry.mood), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = onDelete) { Text("Eliminar") }
            }
        }
    }
}

internal fun moodColor(mood: String, dark: Boolean): Color = when (mood) {
    "CANSADO" -> if (dark) Color(0xFF384654) else Color(0xFFE0E8F0)
    "ALEGRE" -> if (dark) Color(0xFF5B4520) else Color(0xFFFFE8A3)
    "APRENDIZAJE" -> if (dark) Color(0xFF2B5138) else Color(0xFFD9F2DC)
    "TRANQUILO" -> if (dark) Color(0xFF264E56) else Color(0xFFD8F1F0)
    "TRISTE" -> if (dark) Color(0xFF424062) else Color(0xFFE7E3F4)
    else -> if (dark) Color(0xFF3C3C3C) else Color(0xFFF1EFE9)
}

internal fun moodLabel(mood: String): String = when (mood) {
    "CANSADO" -> "Cansancio"
    "ALEGRE" -> "Alegría"
    "APRENDIZAJE" -> "Aprendizaje"
    "TRANQUILO" -> "Tranquilidad"
    "TRISTE" -> "Tristeza"
    else -> "Cotidiano"
}
