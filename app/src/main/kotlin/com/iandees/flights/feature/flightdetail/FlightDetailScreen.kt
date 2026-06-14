package com.iandees.flights.feature.flightdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iandees.flights.core.model.Flight
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailScreen(
    flightId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    viewModel: FlightDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(flightId) { viewModel.load(flightId) }

    val flight by viewModel.flight.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this flight?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(flightId, onDone = onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    flight?.let { Text("${it.departureAirport} → ${it.arrivalAirport}") }
                        ?: Text("Flight Detail")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                },
            )
        },
    ) { padding ->
        flight?.let { f ->
            FlightDetailContent(flight = f, modifier = Modifier.padding(padding))
        } ?: Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun FlightDetailContent(flight: Flight, modifier: Modifier = Modifier) {
    val tz = TimeZone.currentSystemDefault()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Route hero
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(flight.departureAirport, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    flight.departureTime?.let {
                        val ldt = it.toLocalDateTime(tz)
                        Text("%02d:%02d".format(ldt.hour, ldt.minute), style = MaterialTheme.typography.bodyLarge)
                        Text("%04d-%02d-%02d".format(ldt.year, ldt.monthNumber, ldt.dayOfMonth), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Icon(Icons.Default.FlightTakeoff, null, modifier = Modifier.padding(top = 12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(flight.arrivalAirport, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    flight.arrivalTime?.let {
                        val ldt = it.toLocalDateTime(tz)
                        Text("%02d:%02d".format(ldt.hour, ldt.minute), style = MaterialTheme.typography.bodyLarge)
                        Text("%04d-%02d-%02d".format(ldt.year, ldt.monthNumber, ldt.dayOfMonth), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        DetailSection(title = "Flight") {
            DetailRow("Airline", flight.airline)
            DetailRow("Flight Number", flight.flightNumber)
        }

        DetailSection(title = "Booking") {
            DetailRow("Record Locator", flight.recordLocator)
            DetailRow("Ticket Number", flight.ticketNumber)
        }

        DetailSection(title = "Seat") {
            DetailRow("Seat", flight.seat)
            DetailRow("Boarding Group", flight.boardingGroup)
            DetailRow("Class", flight.seatClass)
        }

        DetailSection(title = "Aircraft") {
            DetailRow("Model", flight.planeModel)
            DetailRow("Registration (N-code)", flight.registration)
        }

        if (flight.mqm != null || flight.mqs != null || flight.mqd != null || flight.awardMiles != null) {
            DetailSection(title = "Miles & Segments") {
                flight.mqm?.let { DetailRow("MQM", it.toString()) }
                flight.mqs?.let { DetailRow("MQS", it.toString()) }
                flight.mqd?.let { DetailRow("MQD", "$${it}") }
                flight.awardMiles?.let { DetailRow("Award Miles", it.toString()) }
            }
        }

        if (flight.notes.isNotBlank()) {
            DetailSection(title = "Notes") {
                Text(flight.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
