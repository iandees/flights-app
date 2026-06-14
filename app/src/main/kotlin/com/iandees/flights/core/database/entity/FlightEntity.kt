package com.iandees.flights.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flights")
data class FlightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Route
    val airline: String,
    @ColumnInfo(name = "flight_number") val flightNumber: String,
    @ColumnInfo(name = "departure_airport") val departureAirport: String,
    @ColumnInfo(name = "arrival_airport") val arrivalAirport: String,
    @ColumnInfo(name = "departure_time_ms") val departureTimeMs: Long?,   // epoch millis
    @ColumnInfo(name = "arrival_time_ms") val arrivalTimeMs: Long?,       // epoch millis

    // Booking
    @ColumnInfo(name = "record_locator") val recordLocator: String,
    @ColumnInfo(name = "ticket_number") val ticketNumber: String,

    // Seat
    val seat: String,
    @ColumnInfo(name = "boarding_group") val boardingGroup: String,
    @ColumnInfo(name = "seat_class") val seatClass: String,

    // Aircraft
    @ColumnInfo(name = "plane_model") val planeModel: String,
    val registration: String,

    // Miles
    val mqm: Int?,
    val mqs: Int?,
    val mqd: Int?,
    @ColumnInfo(name = "award_miles") val awardMiles: Int?,

    // Notes
    val notes: String,

    // IANA timezone IDs
    @ColumnInfo(name = "departure_timezone") val departureTimezone: String,
    @ColumnInfo(name = "arrival_timezone") val arrivalTimezone: String,
)
