package com.iandees.flights.feature.importcsv

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCsvScreen(
    onBack: () -> Unit,
    viewModel: ImportCsvViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            viewModel.onFilePicked(uri) { u ->
                context.contentResolver.openInputStream(u)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import CSV") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.reset()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = uiState) {
                is ImportUiState.Idle -> IdleContent(onPickFile = { filePicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) })
                is ImportUiState.Parsing -> LoadingContent("Parsing CSV…")
                is ImportUiState.Preview -> PreviewContent(
                    state = s,
                    onConfirm = { viewModel.confirmImport() },
                    onCancel  = { viewModel.reset() },
                )
                is ImportUiState.Importing -> LoadingContent("Importing ${(uiState as? ImportUiState.Preview)?.flightCount ?: ""} flights…")
                is ImportUiState.Done -> DoneContent(imported = s.imported, onDone = { viewModel.reset(); onBack() })
                is ImportUiState.Error -> ErrorContent(message = s.message, onRetry = { viewModel.reset() })
            }
        }
    }
}

@Composable
private fun IdleContent(onPickFile: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Import from Google Sheets CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = "Export your Google Sheet as CSV (File → Download → CSV), then tap below to pick the file.\n\n" +
                   "Expected columns:\nAirline · From · To · Departure · Arrival · Record Loc · " +
                   "Ticket # · Flight No · Seat · Group · Class · Plane · Registration · " +
                   "MQM · MQS · MQD · Award Miles · Notes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
            Text("Choose CSV File")
        }
    }
}

@Composable
private fun LoadingContent(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        CircularProgressIndicator()
        Text(message)
    }
}

@Composable
private fun PreviewContent(
    state: ImportUiState.Preview,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ready to Import", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Flights found")
                Text(state.flightCount.toString(), fontWeight = FontWeight.Bold)
            }
            if (state.skipped > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Rows skipped")
                    Text(state.skipped.toString(), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
            if (state.errors.isNotEmpty()) {
                Text("Errors:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                state.errors.take(5).forEach { err ->
                    Text("• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (state.errors.size > 5) {
                    Text("…and ${state.errors.size - 5} more", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
        Button(onClick = onConfirm, modifier = Modifier.weight(1f), enabled = state.flightCount > 0) {
            Text("Import ${state.flightCount} Flights")
        }
    }
}

@Composable
private fun DoneContent(imported: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text("✅", style = MaterialTheme.typography.displayMedium)
        Text("Imported $imported flights!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text("⚠️", style = MaterialTheme.typography.displayMedium)
        Text("Import failed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Try Again") }
    }
}
