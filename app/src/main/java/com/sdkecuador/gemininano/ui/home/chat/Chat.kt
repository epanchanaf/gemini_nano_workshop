package com.sdkecuador.gemininano.ui.home.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkecuador.gemininano.ui.components.FeatureTopBar
import com.sdkecuador.gemininano.ui.components.GenerationStatus
import com.sdkecuador.gemininano.ui.components.InputLimitInfo
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ChatContent(
        state = state,
        onSend = viewModel::onSend,
        onInputChange = viewModel::onInputChange,
        onBack = onBack
    )
}

@Composable
private fun ChatContent(
    state: ChatUiState,
    onSend: () -> Unit,
    onInputChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = { FeatureTopBar("Gemini Nano", onBack) },
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            MessageList(state.messages, Modifier.weight(1f))

            GenerationStatus(
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                downloadProgress = state.downloadProgress.takeIf { state.mbToDownload > 0 },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            MessageInput(
                value = state.input,
                onValueChange = onInputChange,
                onSend = onSend,
                enabled = !state.isLoading && state.mbToDownload == 0.0
            )
        }
    }
}

@Composable
private fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.message) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages) { message -> MessageBubble(message) }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val color = if (message.isFromUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (message.isFromUser) 64.dp else 0.dp,
                end = if (message.isFromUser) 0.dp else 64.dp
            ),
        contentAlignment = if (message.isFromUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = message.message,
            color = contentColorFor(color),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color)
                .padding(12.dp)
        )
    }
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    val canSend = enabled && value.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = { candidate ->
                    if (candidate.estimatedTokenCount() <= AiInputLimits.MAX_SHORT_TEXT_TOKENS) {
                        onValueChange(candidate)
                    }
                },
                placeholder = { Text("Escribe un mensaje...") },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() })
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { if (canSend) onSend() },
                containerColor = if (canSend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        }
        InputLimitInfo(
            currentEstimatedTokens = value.estimatedTokenCount(),
            maximumTokens = AiInputLimits.MAX_SHORT_TEXT_TOKENS,
            description = "Límite de demo; el modelo valida el total real antes de enviar"
        )
    }
}
