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
     * Results are sorted: exact IATA-code match first, then prefix matches, then name matches.
     * Within each group, airlines that appear more often in the DB float to the top via [usageCounts].
     */
    fun search(query: String, usageCounts: Map<String, Int>, limit: Int = 8): List<AirlineSuggestion> {
        if (query.isBlank()) {
            // No query — return most-used airlines
            return airlines
                .sortedByDescending { usageCounts[it.iata] ?: 0 }
                .take(limit)
        }
        val q = query.uppercase().trim()
        val exact   = airlines.filter { it.iata == q }
        val prefix  = airlines.filter { it.iata.startsWith(q) && it.iata != q }
        val nameHit = airlines.filter {
            !it.iata.startsWith(q) && it.name.contains(query, ignoreCase = true)
        }
        return (exact + prefix + nameHit)
            .sortedByDescending { usageCounts[it.iata] ?: 0 }
            .take(limit)
    }
}
