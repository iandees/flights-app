package com.iandees.flights.core.model

import kotlinx.datetime.Instant

/**
 * Pure domain model for a flight segment — no Android/Room dependencies.
 * Maps directly to the Google Sheets columns:
 *   Airline | From | To | Departure | Arrival | Record Loc | Ticket # | Flight No |
 *   Seat | Group | Class | Plane | Registration | MQM | MQS | MQD | Award Miles | Notes
 */
data class Flight(
    val id: Long = 0,

    // Route
    val airline: String,
    val flightNumber: String,
    val departureAirport: String,       // IATA code, e.g. "ORD"
    val arrivalAirport: String,         // IATA code, e.g. "LAX"
    val departureTime: Instant?,
    val arrivalTime: Instant?,

    // Booking
    val recordLocator: String,          // "Record Loc" / PNR
    val ticketNumber: String,           // "Ticket #"

    // Seat
    val seat: String,                   // e.g. "12A"
    val boardingGroup: String,          // "Group"
    val seatClass: String,              // "Class" — e.g. "Y", "J", "F", "W"

    // Aircraft
    val planeModel: String,             // "Plane" column — aircraft model name, e.g. "Boeing 737-800"
    val registration: String,           // "Registration" column — aircraft tail number / N-code, e.g. "N12345"

    // Elite miles (Delta/airline loyalty program columns)
    val mqm: Int?,                      // Medallion Qualifying Miles
    val mqs: Int?,                      // Medallion Qualifying Segments
    val mqd: Int?,                      // Medallion Qualifying Dollars
    val awardMiles: Int?,               // Award Miles earned

    // Free-form
    val notes: String,

    // IANA timezone ID for departure/arrival (e.g. "America/Chicago")
    val departureTimezone: String,
    val arrivalTimezone: String,
)
