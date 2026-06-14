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

    val arrivalDate: String = "",
    val arrivalTime: String = "",

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

    // Usage counts from existing flights (computed once)
    private var airlineUsage: Map<String, Int> = emptyMap()

    init {
        viewModelScope.launch {
            repository.getAllFlights().first().let { flights ->
                airlineUsage = flights.groupingBy { it.airline }.eachCount()
            }
        }
    }

    fun loadFlight(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val flight = repository.getFlightById(id)
            if (flight != null) {
                // Reconstruct local date/time using the stored timezone so the user
                // sees the time as they originally entered it (local to the airport).
                val depTz = flight.departureTimezone.toTzOrDefault()
                val arrTz = flight.arrivalTimezone.toTzOrDefault()
                val depLdt = flight.departureTime?.toLocalDateTime(depTz)
                val arrLdt = flight.arrivalTime?.toLocalDateTime(arrTz)
                _uiState.update { _ ->
                    AddEditUiState(
                        isLoading        = false,
                        airline          = flight.airline,
                        flightNumber     = flight.flightNumber,
                        departureAirport = flight.departureAirport,
                        arrivalAirport   = flight.arrivalAirport,
                        departureDate    = depLdt?.formatDate() ?: "",
                        departureTime    = depLdt?.formatTime() ?: "",
                        arrivalDate      = arrLdt?.formatDate() ?: "",
                        arrivalTime      = arrLdt?.formatTime() ?: "",
                        recordLocator    = flight.recordLocator,
                        ticketNumber     = flight.ticketNumber,
                        seat             = flight.seat,
                        boardingGroup    = flight.boardingGroup,
                        seatClass        = flight.seatClass,
                        planeModel       = flight.planeModel,
                        registration     = flight.registration,
                        mqm              = flight.mqm?.toString() ?: "",
                        mqs              = flight.mqs?.toString() ?: "",
                        mqd              = flight.mqd?.toString() ?: "",
                        awardMiles       = flight.awardMiles?.toString() ?: "",
                        notes            = flight.notes,
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
        _uiState.update { it.copy(airline = value, isDirty = true) }
        _airlineSuggestions.value = airlineSearch.search(value, airlineUsage)
    }

    fun onAirlineSuggestionSelected(iata: String, name: String) {
        _uiState.update { it.copy(airline = "$iata - $name", isDirty = true) }
        _airlineSuggestions.value = emptyList()
    }

    fun dismissAirlineSuggestions() { _airlineSuggestions.value = emptyList() }

    // ── Airport autocomplete ──────────────────────────────────────────────

    fun onDepartureAirportChange(iata: String) {
        _uiState.update { it.copy(departureAirport = iata.uppercase(), isDirty = true) }
        _depSuggestions.value = airportSearch.search(iata)
    }

    fun onDepartureSuggestionSelected(iata: String) {
        _uiState.update { it.copy(departureAirport = iata, isDirty = true) }
        _depSuggestions.value = emptyList()
    }

    fun onArrivalAirportChange(iata: String) {
        _uiState.update { it.copy(arrivalAirport = iata.uppercase(), isDirty = true) }
        _arrSuggestions.value = airportSearch.search(iata)
    }

    fun onArrivalSuggestionSelected(iata: String) {
        _uiState.update { it.copy(arrivalAirport = iata, isDirty = true) }
        _arrSuggestions.value = emptyList()
    }

    // ── Flight lookup (AirLabs) ───────────────────────────────────────────

    /** Called when airline + flight number + departure date are set. */
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
                _uiState.update { it.copy(
                    isLookingUpFlight = false,
                    isDirty           = true,
                    departureAirport  = result.departureAirport,
                    arrivalAirport    = result.arrivalAirport,
                    departureDate     = result.departureDate.ifBlank { it.departureDate },
                    departureTime     = result.departureTime.ifBlank { it.departureTime },
                    arrivalDate       = result.arrivalDate.ifBlank { it.arrivalDate },
                    arrivalTime       = result.arrivalTime.ifBlank { it.arrivalTime },
                    registration      = result.registration.ifBlank { it.registration },
                    planeModel        = result.aircraftModel.ifBlank { it.planeModel },
                ) }
            } else {
                val hint = if (s.departureDate.isBlank())
                    " Tip: set the departure date first for a better match."
                else
                    " The flight may not be in AirLabs' schedule window (live/next ~10 hours only)."
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
            val depAirport = s.departureAirport.trim().uppercase()
            val arrAirport = s.arrivalAirport.trim().uppercase()

            // Timezone is always derived from the airport code — never user input.
            // Fall back to UTC if the airport isn't in our asset.
            val depTzId = airportTz.timezoneFor(depAirport) ?: "UTC"
            val arrTzId = airportTz.timezoneFor(arrAirport) ?: "UTC"

            fun parseInstant(date: String, time: String, tzId: String): Instant? {
                if (date.isBlank()) return null
                return try {
                    val tz = TimeZone.of(tzId)
                    val parts = date.split("-").map { it.toInt() }
                    val timeParts = if (time.isBlank()) listOf(0, 0)
                                    else time.split(":").map { it.toInt() }
                    LocalDateTime(parts[0], parts[1], parts[2],
                                  timeParts[0], timeParts.getOrElse(1) { 0 })
                        .toInstant(tz)
                } catch (_: Exception) { null }
            }

            val flight = Flight(
                id               = existingId ?: 0,
                airline          = s.airline.trim(),
                flightNumber     = s.flightNumber.trim(),
                departureAirport = depAirport,
                arrivalAirport   = arrAirport,
                departureTime    = parseInstant(s.departureDate, s.departureTime, depTzId),
                arrivalTime      = parseInstant(s.arrivalDate,   s.arrivalTime,   arrTzId),
                departureTimezone = depTzId,
                arrivalTimezone   = arrTzId,
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

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Extracts the IATA flight code, e.g. "DL" + "123" → "DL123". */
    private fun buildFlightIata(airline: String, flightNumber: String): String {
        val num = flightNumber.trim().trimStart('0').ifBlank { return "" }
        val iata = airline.trim().take(2).uppercase()
        return if (iata.length == 2) "$iata$num" else ""
    }

    private fun String.toTzOrDefault(): TimeZone =
        if (isNotBlank()) try { TimeZone.of(this) } catch (_: Exception) { TimeZone.currentSystemDefault() }
        else TimeZone.currentSystemDefault()

    private fun LocalDateTime.formatDate() = "%04d-%02d-%02d".format(year, monthNumber, dayOfMonth)
    private fun LocalDateTime.formatTime() = "%02d:%02d".format(hour, minute)
}
