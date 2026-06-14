package com.iandees.flights.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.TimeZone

/**
 * Provides timezone autocomplete suggestions from the device's IANA timezone database,
 * sorted so most-frequently-used timezones appear first.
 */
@Singleton
class TimezoneSearchRepository @Inject constructor() {

    private val allZones: List<String> by lazy {
        TimeZone.availableZoneIds.sorted()
    }

    fun search(query: String, usageCounts: Map<String, Int>, limit: Int = 8): List<String> {
        if (query.isBlank()) {
            return allZones.sortedByDescending { usageCounts[it] ?: 0 }.take(limit)
        }
        val q = query.trim()
        val prefixMatches = allZones.filter { it.startsWith(q, ignoreCase = true) }
        val otherMatches  = allZones.filter {
            !it.startsWith(q, ignoreCase = true) && it.contains(q, ignoreCase = true)
        }
        return (prefixMatches + otherMatches)
            .sortedByDescending { usageCounts[it] ?: 0 }
            .take(limit)
    }
}
