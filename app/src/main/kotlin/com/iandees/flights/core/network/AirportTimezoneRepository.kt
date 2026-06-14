package com.iandees.flights.core.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads iata_tz.csv from assets (format: "IATA^IANA_TZ" per line) and provides
 * O(1) lookup of the IANA timezone ID for a given IATA airport code.
 *
 * Example: "ATL" → "America/New_York"
 */
@Singleton
class AirportTimezoneRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val map: Map<String, String> by lazy {
        buildMap {
            context.assets.open("iata_tz.csv").bufferedReader().use { reader ->
                reader.lineSequence()
                    .filter { it.contains('^') }
                    .forEach { line ->
                        val idx = line.indexOf('^')
                        val iata = line.substring(0, idx).trim()
                        val tz   = line.substring(idx + 1).trim()
                        if (iata.isNotEmpty() && tz.isNotEmpty()) put(iata, tz)
                    }
            }
        }
    }

    /** Returns the IANA timezone ID for [iataCode], or null if unknown. */
    fun timezoneFor(iataCode: String): String? = map[iataCode.uppercase()]
}
