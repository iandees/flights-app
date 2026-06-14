package com.iandees.flights.feature.addedit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
                FormField("Airline", uiState.airline, caps = KeyboardCapitalization.Words) { viewModel.update { copy(airline = it) } }
                FormField("Flight Number (e.g. DL123)", uiState.flightNumber, caps = KeyboardCapitalization.Characters) { viewModel.update { copy(flightNumber = it) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("From (IATA)", uiState.departureAirport, modifier = Modifier.weight(1f), caps = KeyboardCapitalization.Characters) { viewModel.update { copy(departureAirport = it) } }
                    FormField("To (IATA)", uiState.arrivalAirport, modifier = Modifier.weight(1f), caps = KeyboardCapitalization.Characters) { viewModel.update { copy(arrivalAirport = it) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Dep. Date (YYYY-MM-DD)", uiState.departureDate, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { viewModel.update { copy(departureDate = it) } }
                    FormField("Dep. Time (HH:MM)", uiState.departureTime, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { viewModel.update { copy(departureTime = it) } }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Arr. Date (YYYY-MM-DD)", uiState.arrivalDate, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { viewModel.update { copy(arrivalDate = it) } }
                    FormField("Arr. Time (HH:MM)", uiState.arrivalTime, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { viewModel.update { copy(arrivalTime = it) } }
                }
            }

            FormSection("Booking") {
                FormField("Record Locator (PNR)", uiState.recordLocator, caps = KeyboardCapitalization.Characters) { viewModel.update { copy(recordLocator = it) } }
                FormField("Ticket Number", uiState.ticketNumber, keyboardType = KeyboardType.Number) { viewModel.update { copy(ticketNumber = it) } }
            }

            FormSection("Seat") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Seat (e.g. 12A)", uiState.seat, modifier = Modifier.weight(1f), caps = KeyboardCapitalization.Characters) { viewModel.update { copy(seat = it) } }
                    FormField("Boarding Group", uiState.boardingGroup, modifier = Modifier.weight(1f)) { viewModel.update { copy(boardingGroup = it) } }
                }
                FormField("Class (Y / W / J / F)", uiState.seatClass, caps = KeyboardCapitalization.Characters) { viewModel.update { copy(seatClass = it) } }
            }

            FormSection("Aircraft") {
                FormField("Model (e.g. Boeing 737-800)", uiState.planeModel, caps = KeyboardCapitalization.Words) { viewModel.update { copy(planeModel = it) } }
                FormField("Registration / N-code (e.g. N12345)", uiState.registration, caps = KeyboardCapitalization.Characters) { viewModel.update { copy(registration = it) } }
            }

            FormSection("Miles & Segments") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("MQM", uiState.mqm, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { viewModel.update { copy(mqm = it) } }
                    FormField("MQS", uiState.mqs, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { viewModel.update { copy(mqs = it) } }
                    FormField("MQD ($)", uiState.mqd, modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number) { viewModel.update { copy(mqd = it) } }
                }
                FormField("Award Miles", uiState.awardMiles, keyboardType = KeyboardType.Number) { viewModel.update { copy(awardMiles = it) } }
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
