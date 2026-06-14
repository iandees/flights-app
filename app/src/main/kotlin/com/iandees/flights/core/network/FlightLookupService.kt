package com.iandees.flights.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Subset of the AirLabs Flight Info API response we care about.
 * https://airlabs.co/docs/flight
 *
 * Call: GET https://airlabs.co/api/v9/flight?flight_iata={IATA_CODE}&api_key={KEY}
 *
 * Returns the nearest scheduled/live/landed flight for that flight number.
 * dep_time and arr_time are in the airport's local timezone ("YYYY-MM-DD HH:MM").
 */
@Serializable
data class FlightLookupResponse(
    val response: FlightInfo? = null,
    val error: ErrorInfo? = null,
)

@Serializable
data class FlightInfo(
    @SerialName("dep_iata")  val depIata: String? = null,
    @SerialName("arr_iata")  val arrIata: String? = null,
    @SerialName("dep_time")  val depTime: String? = null,   // "YYYY-MM-DD HH:MM" local
    @SerialName("arr_time")  val arrTime: String? = null,   // "YYYY-MM-DD HH:MM" local
    @SerialName("reg_number") val registration: String? = null,
    @SerialName("model")     val aircraftModel: String? = null,
    @SerialName("airline_iata") val airlineIata: String? = null,
    @SerialName("flight_number") val flightNumber: String? = null,
    @SerialName("status")    val status: String? = null,
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
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Looks up flight data by IATA flight code (e.g. "DL123") using the AirLabs API.
     * Returns null if the API key is blank, the flight isn't found, or the network fails.
     */
    fun lookup(flightIata: String, apiKey: String): FlightLookupResult? {
        if (apiKey.isBlank() || flightIata.isBlank()) return null
        return try {
            val url = "https://airlabs.co/api/v9/flight?flight_iata=${flightIata.trim()}&api_key=${apiKey.trim()}"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val parsed = json.decodeFromString<FlightLookupResponse>(body)
                val info = parsed.response ?: return null
                FlightLookupResult(
                    departureAirport = info.depIata ?: return null,
                    arrivalAirport   = info.arrIata ?: return null,
                    departureDate    = info.depTime?.substringBefore(' ') ?: "",
                    departureTime    = info.depTime?.substringAfter(' ') ?: "",
                    arrivalDate      = info.arrTime?.substringBefore(' ') ?: "",
                    arrivalTime      = info.arrTime?.substringAfter(' ') ?: "",
                    registration     = info.registration ?: "",
                    aircraftModel    = info.aircraftModel ?: "",
                )
            }
        } catch (_: Exception) { null }
    }
}
