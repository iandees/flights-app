package com.iandees.flights.core.network

import com.iandees.flights.core.model.Flight
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.InputStream
import java.io.InputStreamReader
import java.time.format.DateTimeParseException

/**
 * Parses a CSV exported from Google Sheets.
 *
 * Expected headers (case-insensitive, extra columns ignored):
 *   Airline | From | To | Departure | Arrival | Record Loc | Ticket # | Flight No |
 *   Seat | Group | Class | Plane | Registration | MQM | MQS | MQD | Award Miles | Notes
 */
object CsvImporter {

    // Candidate date/time formats seen in Google Sheets exports
    private val DATE_TIME_FORMATS = listOf(
        "M/d/yyyy H:mm",
        "M/d/yyyy HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "M/d/yyyy",
        "yyyy-MM-dd",
    )

    data class ImportResult(
        val flights: List<Flight>,
        val skippedRows: Int,
        val errors: List<String>,
    )

    fun parse(inputStream: InputStream, timeZone: TimeZone = TimeZone.UTC): ImportResult {
        val flights = mutableListOf<Flight>()
        val errors = mutableListOf<String>()
        var skipped = 0

        val format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build()

        CSVParser(InputStreamReader(inputStream, Charsets.UTF_8), format).use { parser ->
            for ((index, record) in parser.withIndex()) {
                try {
                    val airline    = record.getOrEmpty("Airline")
                    val from       = record.getOrEmpty("From").uppercase()
                    val to         = record.getOrEmpty("To").uppercase()
                    val departure  = record.getOrEmpty("Departure").parseInstant(timeZone)
                    val arrival    = record.getOrEmpty("Arrival").parseInstant(timeZone)
                    val recordLoc  = record.getOrEmpty("Record Loc")
                    val ticketNum  = record.getOrEmpty("Ticket #")
                    val flightNo   = record.getOrEmpty("Flight No")
                    val seat       = record.getOrEmpty("Seat")
                    val group      = record.getOrEmpty("Group")
                    val seatClass  = record.getOrEmpty("Class")
                    val plane      = record.getOrEmpty("Plane")
                    val reg        = record.getOrEmpty("Registration")
                    val mqm        = record.getOrEmpty("MQM").toIntOrNull()
                    val mqs        = record.getOrEmpty("MQS").toIntOrNull()
                    val mqd        = record.getOrEmpty("MQD").toIntOrNull()
                    val award      = record.getOrEmpty("Award Miles").toIntOrNull()
                    val notes      = record.getOrEmpty("Notes")

                    // Skip rows that have no meaningful data
                    if (airline.isBlank() && flightNo.isBlank() && from.isBlank()) {
                        skipped++
                        continue
                    }

                    flights.add(
                        Flight(
                            airline          = airline,
                            flightNumber     = flightNo,
                            departureAirport = from,
                            arrivalAirport   = to,
                            departureTime    = departure,
                            arrivalTime      = arrival,
                            recordLocator    = recordLoc,
                            ticketNumber     = ticketNum,
                            seat             = seat,
                            boardingGroup    = group,
                            seatClass        = seatClass,
                            planeModel       = plane,
                            registration     = reg,
                            mqm              = mqm,
                            mqs              = mqs,
                            mqd              = mqd,
                            awardMiles       = award,
                            notes            = notes,
                            departureTimezone = "",
                            arrivalTimezone   = "",
                        )
                    )
                } catch (e: Exception) {
                    errors.add("Row ${index + 2}: ${e.message}")
                    skipped++
                }
            }
        }

        return ImportResult(flights = flights, skippedRows = skipped, errors = errors)
    }

    private fun org.apache.commons.csv.CSVRecord.getOrEmpty(header: String): String =
        if (isMapped(header)) get(header) ?: "" else ""

    private fun String.parseInstant(tz: TimeZone): Instant? {
        if (isBlank()) return null
        for (pattern in DATE_TIME_FORMATS) {
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
                return try {
                    val ldt = java.time.LocalDateTime.parse(this, formatter)
                    LocalDateTime(
                        ldt.year, ldt.monthValue, ldt.dayOfMonth,
                        ldt.hour, ldt.minute, ldt.second
                    ).toInstant(tz)
                } catch (_: DateTimeParseException) {
                    val ld = java.time.LocalDate.parse(this, formatter)
                    LocalDateTime(ld.year, ld.monthValue, ld.dayOfMonth, 0, 0, 0).toInstant(tz)
                }
            } catch (_: Exception) { /* try next format */ }
        }
        return null // unparseable date — stored as null, shown in UI as unknown
    }
}
