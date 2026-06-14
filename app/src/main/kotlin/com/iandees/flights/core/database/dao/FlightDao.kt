package com.iandees.flights.core.database.dao

import androidx.room.*
import com.iandees.flights.core.database.entity.FlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightDao {

    @Query("SELECT * FROM flights ORDER BY departure_time_ms DESC")
    fun getAllFlights(): Flow<List<FlightEntity>>

    @Query("SELECT * FROM flights WHERE id = :id")
    suspend fun getFlightById(id: Long): FlightEntity?

    @Query("""
        SELECT * FROM flights
        WHERE (:query = '' OR
               airline LIKE '%' || :query || '%' OR
               flight_number LIKE '%' || :query || '%' OR
               departure_airport LIKE '%' || :query || '%' OR
               arrival_airport LIKE '%' || :query || '%' OR
               plane_model LIKE '%' || :query || '%' OR
               registration LIKE '%' || :query || '%' OR
               notes LIKE '%' || :query || '%')
        ORDER BY departure_time_ms DESC
    """)
    fun searchFlights(query: String): Flow<List<FlightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlight(flight: FlightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlights(flights: List<FlightEntity>)

    @Update
    suspend fun updateFlight(flight: FlightEntity)

    @Delete
    suspend fun deleteFlight(flight: FlightEntity)

    @Query("DELETE FROM flights WHERE id = :id")
    suspend fun deleteFlightById(id: Long)

    // --- Stats queries ---

    @Query("SELECT COUNT(*) FROM flights")
    suspend fun getTotalCount(): Int

    @Query("SELECT DISTINCT airline FROM flights ORDER BY airline ASC")
    suspend fun getDistinctAirlines(): List<String>

    @Query("SELECT DISTINCT plane_model FROM flights WHERE plane_model != '' ORDER BY plane_model ASC")
    suspend fun getDistinctPlaneModels(): List<String>

    @Query("SELECT DISTINCT departure_airport FROM flights ORDER BY departure_airport ASC")
    suspend fun getDistinctDepartureAirports(): List<String>

    @Query("SELECT DISTINCT arrival_airport FROM flights ORDER BY arrival_airport ASC")
    suspend fun getDistinctArrivalAirports(): List<String>
}
