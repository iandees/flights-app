package com.iandees.flights.feature.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iandees.flights.core.model.FlightStats
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Stats") }) }) { padding ->
        when (val s = uiState) {
            is StatsUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is StatsUiState.Empty -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No flights yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is StatsUiState.Success -> StatsContent(stats = s.stats, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun StatsContent(stats: FlightStats, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Summary cards
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Segments", stats.totalSegments.toString(), modifier = Modifier.weight(1f))
            SummaryCard("MQM", "%,d".format(stats.totalMqm), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("MQS", stats.totalMqs.toString(), modifier = Modifier.weight(1f))
            SummaryCard("Award Miles", "%,d".format(stats.totalAwardMiles), modifier = Modifier.weight(1f))
        }

        // Segments by year chart
        if (stats.segmentsByYear.isNotEmpty()) {
            StatsCard("Segments by Year") {
                val sortedYears = stats.segmentsByYear.entries.sortedBy { it.key }
                val producer = remember(stats.segmentsByYear) {
                    CartesianChartModelProducer()
                }
                LaunchedEffect(stats.segmentsByYear) {
                    producer.runTransaction {
                        columnSeries { series(sortedYears.map { it.value }) }
                    }
                }
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberColumnCartesianLayer(),
                        startAxis = VerticalAxis.rememberStart(),
                        bottomAxis = HorizontalAxis.rememberBottom(
                            valueFormatter = { _, x, _ -> sortedYears.getOrNull(x.toInt())?.key?.toString() ?: "" }
                        ),
                    ),
                    modelProducer = producer,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
            }
        }

        // Top airlines
        if (stats.segmentsByAirline.isNotEmpty()) {
            StatsCard("Segments by Airline") {
                stats.segmentsByAirline.entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .forEach { (airline, count) ->
                        RankRow(label = airline, count = count, total = stats.totalSegments)
                    }
            }
        }

        // Top plane models
        if (stats.segmentsByPlaneModel.isNotEmpty()) {
            StatsCard("Segments by Aircraft") {
                stats.segmentsByPlaneModel.entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .forEach { (model, count) ->
                        RankRow(label = model, count = count, total = stats.totalSegments)
                    }
            }
        }

        // Seat class breakdown
        if (stats.segmentsBySeatClass.isNotEmpty()) {
            StatsCard("Segments by Class") {
                stats.segmentsBySeatClass.entries
                    .sortedByDescending { it.value }
                    .forEach { (cls, count) ->
                        RankRow(label = cls, count = count, total = stats.totalSegments)
                    }
            }
        }

        // Top airports
        if (stats.topDepartureAirports.isNotEmpty()) {
            StatsCard("Top Departure Airports") {
                stats.topDepartureAirports.forEach { (airport, count) ->
                    RankRow(label = airport, count = count, total = stats.totalSegments)
                }
            }
        }

        // Top routes
        if (stats.topRoutes.isNotEmpty()) {
            StatsCard("Top Routes") {
                stats.topRoutes.forEach { (from, to, count) ->
                    RankRow(label = "$from → $to", count = count, total = stats.totalSegments)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun RankRow(label: String, count: Int, total: Int) {
    val fraction = if (total > 0) count.toFloat() / total else 0f
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(count.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(4.dp),
        )
    }
}
