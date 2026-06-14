package com.iandees.flights.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.database.FlightRepository
import com.iandees.flights.core.model.FlightStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(val stats: FlightStats) : StatsUiState()
    object Empty : StatsUiState()
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: FlightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllFlights().collect { flights ->
                if (flights.isEmpty()) {
                    _uiState.value = StatsUiState.Empty
                } else {
                    _uiState.value = StatsUiState.Success(repository.computeStats())
                }
            }
        }
    }
}
