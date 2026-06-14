package com.iandees.flights.feature.addedit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iandees.flights.core.network.AirlineSuggestion
import com.iandees.flights.core.network.AirportSuggestion
import java.util.Calendar

@Suppress("DEPRECATION") // hiltViewModel() moved package not yet published in hilt-navigation-compose 1.3
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFlightScreen(
    flightId: Long?,
    onBack: () -> Unit,
    viewModel: AddEditFlightViewModel = hiltViewModel(),
) {
    LaunchedEffect(flightId) { if (flightId != null) viewModel.loadFlight(flightId) }

    val uiState            by viewModel.uiState.collectAsStateWithLifecycle()
    val depSuggestions     by viewModel.depSuggestions.collectAsStateWithLifecycle()
    val arrSuggestions     by viewModel.arrSuggestions.collectAsStateWithLifecycle()
    val airlineSuggestions by viewModel.airlineSuggestions.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onBack() }

    // Intercept system back gesture/button
    BackHandler(enabled = uiState.isDirty && !uiState.isSaved) {
        viewModel.onBackPressed()
    }

    // Discard-changes confirmation dialog
    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDiscardDialog,
            title = { Text("Discard changes?") },
            text  = { Text("You have unsaved changes. Leave without saving?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDiscard() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDiscardDialog) { Text("Keep editing") }
            },
        )
    }

    uiState.lookupError?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::dismissLookupError,
            title = { Text("Flight lookup") },
            text  = { Text(msg) },
            confirmButton = { TextButton(onClick = viewModel::dismissLookupError) { Text("OK") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (flightId == null) "Add Flight" else "Edit Flight") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!viewModel.onBackPressed()) onBack()
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(flightId) }) {
                        Icon(Icons.Default.Check, "Save")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Route ──────────────────────────────────────────────────────
            FormSection("Route") {
                // Airline autocomplete
                AutocompleteField(
                    label = "Airline",
                    value = uiState.airline,
                    suggestions = airlineSuggestions.map { "${it.iata} – ${it.name}" },
                    onValueChange = { viewModel.onAirlineChange(it) },
                    onSuggestionSelected = { idx ->
                        val s = airlineSuggestions[idx]
                        viewModel.onAirlineSuggestionSelected(s.iata, s.name)
                    },
                    onDismiss = viewModel::dismissAirlineSuggestions,
                    caps = KeyboardCapitalization.Words,
                )

                // Flight number + lookup button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FormField(
                        label = "Flight No. (e.g. 123)",
                        value = uiState.flightNumber,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                    ) { viewModel.update { copy(flightNumber = it) } }

                    if (uiState.isLookingUpFlight) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(
                            onClick = { viewModel.lookupFlight() },
                            enabled = uiState.airline.isNotBlank() &&
                                      uiState.flightNumber.isNotBlank() &&
                                      uiState.departureDate.isNotBlank(),
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Auto-fill from flight number")
                        }
                    }
                }
                if (uiState.airline.isNotBlank() && uiState.flightNumber.isNotBlank() && uiState.departureDate.isBlank()) {
                    Text(
                        "Set the departure date below, then tap 🔍 to auto-fill route and times.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // From / To airport autocomplete
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AirportAutocompleteField(
                        label = "From (IATA)",
                        value = uiState.departureAirport,
                        suggestions = depSuggestions,
                        modifier = Modifier.weight(1f),
                        onValueChange = { viewModel.onDepartureAirportChange(it) },
                        onSuggestionSelected = { viewModel.onDepartureSuggestionSelected(it) },
                    )
                    AirportAutocompleteField(
                        label = "To (IATA)",
                        value = uiState.arrivalAirport,
                        suggestions = arrSuggestions,
                        modifier = Modifier.weight(1f),
                        onValueChange = { viewModel.onArrivalAirportChange(it) },
                        onSuggestionSelected = { viewModel.onArrivalSuggestionSelected(it) },
                    )
                }

                DateTimePickerField(
                    label = "Departure",
                    date  = uiState.departureDate,
                    time  = uiState.departureTime,
                    onDateTimeChange = { d, t -> viewModel.update { copy(departureDate = d, departureTime = t) } },
                )

                DateTimePickerField(
                    label = "Arrival",
                    date  = uiState.arrivalDate,
                    time  = uiState.arrivalTime,
                    onDateTimeChange = { d, t -> viewModel.update { copy(arrivalDate = d, arrivalTime = t) } },
                )
            }

            // ── Booking ────────────────────────────────────────────────────
            FormSection("Booking") {
                // Record locator: monospace font, uppercase letters + digits
                OutlinedTextField(
                    value = uiState.recordLocator,
                    onValueChange = { viewModel.update { copy(recordLocator = it.uppercase()) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Record Locator (PNR)") },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Password,
                    ),
                )

                FormField("Ticket Number", uiState.ticketNumber, keyboardType = KeyboardType.Number) {
                    viewModel.update { copy(ticketNumber = it) }
                }
            }

            // ── Seat ───────────────────────────────────────────────────────
            FormSection("Seat") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Seat: digits + capital letters (e.g. "12A")
                    FormField(
                        label = "Seat (e.g. 12A)",
                        value = uiState.seat,
                        modifier = Modifier.weight(1f),
                        caps = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Password,
                    ) { viewModel.update { copy(seat = it.uppercase()) } }

                    FormField(
                        label = "Boarding Group",
                        value = uiState.boardingGroup,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Number,
                    ) { viewModel.update { copy(boardingGroup = it) } }
                }
                FormField(
                    label = "Class (Y / W / J / F)",
                    value = uiState.seatClass,
                    caps = KeyboardCapitalization.Characters,
                ) { viewModel.update { copy(seatClass = it.uppercase()) } }
            }

            // ── Aircraft ───────────────────────────────────────────────────
            FormSection("Aircraft") {
                FormField("Model (e.g. Boeing 737-800)", uiState.planeModel, caps = KeyboardCapitalization.Words) {
                    viewModel.update { copy(planeModel = it) }
                }
                // Registration / N-code: uppercase letters + digits
                FormField(
                    label = "Registration / N-code (e.g. N12345)",
                    value = uiState.registration,
                    caps = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Password,
                ) { viewModel.update { copy(registration = it.uppercase()) } }
            }

            // ── Miles ──────────────────────────────────────────────────────
            FormSection("Miles & Segments") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("MQM", uiState.mqm, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) {
                        viewModel.update { copy(mqm = it) }
                    }
                    FormField("MQS", uiState.mqs, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) {
                        viewModel.update { copy(mqs = it) }
                    }
                    FormField("MQD ($)", uiState.mqd, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) {
                        viewModel.update { copy(mqd = it) }
                    }
                }
                FormField("Award Miles", uiState.awardMiles, keyboardType = KeyboardType.Number) {
                    viewModel.update { copy(awardMiles = it) }
                }
            }

            // ── Notes ──────────────────────────────────────────────────────
            FormSection("Notes") {
                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { viewModel.update { copy(notes = it) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes (delays, cancellations, etc.)") },
                    minLines = 3,
                    maxLines = 8,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

/** Generic string-list autocomplete using ExposedDropdownMenuBox. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutocompleteField(
    label: String,
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit,
    onSuggestionSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    caps: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    ExposedDropdownMenuBox(
        expanded = suggestions.isNotEmpty(),
        onExpandedChange = {},
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = caps, keyboardType = keyboardType),
        )
        if (suggestions.isNotEmpty()) {
            ExposedDropdownMenu(expanded = true, onDismissRequest = onDismiss) {
                suggestions.forEachIndexed { idx, text ->
                    DropdownMenuItem(
                        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
                        onClick = { onSuggestionSelected(idx) },
                    )
                }
            }
        }
    }
}

/** Airport-specific autocomplete showing IATA code + airport name. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AirportAutocompleteField(
    label: String,
    value: String,
    suggestions: List<AirportSuggestion>,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = suggestions.isNotEmpty(),
        onExpandedChange = {},
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        )
        if (suggestions.isNotEmpty()) {
            ExposedDropdownMenu(expanded = true, onDismissRequest = { onSuggestionSelected(value) }) {
                suggestions.forEach { s ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(s.iata, style = MaterialTheme.typography.bodyMedium)
                                Text(s.label, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        },
                        onClick = { onSuggestionSelected(s.iata) },
                    )
                }
            }
        }
    }
}

/** Combined date+time picker button, plus timezone autocomplete field. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerField(
    label: String,
    date: String,
    time: String,
    onDateTimeChange: (date: String, time: String) -> Unit,
) {
    val context = LocalContext.current
    val cal = remember(date, time) {
        Calendar.getInstance().also { c ->
            date.split("-").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 }?.let { (y, m, d) ->
                c.set(y, m - 1, d)
            }
            time.split(":").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 }?.let { (h, min) ->
                c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, min)
            }
        }
    }

    val displayText = when {
        date.isBlank() -> "$label date & time"
        time.isBlank() -> date
        else           -> "$date  $time"
    }

    OutlinedButton(
        onClick = {
            DatePickerDialog(context, { _, year, month, day ->
                val dateStr = "%04d-%02d-%02d".format(year, month + 1, day)
                TimePickerDialog(context, { _, hour, minute ->
                    onDateTimeChange(dateStr, "%02d:%02d".format(hour, minute))
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(displayText, maxLines = 1)
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    caps: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = caps, keyboardType = keyboardType),
    )
}
