package com.sdkecuador.gemininano.ui.home.summarization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkecuador.gemininano.ui.components.FeatureTopBar
import com.sdkecuador.gemininano.ui.components.GenerationStatus
import com.sdkecuador.gemininano.ui.components.InputLimitInfo
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummarizationScreen(
    onBack: () -> Unit,
    viewModel: SummarizationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current

    if (state.summary.isNotBlank()) {
        ModalBottomSheet(
            onDismissRequest = viewModel::clearSummary,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Resumen", style = MaterialTheme.typography.headlineSmall)
                Text(state.summary, style = MaterialTheme.typography.bodyLarge)
                Button(
                    onClick = viewModel::clearSummary,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }

    SummarizationContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onPaste = {
            val clip = clipboard.nativeClipboard.primaryClip
            val text = if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0).text?.toString().orEmpty()
            } else {
                ""
            }
            viewModel.onInputChange(text)
        },
        onSummarize = viewModel::summarize,
        onBack = onBack
    )
}

@Composable
private fun SummarizationContent(
    state: SummarizationState,
    onInputChange: (String) -> Unit,
    onPaste: () -> Unit,
    onSummarize: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { FeatureTopBar("Resumen de texto", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Escribe o pega un artículo de al menos 400 caracteres y hasta 4 000 tokens estimados:",
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = state.input,
                onValueChange = { candidate ->
                    if (candidate.estimatedTokenCount() <= AiInputLimits.MAX_SUMMARY_TOKENS) {
                        onInputChange(candidate)
                    }
                },
                placeholder = { Text("Pega o escribe el artículo aquí...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                enabled = !state.isLoading && !state.isDownloadingModel,
                isError = state.errorMessage != null,
                trailingIcon = if (state.input.isNotBlank() && !state.isLoading) {
                    {
                        IconButton(onClick = { onInputChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar texto")
                        }
                    }
                } else {
                    null
                }
            )

            InputLimitInfo(
                currentEstimatedTokens = state.input.estimatedTokenCount(),
                maximumTokens = AiInputLimits.MAX_SUMMARY_TOKENS,
                description = "ML Kit requiere al menos 400 caracteres"
            )

            GenerationStatus(
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                downloadProgress = state.downloadProgress.takeIf { state.isDownloadingModel }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPaste,
                    enabled = !state.isLoading && !state.isDownloadingModel
                ) {
                    Text("Pegar")
                }
                Button(
                    onClick = onSummarize,
                    enabled = state.input.trim().length >= AiInputLimits.MIN_SUMMARY_CHARACTERS &&
                        !state.isLoading &&
                        !state.isDownloadingModel
                ) {
                    Text(state.actionButtonLabel)
                }
            }
        }
    }
}
