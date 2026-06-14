package com.iandees.flights.core.database

import com.iandees.flights.core.database.dao.FlightDao
import com.iandees.flights.core.model.Flight
import com.iandees.flights.core.model.FlightStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FlightRepository(
    private val dao: FlightDao,
) {
    fun getAllFlights(): Flow<List<Flight>> =
        dao.getAllFlights().map { list -> list.map { it.toDomain() } }

    fun searchFlights(query: String): Flow<List<Flight>> =
        dao.searchFlights(query.trim()).map { list -> list.map { it.toDomain() } }

    suspend fun getFlightById(id: Long): Flight? =
        dao.getFlightById(id)?.toDomain()

    suspend fun saveFlight(flight: Flight): Long =
        dao.insertFlight(flight.toEntity())

    suspend fun updateFlight(flight: Flight) =
        dao.updateFlight(flight.toEntity())

    suspend fun deleteFlight(id: Long) =
        dao.deleteFlightById(id)

    suspend fun importFlights(flights: List<Flight>) =
        dao.insertFlights(flights.map { it.toEntity() })

    suspend fun computeStats(): FlightStats {
        val snapshot = dao.getAllFlights().first().map { it.toDomain() }

        val tz = TimeZone.currentSystemDefault()

        val byYear = snapshot
            .mapNotNull { it.departureTime?.toLocalDateTime(tz)?.year }
            .groupingBy { it }.eachCount()

        val byAirline = snapshot
            .filter { it.airline.isNotBlank() }
            .groupingBy { it.airline }.eachCount()

        val byPlane = snapshot
            .filter { it.planeModel.isNotBlank() }
            .groupingBy { it.planeModel }.eachCount()

        val byClass = snapshot
            .filter { it.seatClass.isNotBlank() }
            .groupingBy { it.seatClass }.eachCount()

        val topDep = snapshot
            .filter { it.departureAirport.isNotBlank() }
            .groupingBy { it.departureAirport }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(10).map { it.key to it.value }

        val topArr = snapshot
            .filter { it.arrivalAirport.isNotBlank() }
            .groupingBy { it.arrivalAirport }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(10).map { it.key to it.value }

        val topRoutes = snapshot
            .filter { it.departureAirport.isNotBlank() && it.arrivalAirport.isNotBlank() }
            .groupingBy { it.departureAirport to it.arrivalAirport }.eachCount()
            .entries.sortedByDescending { it.value }
            .take(10).map { Triple(it.key.first, it.key.second, it.value) }

        return FlightStats(
            totalSegments = snapshot.size,
            segmentsByYear = byYear,
            segmentsByAirline = byAirline,
            segmentsByPlaneModel = byPlane,
            segmentsBySeatClass = byClass,
            topDepartureAirports = topDep,
            topArrivalAirports = topArr,
            topRoutes = topRoutes,
            totalMqm = snapshot.sumOf { it.mqm ?: 0 },
            totalMqs = snapshot.sumOf { it.mqs ?: 0 },
            totalMqd = snapshot.sumOf { it.mqd ?: 0 },
            totalAwardMiles = snapshot.sumOf { it.awardMiles ?: 0 },
        )
    }
}
