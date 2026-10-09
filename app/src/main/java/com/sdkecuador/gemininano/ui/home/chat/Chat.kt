package com.sdkecuador.gemininano.ui.home.chat

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkecuador.gemininano.ui.components.FeatureTopBar
import com.sdkecuador.gemininano.ui.components.GenerationStatus
import com.sdkecuador.gemininano.ui.components.InputLimitInfo
import com.sdkecuador.gemininano.data.genai.model.AiInputLimits
import com.sdkecuador.gemininano.data.genai.model.estimatedTokenCount
import com.sdkecuador.gemininano.R
import com.sdkecuador.gemininano.ui.components.decodeAiImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    // Paso 3. La UI observa el estado que actualizó el ViewModel y se recompone con el borrador.
    // Siguiente paso al enviar: ChatViewModel.kt -> ChatViewModel.onSend() (paso 4).
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) { decodeAiImage(context, uri) }
                    viewModel.onImageSelected(bitmap)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    viewModel.onImageSelectionFailed()
                }
            }
        }
    }

    ChatContent(
        state = state,
        onSend = viewModel::onSend,
        onInputChange = viewModel::onInputChange,
        onPickImage = { imagePicker.launch("image/*") },
        onRemoveImage = viewModel::onRemoveImage,
        onBack = onBack
    )
}

/**
 * Paso 7. Representa el estado de preparación, descarga o generación en los mensajes y controles.
 * Siguiente paso: `ChatRepository.kt -> ChatRepository.streamResponse()` (paso 8).
 */
@Composable
private fun ChatContent(
    state: ChatUiState,
    onSend: () -> Unit,
    onInputChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
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

            // Paso 12. Muestra el resultado o el error y reactiva este campo cuando la operación termina.
            // Siguiente paso: MessageInput() en este archivo recibe otro mensaje (paso 1).
            MessageInput(
                value = state.input,
                image = state.pendingImage,
                onValueChange = onInputChange,
                onSend = onSend,
                onPickImage = onPickImage,
                onRemoveImage = onRemoveImage,
                enabled = !state.isLoading && state.mbToDownload == 0.0
            )
        }
    }
}

@Composable
private fun MessageList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()

    // Paso 10. La lista muestra cada fragmento nuevo y se desplaza al último mensaje.
    // Siguiente paso mientras llega texto: ChatRepository.kt -> streamResponse() (paso 8).
    // Siguiente paso al finalizar: ChatViewModel.kt -> handleOperation() (paso 11).
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
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(color)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            message.image?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Imagen adjunta",
                    modifier = Modifier.size(width = 180.dp, height = 160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            if (message.message.isNotBlank()) {
                Text(text = message.message, color = contentColorFor(color))
            }
        }
    }
}

/**
 * Paso 1. Recibe lo que escribe la persona, aplica el límite estimado y delega el cambio.
 * Siguiente paso: `ChatViewModel.kt -> ChatViewModel.onInputChange()` (paso 2).
 */
@Composable
private fun MessageInput(
    value: String,
    image: Bitmap?,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    enabled: Boolean
) {
    val canSend = enabled && (value.isNotBlank() || image != null)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        image?.let { bitmap ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Imagen lista para enviar",
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(onClick = onRemoveImage, enabled = enabled) {
                    Icon(Icons.Default.Close, contentDescription = "Quitar imagen")
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPickImage, enabled = enabled) {
                Icon(painterResource(R.drawable.ic_image), contentDescription = "Adjuntar imagen")
            }
            OutlinedTextField(
                value = value,
                onValueChange = { candidate ->
                    if (candidate.estimatedTokenCount() <= AiInputLimits.MAX_SHORT_TEXT_TOKENS) {
                        onValueChange(candidate)
                    }
                },
                placeholder = { Text(if (image == null) "Escribe un mensaje..." else "Pregunta sobre la imagen...") },
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
