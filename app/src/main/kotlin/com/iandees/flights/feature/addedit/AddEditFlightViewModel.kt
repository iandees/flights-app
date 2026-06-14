package com.iandees.flights.feature.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.database.FlightRepository
import com.iandees.flights.core.model.Flight
import com.iandees.flights.core.network.AirportSearchRepository
import com.iandees.flights.core.network.AirportSuggestion
import com.iandees.flights.core.network.AirportTimezoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import javax.inject.Inject

data class AddEditUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,

    val airline: String = "",
    val flightNumber: String = "",
    val departureAirport: String = "",
    val arrivalAirport: String = "",

    // Date/time stored as "YYYY-MM-DD" / "HH:MM" strings
    val departureDate: String = "",
    val departureTime: String = "",
    val departureTimezone: String = "",   // IANA ID, e.g. "America/Chicago"

    val arrivalDate: String = "",
    val arrivalTime: String = "",
    val arrivalTimezone: String = "",

    val recordLocator: String = "",
    val ticketNumber: String = "",
    val seat: String = "",
    val boardingGroup: String = "",
    val seatClass: String = "",
    val planeModel: String = "",
    val registration: String = "",
    val mqm: String = "",
    val mqs: String = "",
    val mqd: String = "",
    val awardMiles: String = "",
    val notes: String = "",
)

@HiltViewModel
class AddEditFlightViewModel @Inject constructor(
    private val repository: FlightRepository,
    private val airportTz: AirportTimezoneRepository,
    private val airportSearch: AirportSearchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    private val _depSuggestions = MutableStateFlow<List<AirportSuggestion>>(emptyList())
    val depSuggestions: StateFlow<List<AirportSuggestion>> = _depSuggestions.asStateFlow()

    private val _arrSuggestions = MutableStateFlow<List<AirportSuggestion>>(emptyList())
    val arrSuggestions: StateFlow<List<AirportSuggestion>> = _arrSuggestions.asStateFlow()

    fun loadFlight(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val flight = repository.getFlightById(id)
            if (flight != null) {
                val depTz = flight.departureTimezone.toTzOrDefault()
                val arrTz = flight.arrivalTimezone.toTzOrDefault()
                val depLdt = flight.departureTime?.toLocalDateTime(depTz)
                val arrLdt = flight.arrivalTime?.toLocalDateTime(arrTz)
                _uiState.update { _ ->
                    AddEditUiState(
                        isLoading         = false,
                        airline           = flight.airline,
                        flightNumber      = flight.flightNumber,
                        departureAirport  = flight.departureAirport,
                        arrivalAirport    = flight.arrivalAirport,
                        departureDate     = depLdt?.formatDate() ?: "",
                        departureTime     = depLdt?.formatTime() ?: "",
                        departureTimezone = flight.departureTimezone,
                        arrivalDate       = arrLdt?.formatDate() ?: "",
                        arrivalTime       = arrLdt?.formatTime() ?: "",
                        arrivalTimezone   = flight.arrivalTimezone,
                        recordLocator     = flight.recordLocator,
                        ticketNumber      = flight.ticketNumber,
                        seat              = flight.seat,
                        boardingGroup     = flight.boardingGroup,
                        seatClass         = flight.seatClass,
                        planeModel        = flight.planeModel,
                        registration      = flight.registration,
                        mqm               = flight.mqm?.toString() ?: "",
                        mqs               = flight.mqs?.toString() ?: "",
                        mqd               = flight.mqd?.toString() ?: "",
                        awardMiles        = flight.awardMiles?.toString() ?: "",
                        notes             = flight.notes,
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

    /** Called when the departure airport code changes; prefills timezone if not already set. */
    fun onDepartureAirportChange(iata: String) {
        _uiState.update { state ->
            val tz = if (state.departureTimezone.isBlank())
                airportTz.timezoneFor(iata) ?: state.departureTimezone
            else state.departureTimezone
            state.copy(departureAirport = iata.uppercase(), departureTimezone = tz)
        }
        _depSuggestions.value = airportSearch.search(iata)
    }

    fun onDepartureSuggestionSelected(iata: String) {
        _uiState.update { state ->
            val tz = airportTz.timezoneFor(iata) ?: state.departureTimezone
            state.copy(departureAirport = iata, departureTimezone = tz)
        }
        _depSuggestions.value = emptyList()
    }

    fun onArrivalAirportChange(iata: String) {
        _uiState.update { state ->
            val tz = if (state.arrivalTimezone.isBlank())
                airportTz.timezoneFor(iata) ?: state.arrivalTimezone
            else state.arrivalTimezone
            state.copy(arrivalAirport = iata.uppercase(), arrivalTimezone = tz)
        }
        _arrSuggestions.value = airportSearch.search(iata)
    }

    fun onArrivalSuggestionSelected(iata: String) {
        _uiState.update { state ->
            val tz = airportTz.timezoneFor(iata) ?: state.arrivalTimezone
            state.copy(arrivalAirport = iata, arrivalTimezone = tz)
        }
        _arrSuggestions.value = emptyList()
    }

    fun save(existingId: Long?) {
        viewModelScope.launch {
            val s = _uiState.value

            fun parseInstant(date: String, time: String, tzId: String): Instant? {
                if (date.isBlank()) return null
                return try {
                    val tz = tzId.toTzOrDefault()
                    val parts = date.split("-").map { it.toInt() }
                    val timeParts = if (time.isBlank()) listOf(0, 0) else time.split(":").map { it.toInt() }
                    LocalDateTime(parts[0], parts[1], parts[2], timeParts[0], timeParts.getOrElse(1) { 0 })
                        .toInstant(tz)
                } catch (_: Exception) { null }
            }

            val flight = Flight(
                id                = existingId ?: 0,
                airline           = s.airline.trim(),
                flightNumber      = s.flightNumber.trim(),
                departureAirport  = s.departureAirport.trim().uppercase(),
                arrivalAirport    = s.arrivalAirport.trim().uppercase(),
                departureTime     = parseInstant(s.departureDate, s.departureTime, s.departureTimezone),
                arrivalTime       = parseInstant(s.arrivalDate, s.arrivalTime, s.arrivalTimezone),
                departureTimezone = s.departureTimezone,
                arrivalTimezone   = s.arrivalTimezone,
                recordLocator     = s.recordLocator.trim(),
                ticketNumber      = s.ticketNumber.trim(),
                seat              = s.seat.trim(),
                boardingGroup     = s.boardingGroup.trim(),
                seatClass         = s.seatClass.trim(),
                planeModel        = s.planeModel.trim(),
                registration      = s.registration.trim().uppercase(),
                mqm               = s.mqm.toIntOrNull(),
                mqs               = s.mqs.toIntOrNull(),
                mqd               = s.mqd.toIntOrNull(),
                awardMiles        = s.awardMiles.toIntOrNull(),
                notes             = s.notes.trim(),
            )

            if (existingId != null) repository.updateFlight(flight)
            else repository.saveFlight(flight)

            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun String.toTzOrDefault(): TimeZone =
        if (isNotBlank()) try { TimeZone.of(this) } catch (_: Exception) { TimeZone.currentSystemDefault() }
        else TimeZone.currentSystemDefault()

    private fun LocalDateTime.formatDate() = "%04d-%02d-%02d".format(year, monthNumber, dayOfMonth)
    private fun LocalDateTime.formatTime() = "%02d:%02d".format(hour, minute)
}
