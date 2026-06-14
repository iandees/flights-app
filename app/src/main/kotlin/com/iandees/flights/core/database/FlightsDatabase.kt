package com.iandees.flights.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iandees.flights.core.database.dao.FlightDao
import com.iandees.flights.core.database.entity.FlightEntity

@Database(
    entities = [FlightEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class FlightsDatabase : RoomDatabase() {
    abstract fun flightDao(): FlightDao

    companion object {
        const val DATABASE_NAME = "flights.db"
    }
}
