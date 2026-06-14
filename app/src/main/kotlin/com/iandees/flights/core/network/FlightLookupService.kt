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
 * Strategy:
 *  1. GET /v9/schedules?flight_iata=... — route + times (up to ~10h ahead)
 *  2. GET /v9/flight?flight_iata=...    — registration + aircraft model (live/nearest)
 *
 * Both calls are made in parallel. Results are merged: schedules wins for route/times,
 * flight-info wins for registration and model.
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
data class FlightInfoResponse(
    val response: FlightInfo? = null,
    val error: ErrorInfo? = null,
)

@Serializable
data class FlightInfo(
    @SerialName("reg_number")  val registration: String? = null,
    @SerialName("model")       val model: String? = null,
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
     * Makes two parallel calls:
     *  - /v9/schedules for route + times, filtered to [date]
     *  - /v9/flight    for registration + aircraft model (nearest live flight)
     *
     * Returns null if the API key is blank, the network fails, or schedules returns no rows.
     */
    fun lookup(flightIata: String, apiKey: String, date: String): FlightLookupResult? {
        if (apiKey.isBlank() || flightIata.isBlank()) return null
        val iata = flightIata.trim()
        val key  = apiKey.trim()
        return try {
            // Fire both requests; use OkHttp's connection pool for parallelism
            val schedCall = client.newCall(
                Request.Builder()
                    .url("https://airlabs.co/api/v9/schedules?flight_iata=$iata&api_key=$key")
                    .build()
            )
            val infoCall = client.newCall(
                Request.Builder()
                    .url("https://airlabs.co/api/v9/flight?flight_iata=$iata&api_key=$key")
                    .build()
            )

            // Execute schedules (required)
            val schedResult = schedCall.execute().use { r ->
                if (!r.isSuccessful) return null
                json.decodeFromString<SchedulesResponse>(r.body?.string() ?: return null)
            }
            val rows = schedResult.response ?: return null
            if (rows.isEmpty()) return null

            val match = if (date.isNotBlank()) {
                rows.firstOrNull { it.depTime?.startsWith(date) == true } ?: rows.first()
            } else {
                rows.first()
            }

            val dep = match.depIata ?: return null
            val arr = match.arrIata ?: return null
            val matchedDate = match.depTime?.substringBefore(' ') ?: ""
            val useDatetime = date.isBlank() || matchedDate == date

            // Execute flight-info (optional — swallow errors)
            val flightInfo = try {
                infoCall.execute().use { r ->
                    if (r.isSuccessful)
                        json.decodeFromString<FlightInfoResponse>(r.body?.string() ?: "").response
                    else null
                }
            } catch (_: Exception) { null }

            FlightLookupResult(
                departureAirport = dep,
                arrivalAirport   = arr,
                departureDate    = if (useDatetime) matchedDate else date,
                departureTime    = if (useDatetime) match.depTime?.substringAfter(' ') ?: "" else "",
                arrivalDate      = if (useDatetime) match.arrTime?.substringBefore(' ') ?: "" else "",
                arrivalTime      = if (useDatetime) match.arrTime?.substringAfter(' ') ?: "" else "",
                registration     = flightInfo?.registration.orEmpty(),
                aircraftModel    = flightInfo?.model.orEmpty(),
            )
        } catch (_: Exception) { null }
    }
}
