package com.iandees.flights.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val savedKey by viewModel.apiKey.collectAsStateWithLifecycle()

    // Local draft — edit freely, only persisted on Save
    var draft by remember(savedKey) { mutableStateOf(savedKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── AirLabs section ──────────────────────────────────────────
            Text("Flight Data Lookup", style = MaterialTheme.typography.titleMedium)

            Text(
                "Enter your AirLabs API key to enable auto-fill when adding a flight. " +
                "Get a free key at airlabs.co — the free tier is enough for personal use.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it; saved = false },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("AirLabs API Key") },
                placeholder = { Text("Paste key here…") },
                singleLine = true,
                visualTransformation = if (keyVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { keyVisible = !keyVisible }) {
                        Icon(
                            if (keyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (keyVisible) "Hide key" else "Show key",
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    viewModel.saveApiKey(draft)
                    saved = true
                }),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.saveApiKey(draft)
                        saved = true
                    },
                    enabled = draft != savedKey || !saved,
                ) {
                    Text("Save")
                }
                if (draft.isNotBlank()) {
                    OutlinedButton(onClick = {
                        draft = ""
                        viewModel.saveApiKey("")
                        saved = false
                    }) {
                        Text("Clear")
                    }
                }
            }

            if (saved && draft.isNotBlank()) {
                Text(
                    "✓ API key saved.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            HorizontalDivider()

            // ── Status badge ─────────────────────────────────────────────
            val statusText = when {
                savedKey.isBlank() -> "Not configured — flight lookup disabled"
                else               -> "Configured (${savedKey.take(4)}${"•".repeat(8)})"
            }
            val statusColor = if (savedKey.isBlank())
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary

            Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
        }
    }
}
