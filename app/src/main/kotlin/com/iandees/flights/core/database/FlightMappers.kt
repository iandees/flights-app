package com.iandees.flights.core.database

import com.iandees.flights.core.database.entity.FlightEntity
import com.iandees.flights.core.model.Flight
import kotlinx.datetime.Instant

fun FlightEntity.toDomain() = Flight(
    id = id,
    airline = airline,
    flightNumber = flightNumber,
    departureAirport = departureAirport,
    arrivalAirport = arrivalAirport,
    departureTime = departureTimeMs?.let { Instant.fromEpochMilliseconds(it) },
    arrivalTime = arrivalTimeMs?.let { Instant.fromEpochMilliseconds(it) },
    recordLocator = recordLocator,
    ticketNumber = ticketNumber,
    seat = seat,
    boardingGroup = boardingGroup,
    seatClass = seatClass,
    planeModel = planeModel,
    registration = registration,
    mqm = mqm,
    mqs = mqs,
    mqd = mqd,
    awardMiles = awardMiles,
    notes = notes,
)

fun Flight.toEntity() = FlightEntity(
    id = id,
    airline = airline,
    flightNumber = flightNumber,
    departureAirport = departureAirport,
    arrivalAirport = arrivalAirport,
    departureTimeMs = departureTime?.toEpochMilliseconds(),
    arrivalTimeMs = arrivalTime?.toEpochMilliseconds(),
    recordLocator = recordLocator,
    ticketNumber = ticketNumber,
    seat = seat,
    boardingGroup = boardingGroup,
    seatClass = seatClass,
    planeModel = planeModel,
    registration = registration,
    mqm = mqm,
    mqs = mqs,
    mqd = mqd,
    awardMiles = awardMiles,
    notes = notes,
)
