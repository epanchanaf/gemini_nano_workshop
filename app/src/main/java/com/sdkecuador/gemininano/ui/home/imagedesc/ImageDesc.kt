package com.sdkecuador.gemininano.ui.home.imagedesc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkecuador.gemininano.ui.components.FeatureTopBar

@Composable
fun ImageDescScreen(
    onBack: () -> Unit,
    viewModel: ImageDescViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { FeatureTopBar("Descripción de imágenes", onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ImagePickerScreen(onImagePicked = viewModel::describe)

            ImageDescContent(
                modifier = Modifier.padding(16.dp),
                description = state.description,
                errorMessage = state.errorMessage,
                isLoading = state.isLoading,
                downloadProgress = state.downloadProgress.takeIf {
                    state.isLoading && state.mbToDownload > 0
                },
                onClear = viewModel::clearDescription
            )
        }
    }
}

@Composable
private fun ImageDescContent(
    modifier: Modifier = Modifier,
    description: String,
    errorMessage: String?,
    isLoading: Boolean,
    downloadProgress: String?,
    onClear: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (!downloadProgress.isNullOrBlank()) {
            Text(
                text = downloadProgress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (isLoading && description.isBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (description.isNotBlank()) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Start
            )

            Button(
                onClick = onClear,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Limpiar")
            }
        }
    }
}
