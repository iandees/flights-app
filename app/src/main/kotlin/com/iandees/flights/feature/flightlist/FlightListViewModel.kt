package com.iandees.flights.feature.flightlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.database.FlightRepository
import com.iandees.flights.core.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

/** Items that can appear in the flight list. */
sealed interface FlightListItem {
    /** A "Month YYYY" section header. */
    data class MonthHeader(val label: String) : FlightListItem
    /** The "─── Today ───" divider inserted between past and future flights. */
    data object TodayDivider : FlightListItem
    /** A regular flight row. */
    data class FlightRow(val flight: Flight) : FlightListItem
}

data class FlightListUiState(
    val items: List<FlightListItem> = emptyList(),
    val isLoading: Boolean = true,
    // Index to auto-scroll to on first load (the TodayDivider position)
    val todayIndex: Int = 0,
)

@HiltViewModel
class FlightListViewModel @Inject constructor(
    private val repository: FlightRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<FlightListUiState> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllFlights()
            else repository.searchFlights(query)
        }
        .map { flights -> buildUiState(flights, _searchQuery.value.isBlank()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FlightListUiState(),
        )

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun deleteFlight(id: Long) {
        viewModelScope.launch { repository.deleteFlight(id) }
    }

    private fun buildUiState(flights: List<Flight>, insertTodayDivider: Boolean): FlightListUiState {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date

        val items = mutableListOf<FlightListItem>()
        var lastMonth: String? = null
        var todayInserted = false
        var todayIndex = 0

        flights.forEach { flight ->
            val depDate = flight.departureTime
                ?.toLocalDateTime(tz)?.date

            // Insert "Today" divider once, just before the first future/today flight
            if (insertTodayDivider && !todayInserted && depDate != null && depDate >= today) {
                todayIndex = items.size
                items.add(FlightListItem.TodayDivider)
                todayInserted = true
            }

            // Month header
            val monthLabel = depDate?.let {
                "${it.month.name.lowercase().replaceFirstChar { c -> c.uppercase() }} ${it.year}"
            }
            if (monthLabel != null && monthLabel != lastMonth) {
                items.add(FlightListItem.MonthHeader(monthLabel))
                lastMonth = monthLabel
            }

            items.add(FlightListItem.FlightRow(flight))
        }

        // If all flights are in the past, append the divider at the end
        if (insertTodayDivider && !todayInserted) {
            todayIndex = items.size
            items.add(FlightListItem.TodayDivider)
        }

        return FlightListUiState(items = items, isLoading = false, todayIndex = todayIndex)
    }
}
