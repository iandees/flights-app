package com.iandees.flights.feature.flightdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.database.FlightRepository
import com.iandees.flights.core.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FlightDetailViewModel @Inject constructor(
    private val repository: FlightRepository,
) : ViewModel() {

    private val _flight = MutableStateFlow<Flight?>(null)
    val flight: StateFlow<Flight?> = _flight.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _flight.value = repository.getFlightById(id)
        }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteFlight(id)
            onDone()
        }
    }
}
