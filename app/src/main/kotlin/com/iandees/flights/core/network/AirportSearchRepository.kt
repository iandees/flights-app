package com.iandees.flights.core.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AirportSuggestion(
    val iata: String,
    val label: String,   // e.g. "O'Hare International Airport (Chicago)"
)

/**
 * Loads iata_names.csv from assets (format: "IATA^Name (City)" per line).
 * Provides fast prefix/substring search for autocomplete.
 */
@Singleton
class AirportSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val airports: List<AirportSuggestion> by lazy {
        buildList {
            context.assets.open("iata_names.csv").bufferedReader().use { reader ->
                reader.lineSequence()
                    .filter { it.contains('^') }
                    .forEach { line ->
                        val idx = line.indexOf('^')
                        val iata  = line.substring(0, idx).trim()
                        val label = line.substring(idx + 1).trim()
                        if (iata.length == 3) add(AirportSuggestion(iata, label))
                    }
            }
        }
    }

    /**
     * Returns up to [limit] airports whose IATA code starts with [query] (case-insensitive),
     * followed by airports whose name/city contains [query].
     */
    fun search(query: String, limit: Int = 6): List<AirportSuggestion> {
        if (query.isBlank()) return emptyList()
        val q = query.uppercase().trim()
        val codeMatches = airports.filter { it.iata.startsWith(q) }
        val nameMatches = airports.filter {
            !it.iata.startsWith(q) && it.label.contains(query, ignoreCase = true)
        }
        return (codeMatches + nameMatches).take(limit)
    }
}
