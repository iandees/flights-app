package com.iandees.flights.feature.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.database.FlightRepository
import com.iandees.flights.core.model.Flight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

data class AddEditUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,

    // Form fields — all strings for easy text field binding
    val airline: String = "",
    val flightNumber: String = "",
    val departureAirport: String = "",
    val arrivalAirport: String = "",
    val departureDate: String = "",   // "YYYY-MM-DD"
    val departureTime: String = "",   // "HH:MM"
    val arrivalDate: String = "",
    val arrivalTime: String = "",
    val recordLocator: String = "",
    val ticketNumber: String = "",
    val seat: String = "",
    val boardingGroup: String = "",
    val seatClass: String = "",
    val planeModel: String = "",
    val registration: String = "",    // aircraft tail number / N-code
    val mqm: String = "",
    val mqs: String = "",
    val mqd: String = "",
    val awardMiles: String = "",
    val notes: String = "",
)

@HiltViewModel
class AddEditFlightViewModel @Inject constructor(
    private val repository: FlightRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    fun loadFlight(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val flight = repository.getFlightById(id)
            if (flight != null) {
                val tz = TimeZone.currentSystemDefault()
                val depLdt = flight.departureTime?.toLocalDateTime(tz)
                val arrLdt = flight.arrivalTime?.toLocalDateTime(tz)
                _uiState.update { _ ->
                    AddEditUiState(
                        isLoading      = false,
                        airline        = flight.airline,
                        flightNumber   = flight.flightNumber,
                        departureAirport = flight.departureAirport,
                        arrivalAirport = flight.arrivalAirport,
                        departureDate  = depLdt?.let { "%04d-%02d-%02d".format(it.year, it.monthNumber, it.dayOfMonth) } ?: "",
                        departureTime  = depLdt?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "",
                        arrivalDate    = arrLdt?.let { "%04d-%02d-%02d".format(it.year, it.monthNumber, it.dayOfMonth) } ?: "",
                        arrivalTime    = arrLdt?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "",
                        recordLocator  = flight.recordLocator,
                        ticketNumber   = flight.ticketNumber,
                        seat           = flight.seat,
                        boardingGroup  = flight.boardingGroup,
                        seatClass      = flight.seatClass,
                        planeModel     = flight.planeModel,
                        registration   = flight.registration,
                        mqm            = flight.mqm?.toString() ?: "",
                        mqs            = flight.mqs?.toString() ?: "",
                        mqd            = flight.mqd?.toString() ?: "",
                        awardMiles     = flight.awardMiles?.toString() ?: "",
                        notes          = flight.notes,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun update(block: AddEditUiState.() -> AddEditUiState) {
        _uiState.update { it.block() }
    }

    fun save(existingId: Long?) {
        viewModelScope.launch {
            val s = _uiState.value
            val tz = TimeZone.currentSystemDefault()

            fun parseInstant(date: String, time: String): Instant? {
                if (date.isBlank()) return null
                return try {
                    val parts = date.split("-").map { it.toInt() }
                    val timeParts = if (time.isBlank()) listOf(0, 0) else time.split(":").map { it.toInt() }
                    LocalDateTime(parts[0], parts[1], parts[2], timeParts[0], timeParts.getOrElse(1) { 0 })
                        .toInstant(tz)
                } catch (_: Exception) { null }
            }

            val flight = Flight(
                id               = existingId ?: 0,
                airline          = s.airline.trim(),
                flightNumber     = s.flightNumber.trim(),
                departureAirport = s.departureAirport.trim().uppercase(),
                arrivalAirport   = s.arrivalAirport.trim().uppercase(),
                departureTime    = parseInstant(s.departureDate, s.departureTime),
                arrivalTime      = parseInstant(s.arrivalDate, s.arrivalTime),
                recordLocator    = s.recordLocator.trim(),
                ticketNumber     = s.ticketNumber.trim(),
                seat             = s.seat.trim(),
                boardingGroup    = s.boardingGroup.trim(),
                seatClass        = s.seatClass.trim(),
                planeModel       = s.planeModel.trim(),
                registration     = s.registration.trim().uppercase(),
                mqm              = s.mqm.toIntOrNull(),
                mqs              = s.mqs.toIntOrNull(),
                mqd              = s.mqd.toIntOrNull(),
                awardMiles       = s.awardMiles.toIntOrNull(),
                notes            = s.notes.trim(),
            )

            if (existingId != null) repository.updateFlight(flight)
            else repository.saveFlight(flight)

            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
