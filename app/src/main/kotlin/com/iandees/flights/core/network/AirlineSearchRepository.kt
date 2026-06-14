package com.iandees.flights.core.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AirlineSuggestion(
    val iata: String,   // 2-char IATA code, e.g. "DL"
    val name: String,   // e.g. "Delta Air Lines"
)

/**
 * Loads airline_names.csv from assets (format: "IATA^Name" per line).
 * Provides prefix/substring search, with most-frequently-flown airlines ranked first.
 */
@Singleton
class AirlineSearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val airlines: List<AirlineSuggestion> by lazy {
        buildList {
            context.assets.open("airline_names.csv").bufferedReader().use { reader ->
                reader.lineSequence()
                    .filter { it.contains('^') }
                    .forEach { line ->
                        val idx = line.indexOf('^')
                        val iata = line.substring(0, idx).trim()
                        val name = line.substring(idx + 1).trim()
                        if (iata.isNotEmpty() && name.isNotEmpty()) add(AirlineSuggestion(iata, name))
                    }
            }
        }
    }

    /**
     * Returns up to [limit] suggestions for [query].
     * The first 11 entries in the file are the preferred US majors — they always
     * rank above everything else when there is no better match from [usageCounts].
     */
    fun search(query: String, usageCounts: Map<String, Int>, limit: Int = 8): List<AirlineSuggestion> {
        // Give the top-11 US majors a synthetic usage boost so they float up
        val preferredBoost = airlines.take(11).associate { it.iata to 10_000 }
        val combined = { iata: String -> (usageCounts[iata] ?: 0) + (preferredBoost[iata] ?: 0) }

        if (query.isBlank()) {
            return airlines.sortedByDescending { combined(it.iata) }.take(limit)
        }
        val q = query.uppercase().trim()
        val exact   = airlines.filter { it.iata == q }
        val prefix  = airlines.filter { it.iata.startsWith(q) && it.iata != q }
        val nameHit = airlines.filter {
            !it.iata.startsWith(q) && it.name.contains(query, ignoreCase = true)
        }
        return (exact + prefix + nameHit)
            .sortedByDescending { combined(it.iata) }
            .take(limit)
    }
}
