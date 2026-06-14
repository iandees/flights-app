package com.iandees.flights.feature.flightlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.database.FlightRepository
import com.iandees.flights.core.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FlightListUiState(
    val flights: List<Flight> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class FlightListViewModel @Inject constructor(
    private val repository: FlightRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<FlightListUiState> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) repository.getAllFlights()
            else repository.searchFlights(query)
        }
        .map { flights -> FlightListUiState(flights = flights, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FlightListUiState(),
        )

    // Expose raw query directly so the text field value is always in sync.
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteFlight(id: Long) {
        viewModelScope.launch { repository.deleteFlight(id) }
    }
}
