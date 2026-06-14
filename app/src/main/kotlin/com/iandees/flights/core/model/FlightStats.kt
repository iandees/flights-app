package com.iandees.flights.core.model

/** Aggregated stats computed from all flights. */
data class FlightStats(
    val totalSegments: Int,
    val segmentsByYear: Map<Int, Int>,
    val segmentsByAirline: Map<String, Int>,
    val segmentsByPlaneModel: Map<String, Int>,
    val segmentsBySeatClass: Map<String, Int>,
    val topDepartureAirports: List<Pair<String, Int>>,
    val topArrivalAirports: List<Pair<String, Int>>,
    val topRoutes: List<Triple<String, String, Int>>,   // from, to, count
    val totalMqm: Int,
    val totalMqs: Int,
    val totalMqd: Int,
    val totalAwardMiles: Int,
)
