package com.iandees.flights.feature.addedit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iandees.flights.core.database.FlightRepository
import com.iandees.flights.core.model.Flight
import com.iandees.flights.core.network.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import javax.inject.Inject

data class AddEditUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDirty: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val error: String? = null,
    val isLookingUpFlight: Boolean = false,
    val lookupError: String? = null,

    val airline: String = "",
    val flightNumber: String = "",
    val departureAirport: String = "",
    val arrivalAirport: String = "",

    val departureDate: String = "",
    val departureTime: String = "",
    val departureTimezone: String = "",

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
    private val airlineSearch: AirlineSearchRepository,
    private val timezoneSearch: TimezoneSearchRepository,
    private val flightLookup: FlightLookupService,
    private val settings: AppSettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    // Autocomplete suggestion streams
    private val _depSuggestions = MutableStateFlow<List<AirportSuggestion>>(emptyList())
    val depSuggestions: StateFlow<List<AirportSuggestion>> = _depSuggestions.asStateFlow()

    private val _arrSuggestions = MutableStateFlow<List<AirportSuggestion>>(emptyList())
    val arrSuggestions: StateFlow<List<AirportSuggestion>> = _arrSuggestions.asStateFlow()

    private val _airlineSuggestions = MutableStateFlow<List<AirlineSuggestion>>(emptyList())
    val airlineSuggestions: StateFlow<List<AirlineSuggestion>> = _airlineSuggestions.asStateFlow()

    private val _depTzSuggestions = MutableStateFlow<List<String>>(emptyList())
    val depTzSuggestions: StateFlow<List<String>> = _depTzSuggestions.asStateFlow()

    private val _arrTzSuggestions = MutableStateFlow<List<String>>(emptyList())
    val arrTzSuggestions: StateFlow<List<String>> = _arrTzSuggestions.asStateFlow()

    // Usage counts from existing flights (computed once)
    private var airlineUsage: Map<String, Int> = emptyMap()
    private var timezoneUsage: Map<String, Int> = emptyMap()

    init {
        viewModelScope.launch {
            repository.getAllFlights().first().let { flights ->
                airlineUsage  = flights.groupingBy { it.airline }.eachCount()
                timezoneUsage = buildMap {
                    flights.forEach { f ->
                        if (f.departureTimezone.isNotBlank())
                            merge(f.departureTimezone, 1, Int::plus)
                        if (f.arrivalTimezone.isNotBlank())
                            merge(f.arrivalTimezone, 1, Int::plus)
                    }
                }
            }
        }
    }

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

    /** Called by every field change; marks the form dirty so back-navigation confirms. */
    fun update(block: AddEditUiState.() -> AddEditUiState) {
        _uiState.update { it.block().copy(isDirty = true) }
    }

    /** User tapped back (button or gesture) — show confirm dialog if dirty, else allow. */
    fun onBackPressed(): Boolean {
        return if (_uiState.value.isDirty && !_uiState.value.isSaved) {
            _uiState.update { it.copy(showDiscardDialog = true) }
            true  // consumed — do not navigate back yet
        } else {
            false // not dirty — let caller navigate back
        }
    }

    fun dismissDiscardDialog() { _uiState.update { it.copy(showDiscardDialog = false) } }

    fun confirmDiscard() { _uiState.update { it.copy(showDiscardDialog = false, isDirty = false, isSaved = true) } }

    // ── Airline autocomplete ──────────────────────────────────────────────

    fun onAirlineChange(value: String) {
        _uiState.update { it.copy(airline = value) }
        _airlineSuggestions.value = airlineSearch.search(value, airlineUsage)
    }

    fun onAirlineSuggestionSelected(iata: String, name: String) {
        _uiState.update { it.copy(airline = "$iata - $name") }
        _airlineSuggestions.value = emptyList()
    }

    fun dismissAirlineSuggestions() { _airlineSuggestions.value = emptyList() }

    // ── Airport autocomplete ──────────────────────────────────────────────

    fun onDepartureAirportChange(iata: String) {
        val tz = if (_uiState.value.departureTimezone.isBlank())
            airportTz.timezoneFor(iata) ?: _uiState.value.departureTimezone
        else _uiState.value.departureTimezone
        _uiState.update { it.copy(departureAirport = iata.uppercase(), departureTimezone = tz) }
        _depSuggestions.value = airportSearch.search(iata)
    }

    fun onDepartureSuggestionSelected(iata: String) {
        val tz = airportTz.timezoneFor(iata) ?: _uiState.value.departureTimezone
        _uiState.update { it.copy(departureAirport = iata, departureTimezone = tz) }
        _depSuggestions.value = emptyList()
    }

    fun onArrivalAirportChange(iata: String) {
        val tz = if (_uiState.value.arrivalTimezone.isBlank())
            airportTz.timezoneFor(iata) ?: _uiState.value.arrivalTimezone
        else _uiState.value.arrivalTimezone
        _uiState.update { it.copy(arrivalAirport = iata.uppercase(), arrivalTimezone = tz) }
        _arrSuggestions.value = airportSearch.search(iata)
    }

    fun onArrivalSuggestionSelected(iata: String) {
        val tz = airportTz.timezoneFor(iata) ?: _uiState.value.arrivalTimezone
        _uiState.update { it.copy(arrivalAirport = iata, arrivalTimezone = tz) }
        _arrSuggestions.value = emptyList()
    }

    // ── Timezone autocomplete ─────────────────────────────────────────────

    fun onDepTimezoneChange(value: String) {
        _uiState.update { it.copy(departureTimezone = value) }
        _depTzSuggestions.value = timezoneSearch.search(value, timezoneUsage)
    }

    fun onDepTimezoneSuggestionSelected(tz: String) {
        _uiState.update { it.copy(departureTimezone = tz) }
        _depTzSuggestions.value = emptyList()
    }

    fun onArrTimezoneChange(value: String) {
        _uiState.update { it.copy(arrivalTimezone = value) }
        _arrTzSuggestions.value = timezoneSearch.search(value, timezoneUsage)
    }

    fun onArrTimezoneSuggestionSelected(tz: String) {
        _uiState.update { it.copy(arrivalTimezone = tz) }
        _arrTzSuggestions.value = emptyList()
    }

    // ── Flight lookup (AirLabs) ───────────────────────────────────────────

    /** Called when airline + flight number + (optionally) date are set; fetches schedule data from AirLabs. */
    fun lookupFlight() {
        val s = _uiState.value
        val flightIata = buildFlightIata(s.airline, s.flightNumber)
        if (flightIata.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLookingUpFlight = true, lookupError = null) }
            val apiKey = settings.airLabsApiKey.first()
            if (apiKey.isBlank()) {
                _uiState.update { it.copy(
                    isLookingUpFlight = false,
                    lookupError = "Set your AirLabs API key in Settings (⋮ menu) to enable flight lookup",
                ) }
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                flightLookup.lookup(flightIata, apiKey, s.departureDate)
            }
            if (result != null) {
                val depTz = airportTz.timezoneFor(result.departureAirport) ?: s.departureTimezone
                val arrTz = airportTz.timezoneFor(result.arrivalAirport)   ?: s.arrivalTimezone
                _uiState.update { it.copy(
                    isLookingUpFlight = false,
                    departureAirport  = result.departureAirport,
                    arrivalAirport    = result.arrivalAirport,
                    // Only overwrite date/time if the API returned them (non-blank)
                    departureDate     = result.departureDate.ifBlank { it.departureDate },
                    departureTime     = result.departureTime.ifBlank { it.departureTime },
                    departureTimezone = depTz,
                    arrivalDate       = result.arrivalDate.ifBlank { it.arrivalDate },
                    arrivalTime       = result.arrivalTime.ifBlank { it.arrivalTime },
                    arrivalTimezone   = arrTz,
                    registration      = result.registration.ifBlank { it.registration },
                    planeModel        = result.aircraftModel.ifBlank { it.planeModel },
                ) }
            } else {
                val hint = if (s.departureDate.isBlank())
                    " Tip: set the departure date first for a better match."
                else
                    " The flight may not be in AirLabs\u2019 schedule window (live/next ~10 hours only)."
                _uiState.update { it.copy(
                    isLookingUpFlight = false,
                    lookupError = "Flight $flightIata not found.$hint",
                ) }
            }
        }
    }

    fun dismissLookupError() { _uiState.update { it.copy(lookupError = null) } }

    // ── Save ──────────────────────────────────────────────────────────────

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

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Extracts the IATA flight code from airline + flight number, e.g. "DL" + "123" → "DL123". */
    private fun buildFlightIata(airline: String, flightNumber: String): String {
        val num = flightNumber.trim().trimStart('0').ifBlank { return "" }
        // Airline field may be "DL - Delta Air Lines" or just "DL"
        val iata = airline.trim().take(2).uppercase()
        return if (iata.length == 2) "$iata$num" else ""
    }

    private fun String.toTzOrDefault(): TimeZone =
        if (isNotBlank()) try { TimeZone.of(this) } catch (_: Exception) { TimeZone.currentSystemDefault() }
        else TimeZone.currentSystemDefault()

    private fun LocalDateTime.formatDate() = "%04d-%02d-%02d".format(year, monthNumber, dayOfMonth)
    private fun LocalDateTime.formatTime() = "%02d:%02d".format(hour, minute)
}
