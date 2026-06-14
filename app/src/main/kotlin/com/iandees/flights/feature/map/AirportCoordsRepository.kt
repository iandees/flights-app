package com.iandees.flights.feature.map

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Loads iata_coords.csv (IATA^lat^lon) from assets for great-circle arc rendering. */
@Singleton
class AirportCoordsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val coords: Map<String, Pair<Double, Double>> by lazy {
        buildMap {
            context.assets.open("iata_coords.csv").bufferedReader().use { reader ->
                reader.lineSequence().filter { it.contains('^') }.forEach { line ->
                    val parts = line.split('^')
                    if (parts.size == 3) {
                        val iata = parts[0].trim()
                        val lat  = parts[1].toDoubleOrNull() ?: return@forEach
                        val lon  = parts[2].toDoubleOrNull() ?: return@forEach
                        put(iata, lat to lon)
                    }
                }
            }
        }
    }
}
