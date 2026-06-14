package com.iandees.flights.feature.flightlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iandees.flights.core.model.Flight
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Suppress("DEPRECATION") // hiltViewModel() moved package not yet published in hilt-navigation-compose 1.3
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightListScreen(
    onFlightClick: (Long) -> Unit,
    onAddFlight: () -> Unit,
    onImportCsv: () -> Unit,
    onSettings: () -> Unit,
    viewModel: FlightListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to the today divider once on first load
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.todayIndex > 0) {
            coroutineScope.launch { listState.scrollToItem(uiState.todayIndex) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flights") },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Import CSV") },
                            leadingIcon = { Icon(Icons.Default.FileUpload, null) },
                            onClick = { showMenu = false; onImportCsv() },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            leadingIcon = { Icon(Icons.Default.Settings, null) },
                            onClick = { showMenu = false; onSettings() },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFlight) {
                Icon(Icons.Default.Add, contentDescription = "Add flight")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(top = padding.calculateTopPadding())
                .fillMaxSize()
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search flights…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.items.none { it is FlightListItem.FlightRow }) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (searchQuery.isBlank())
                            "No flights yet.\nTap + to add one or import from CSV."
                        else
                            "No flights match \"$searchQuery\".",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = padding.calculateBottomPadding() + 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.items, key = { item ->
                        when (item) {
                            is FlightListItem.MonthHeader  -> "header_${item.label}"
                            is FlightListItem.TodayDivider -> "today_divider"
                            is FlightListItem.FlightRow    -> "flight_${item.flight.id}"
                        }
                    }) { item ->
                        when (item) {
                            is FlightListItem.MonthHeader  -> MonthHeader(item.label)
                            is FlightListItem.TodayDivider -> TodayDivider()
                            is FlightListItem.FlightRow    -> FlightCard(
                                flight  = item.flight,
                                onClick = { onFlightClick(item.flight.id) },
                                onDelete = { viewModel.deleteFlight(item.flight.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun TodayDivider() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = "  Today  ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FlightCard(
    flight: Flight,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val tz = TimeZone.currentSystemDefault()

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete flight?") },
            text = { Text("${flight.airline} ${flight.flightNumber}: ${flight.departureAirport} → ${flight.arrivalAirport}") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Route line
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = flight.departureAirport.ifBlank { "???" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Icon(
                        Icons.Default.FlightTakeoff,
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = flight.arrivalAirport.ifBlank { "???" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Airline + flight number
                Text(
                    text = buildString {
                        if (flight.airline.isNotBlank()) append(flight.airline)
                        if (flight.flightNumber.isNotBlank()) {
                            if (isNotEmpty()) append(" ")
                            append(flight.flightNumber)
                        }
                    }.ifBlank { "Unknown airline" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Date
                flight.departureTime?.let { instant ->
                    val ldt = instant.toLocalDateTime(tz)
                    Text(
                        text = "%04d-%02d-%02d".format(ldt.year, ldt.monthNumber, ldt.dayOfMonth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Seat + class
                val seatInfo = listOf(flight.seat, flight.seatClass).filter { it.isNotBlank() }.joinToString(" · ")
                if (seatInfo.isNotBlank()) {
                    Text(
                        text = seatInfo,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // Aircraft
                val aircraftInfo = listOf(flight.planeModel, flight.registration).filter { it.isNotBlank() }.joinToString(" / ")
                if (aircraftInfo.isNotBlank()) {
                    Text(
                        text = aircraftInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete flight",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
