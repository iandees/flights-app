package com.iandees.flights.feature.addedit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iandees.flights.core.network.AirportSuggestion
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFlightScreen(
    flightId: Long?,
    onBack: () -> Unit,
    viewModel: AddEditFlightViewModel = hiltViewModel(),
) {
    LaunchedEffect(flightId) {
        if (flightId != null) viewModel.loadFlight(flightId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val depSuggestions by viewModel.depSuggestions.collectAsStateWithLifecycle()
    val arrSuggestions by viewModel.arrSuggestions.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (flightId == null) "Add Flight" else "Edit Flight") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save(flightId) }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
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
            FormSection("Route") {
                FormField("Airline", uiState.airline, caps = KeyboardCapitalization.Words) {
                    viewModel.update { copy(airline = it) }
                }
                FormField("Flight Number (e.g. DL123)", uiState.flightNumber, caps = KeyboardCapitalization.Characters) {
                    viewModel.update { copy(flightNumber = it) }
                }
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
                    date = uiState.departureDate,
                    time = uiState.departureTime,
                    timezone = uiState.departureTimezone,
                    onDateTimeChange = { d, t -> viewModel.update { copy(departureDate = d, departureTime = t) } },
                    onTimezoneChange = { viewModel.update { copy(departureTimezone = it) } },
                )

                DateTimePickerField(
                    label = "Arrival",
                    date = uiState.arrivalDate,
                    time = uiState.arrivalTime,
                    timezone = uiState.arrivalTimezone,
                    onDateTimeChange = { d, t -> viewModel.update { copy(arrivalDate = d, arrivalTime = t) } },
                    onTimezoneChange = { viewModel.update { copy(arrivalTimezone = it) } },
                )
            }

            FormSection("Booking") {
                FormField("Record Locator (PNR)", uiState.recordLocator, caps = KeyboardCapitalization.Characters) {
                    viewModel.update { copy(recordLocator = it) }
                }
                FormField("Ticket Number", uiState.ticketNumber, keyboardType = KeyboardType.Number) {
                    viewModel.update { copy(ticketNumber = it) }
                }
            }

            FormSection("Seat") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Seat (e.g. 12A)", uiState.seat, modifier = Modifier.weight(1f), caps = KeyboardCapitalization.Characters) {
                        viewModel.update { copy(seat = it) }
                    }
                    FormField("Boarding Group", uiState.boardingGroup, modifier = Modifier.weight(1f)) {
                        viewModel.update { copy(boardingGroup = it) }
                    }
                }
                FormField("Class (Y / W / J / F)", uiState.seatClass, caps = KeyboardCapitalization.Characters) {
                    viewModel.update { copy(seatClass = it) }
                }
            }

            FormSection("Aircraft") {
                FormField("Model (e.g. Boeing 737-800)", uiState.planeModel, caps = KeyboardCapitalization.Words) {
                    viewModel.update { copy(planeModel = it) }
                }
                FormField("Registration / N-code (e.g. N12345)", uiState.registration, caps = KeyboardCapitalization.Characters) {
                    viewModel.update { copy(registration = it) }
                }
            }

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

/**
 * Text field with a dropdown showing up to 6 airport suggestions.
 * Suggestions show the IATA code + airport name/city.
 * Selecting a suggestion fills the field with just the IATA code.
 */
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
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        )
        if (suggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = true,
                onDismissRequest = { onSuggestionSelected(value) },
            ) {
                suggestions.forEach { s ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(s.iata, style = MaterialTheme.typography.bodyMedium)
                                Text(s.label, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1)
                            }
                        },
                        onClick = { onSuggestionSelected(s.iata) },
                    )
                }
            }
        }
    }
}

/**
 * A single row that opens a DatePickerDialog, then immediately chains into a TimePickerDialog,
 * followed by an editable timezone field.
 */
@Composable
private fun DateTimePickerField(
    label: String,
    date: String,
    time: String,
    timezone: String,
    onDateTimeChange: (date: String, time: String) -> Unit,
    onTimezoneChange: (String) -> Unit,
) {
    val context = LocalContext.current

    // Parse current values so pickers open at the right position
    val cal = remember(date, time) {
        Calendar.getInstance().also { c ->
            date.split("-").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 }?.let { (y, m, d) ->
                c.set(y, m - 1, d)
            }
            time.split(":").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 }?.let { (h, min) ->
                c.set(Calendar.HOUR_OF_DAY, h)
                c.set(Calendar.MINUTE, min)
            }
        }
    }

    val displayText = when {
        date.isBlank() -> "$label date & time"
        time.isBlank() -> date
        else -> "$date  $time"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Single button → DatePicker → TimePicker chained
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        val dateStr = "%04d-%02d-%02d".format(year, month + 1, day)
                        // Immediately chain into time picker
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                onDateTimeChange(dateStr, "%02d:%02d".format(hour, minute))
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true, // 24-hour
                        ).show()
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH),
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(displayText, maxLines = 1)
        }

        // Timezone field
        OutlinedTextField(
            value = timezone,
            onValueChange = onTimezoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("$label timezone (e.g. America/Chicago)") },
            singleLine = true,
        )
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
