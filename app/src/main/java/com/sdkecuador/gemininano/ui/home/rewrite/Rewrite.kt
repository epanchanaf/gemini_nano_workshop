package com.sdkecuador.gemininano.ui.home.rewrite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
fun RewriteScreen(
    onBack: () -> Unit,
    viewModel: RewriteViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    RewriteContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onRewriteTypeSelected = viewModel::onRewriteTypeSelected,
        onSubmit = viewModel::rewrite,
        onApplySuggestion = viewModel::applySuggestion,
        onBack = onBack
    )
}

@Composable
private fun RewriteContent(
    state: RewriteState,
    onInputChange: (String) -> Unit,
    onRewriteTypeSelected: (RewriteType) -> Unit,
    onSubmit: () -> Unit,
    onApplySuggestion: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { FeatureTopBar("Asistente de reescritura", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = state.input,
                onValueChange = { candidate ->
                    if (candidate.estimatedTokenCount() <= AiInputLimits.MAX_PROOFREADING_REWRITING_TOKENS) {
                        onInputChange(candidate)
                    }
                },
                label = { Text("Texto que deseas reescribir") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                enabled = !state.isLoading
            )

            InputLimitInfo(
                currentEstimatedTokens = state.input.estimatedTokenCount(),
                maximumTokens = AiInputLimits.MAX_PROOFREADING_REWRITING_TOKENS,
                description = "Límite máximo de ML Kit para reescritura",
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            RewriteOptions(
                selectedType = state.selectedRewriteType,
                onSelected = onRewriteTypeSelected
            )

            GenerationStatus(
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                downloadProgress = state.downloadProgress.takeIf { state.mbToDownload > 0 },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (state.suggestion.isNotBlank() && !state.isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resultado", style = MaterialTheme.typography.titleMedium)
                        Text(state.suggestion, modifier = Modifier.padding(top = 8.dp))
                        TextButton(onClick = onApplySuggestion) {
                            Text("Usar esta versión")
                        }
                    }
                }
            }

            Button(
                onClick = onSubmit,
                enabled = state.input.isNotBlank() && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Reescribir")
            }
        }
    }
}

@Composable
private fun RewriteOptions(
    selectedType: RewriteType,
    onSelected: (RewriteType) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(RewriteType.entries) { type ->
            FilterChip(
                selected = type == selectedType,
                onClick = { onSelected(type) },
                label = { Text(type.displayName) },
                leadingIcon = if (type == selectedType) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}
