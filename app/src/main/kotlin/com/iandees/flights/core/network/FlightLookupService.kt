package com.iandees.flights.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches schedule data for a specific flight on a specific date from the AirLabs API.
 *
 * Endpoint: GET /v9/schedules?flight_iata={IATA_CODE}&api_key={KEY}
 *   https://airlabs.co/docs/schedules
 *
 * The schedules endpoint returns all currently-scheduled departures for that flight number
 * (up to ~10 hours ahead). We filter by [date] ("YYYY-MM-DD") against dep_time to find
 * the right day's flight — useful for flights on today or the next day.
 *
 * Limitation: AirLabs is a live/near-real-time API. Historical flights (more than a day ago)
 * are not available. For logging past flights, the lookup can still pre-fill the route
 * (dep/arr airports) from the /v9/routes DB if the schedule lookup misses.
 */
@Serializable
data class SchedulesResponse(
    val response: List<ScheduleInfo>? = null,
    val error: ErrorInfo? = null,
)

@Serializable
data class ScheduleInfo(
    @SerialName("dep_iata")      val depIata: String? = null,
    @SerialName("arr_iata")      val arrIata: String? = null,
    @SerialName("dep_time")      val depTime: String? = null,  // "YYYY-MM-DD HH:MM" local
    @SerialName("arr_time")      val arrTime: String? = null,  // "YYYY-MM-DD HH:MM" local
    @SerialName("dep_time_utc")  val depTimeUtc: String? = null,
    @SerialName("arr_time_utc")  val arrTimeUtc: String? = null,
    @SerialName("status")        val status: String? = null,
    @SerialName("flight_iata")   val flightIata: String? = null,
)

@Serializable
data class ErrorInfo(
    val message: String? = null,
)

data class FlightLookupResult(
    val departureAirport: String,
    val arrivalAirport: String,
    /** Local date portion "YYYY-MM-DD" */
    val departureDate: String,
    /** Local time portion "HH:MM" */
    val departureTime: String,
    val arrivalDate: String,
    val arrivalTime: String,
    val registration: String,
    val aircraftModel: String,
)

@Singleton
class FlightLookupService @Inject constructor() {

    private val client = OkHttpClient()
    private val json   = Json { ignoreUnknownKeys = true }

    /**
     * Looks up a flight by IATA code (e.g. "DL328") and local departure date ("YYYY-MM-DD").
     *
     * Strategy:
     *  1. Call /v9/schedules?flight_iata=... — returns all scheduled instances of that flight
     *     within AirLabs' ~10-hour lookahead window.
     *  2. Filter by [date] matching the start of dep_time. Pick the first match.
     *  3. If no date match (e.g. flight is historical), fall back to the first result and
     *     return it without overwriting departure date/time so the user's typed date is kept.
     *
     * Returns null if: API key blank, network failure, or no results at all.
     */
    fun lookup(flightIata: String, apiKey: String, date: String): FlightLookupResult? {
        if (apiKey.isBlank() || flightIata.isBlank()) return null
        return try {
            val url = "https://airlabs.co/api/v9/schedules" +
                "?flight_iata=${flightIata.trim()}" +
                "&api_key=${apiKey.trim()}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val parsed = json.decodeFromString<SchedulesResponse>(body)
                val rows = parsed.response ?: return null
                if (rows.isEmpty()) return null

                // Prefer a row whose dep_time starts with the requested date
                val match = if (date.isNotBlank()) {
                    rows.firstOrNull { it.depTime?.startsWith(date) == true }
                        ?: rows.first()  // fallback: closest scheduled instance
                } else {
                    rows.first()
                }

                val dep = match.depIata ?: return null
                val arr = match.arrIata ?: return null

                // If the matched row has a dep_time on a different date than requested,
                // don't overwrite the user's date/time — return blanks so caller keeps them.
                val matchedDate = match.depTime?.substringBefore(' ') ?: ""
                val useDatetime = date.isBlank() || matchedDate == date

                FlightLookupResult(
                    departureAirport = dep,
                    arrivalAirport   = arr,
                    departureDate    = if (useDatetime) matchedDate else date,
                    departureTime    = if (useDatetime) match.depTime?.substringAfter(' ') ?: "" else "",
                    arrivalDate      = if (useDatetime) match.arrTime?.substringBefore(' ') ?: "" else "",
                    arrivalTime      = if (useDatetime) match.arrTime?.substringAfter(' ') ?: "" else "",
                    registration     = "",   // not available in /schedules; caller may fill from /flight
                    aircraftModel    = "",
                )
            }
        } catch (_: Exception) { null }
    }
}
