package com.iandees.flights.core.database

import android.content.Context
import androidx.room.Room
import com.iandees.flights.core.database.dao.FlightDao
import com.iandees.flights.core.database.MIGRATION_1_2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlightsDatabase =
        Room.databaseBuilder(context, FlightsDatabase::class.java, FlightsDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideFlightDao(db: FlightsDatabase): FlightDao = db.flightDao()

    @Provides
    @Singleton
    fun provideFlightRepository(dao: FlightDao): FlightRepository = FlightRepository(dao)
}
