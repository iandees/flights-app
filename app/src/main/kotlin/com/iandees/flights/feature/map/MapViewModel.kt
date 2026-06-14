package com.iandees.flights.feature.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.database.FlightRepository
import com.iandees.flights.core.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MapUiState(
    val flights: List<Flight> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class MapViewModel @Inject constructor(
    repository: FlightRepository,
) : ViewModel() {

    val uiState: StateFlow<MapUiState> = repository.getAllFlights()
        .map { MapUiState(flights = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MapUiState(),
        )
}
