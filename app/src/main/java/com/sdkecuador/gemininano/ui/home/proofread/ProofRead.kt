package com.sdkecuador.gemininano.ui.home.proofread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkecuador.gemininano.ui.components.FeatureTopBar
import com.sdkecuador.gemininano.ui.components.GenerationStatus
import com.sdkecuador.gemininano.ui.components.InputLimitInfo
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount

@Composable
fun ProofReadingScreen(
    onBack: () -> Unit,
    viewModel: ProofReadingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ProofReadingContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSubmit = viewModel::submitProofreading,
        onApplySuggestion = viewModel::applySuggestion,
        onBack = onBack
    )
}

@Composable
private fun ProofReadingContent(
    state: ProofReadingState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onApplySuggestion: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { FeatureTopBar("Asistente de corrección", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = { candidate ->
                    if (candidate.estimatedTokenCount() <= AiInputLimits.MAX_PROOFREADING_REWRITING_TOKENS) {
                        onInputChange(candidate)
                    }
                },
                label = { Text("Texto que deseas corregir") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                enabled = !state.isLoading
            )

            InputLimitInfo(
                currentEstimatedTokens = state.input.estimatedTokenCount(),
                maximumTokens = AiInputLimits.MAX_PROOFREADING_REWRITING_TOKENS,
                description = "Límite máximo de ML Kit para corrección"
            )

            GenerationStatus(
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                downloadProgress = state.downloadProgress.takeIf { state.mbToDownload > 0 }
            )

            if (state.suggestion.isNotBlank() && !state.isLoading) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Corrección", style = MaterialTheme.typography.titleMedium)
                        Text(state.suggestion, modifier = Modifier.padding(top = 8.dp))
                        TextButton(onClick = onApplySuggestion) {
                            Text("Usar corrección")
                        }
                    }
                }
            }

            Button(
                onClick = onSubmit,
                enabled = state.input.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Corregir")
            }
        }
    }
}
