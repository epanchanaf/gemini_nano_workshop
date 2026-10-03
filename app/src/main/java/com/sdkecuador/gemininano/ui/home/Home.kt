package com.sdkecuador.gemininano.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdkecuador.gemininano.ui.nav.Feature

@Composable
fun HomeScreen(
    onNavigate: (Feature) -> Unit,
    viewModel: ModelStatusViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Demostraciones de IA generativa con Gemini Nano",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            item { RuntimeStatusCard(state, viewModel::refresh) }
            items(Feature.featureScreens) { feature ->
                FeatureCard(feature) { onNavigate(feature) }
            }
        }
    }
}

@Composable
private fun RuntimeStatusCard(state: ModelStatusUiState, onRefresh: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Estado del dispositivo", style = MaterialTheme.typography.titleLarge)
            Text(
                "Versión de la app: ${state.appVersion.ifBlank { "consultando…" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Actualizar",
                modifier = Modifier.clickable(onClick = onRefresh),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(Modifier.size(12.dp))
            Text(
                "Android AI Core: ${state.aiCoreVersion.ifBlank { "consultando…" }}",
                style = MaterialTheme.typography.bodyLarge
            )
            if (state.isLoading) {
                Text(
                    "Comprobando modelos instalados…",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else if (state.errorMessage != null) {
                Text(state.errorMessage, color = MaterialTheme.colorScheme.error)
            } else {
                state.runtimes.forEach { RuntimeStatusRow(it) }
            }
        }
    }
}

@Composable
private fun RuntimeStatusRow(runtime: RuntimeStatus) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(runtime.name, style = MaterialTheme.typography.titleMedium)
        Text(
            runtime.status,
            color = if (runtime.isReady) Color(0xFF1B7F3A) else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            runtime.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureCard(feature: Feature, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(feature.iconRes),
                contentDescription = null,
                modifier = Modifier.size(46.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(feature.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    feature.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
