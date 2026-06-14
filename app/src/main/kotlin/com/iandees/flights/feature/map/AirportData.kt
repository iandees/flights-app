package com.iandees.flights.feature.map

import kotlin.math.*

/**
 * Airport coordinates for common IATA codes.
 * Extend this map as needed; the importer will silently skip airports it doesn't know.
 *
 * Coordinates are (latitude, longitude) in decimal degrees.
 */
val AIRPORT_COORDS: Map<String, Pair<Double, Double>> = mapOf(
    "ATL" to (33.6407 to -84.4277),
    "LAX" to (33.9425 to -118.4081),
    "ORD" to (41.9742 to -87.9073),
    "DFW" to (32.8998 to -97.0403),
    "DEN" to (39.8561 to -104.6737),
    "JFK" to (40.6413 to -73.7781),
    "SFO" to (37.6213 to -122.3790),
    "SEA" to (47.4502 to -122.3088),
    "LAS" to (36.0840 to -115.1537),
    "MCO" to (28.4294 to -81.3089),
    "EWR" to (40.6895 to -74.1745),
    "MIA" to (25.7959 to -80.2870),
    "PHX" to (33.4373 to -112.0078),
    "IAH" to (29.9902 to -95.3368),
    "BOS" to (42.3656 to -71.0096),
    "MSP" to (44.8848 to -93.2223),
    "DTW" to (42.2162 to -83.3554),
    "FLL" to (26.0726 to -80.1527),
    "LGA" to (40.7769 to -73.8740),
    "BWI" to (39.1754 to -76.6682),
    "DCA" to (38.8512 to -77.0402),
    "IAD" to (38.9531 to -77.4565),
    "MDW" to (41.7868 to -87.7522),
    "SAN" to (32.7338 to -117.1933),
    "TPA" to (27.9755 to -82.5332),
    "PDX" to (45.5898 to -122.5951),
    "SLC" to (40.7884 to -111.9778),
    "STL" to (38.7487 to -90.3700),
    "HNL" to (21.3245 to -157.9251),
    "OAK" to (37.7213 to -122.2208),
    "SJC" to (37.3626 to -121.9290),
    "AUS" to (30.1975 to -97.6664),
    "BNA" to (36.1245 to -86.6782),
    "CLT" to (35.2140 to -80.9431),
    "CLE" to (41.4117 to -81.8498),
    "CMH" to (39.9980 to -82.8919),
    "CVG" to (39.0488 to -84.6678),
    "IND" to (39.7173 to -86.2944),
    "JAX" to (30.4941 to -81.6879),
    "MCI" to (39.2976 to -94.7139),
    "MKE" to (42.9472 to -87.8966),
    "MSY" to (29.9934 to -90.2580),
    "PHL" to (39.8744 to -75.2424),
    "PIT" to (40.4915 to -80.2329),
    "RDU" to (35.8776 to -78.7875),
    "RSW" to (26.5362 to -81.7552),
    "SAT" to (29.5337 to -98.4698),
    "SMF" to (38.6954 to -121.5908),
    // International
    "LHR" to (51.4700 to -0.4543),
    "CDG" to (49.0097 to 2.5479),
    "AMS" to (52.3086 to 4.7639),
    "FRA" to (50.0333 to 8.5706),
    "NRT" to (35.7647 to 140.3864),
    "HND" to (35.5494 to 139.7798),
    "ICN" to (37.4602 to 126.4407),
    "PEK" to (40.0799 to 116.6031),
    "HKG" to (22.3080 to 113.9185),
    "SYD" to (-33.9461 to 151.1772),
    "MEL" to (-37.6690 to 144.8410),
    "DXB" to (25.2532 to 55.3657),
    "SIN" to (1.3644 to 103.9915),
    "BKK" to (13.6811 to 100.7472),
    "GRU" to (-23.4356 to -46.4731),
    "MEX" to (19.4363 to -99.0721),
    "YYZ" to (43.6772 to -79.6306),
    "YVR" to (49.1967 to -123.1815),
    "GIG" to (-22.8099 to -43.2505),
    "EZE" to (-34.8222 to -58.5358),
    "CPT" to (-33.9715 to 18.6021),
    "JNB" to (-26.1367 to 28.2411),
    "CAI" to (30.1219 to 31.4056),
    "MUC" to (48.3538 to 11.7861),
    "MAD" to (40.4936 to -3.5668),
    "BCN" to (41.2971 to 2.0785),
    "FCO" to (41.8003 to 12.2389),
    "ZRH" to (47.4647 to 8.5492),
    "VIE" to (48.1103 to 16.5697),
    "BRU" to (50.9014 to 4.4844),
    "ARN" to (59.6519 to 17.9186),
    "CPH" to (55.6180 to 12.6508),
    "OSL" to (60.1939 to 11.1004),
    "HEL" to (60.3172 to 24.9633),
    "LIS" to (38.7813 to -9.1359),
    "MAN" to (53.3537 to -2.2750),
)

/**
 * Compute [steps] intermediate points along the great-circle arc between two lat/lon points.
 * Returns a list of (lat, lon) pairs suitable for drawing a polyline.
 */
fun greatCirclePoints(
    fromLat: Double, fromLon: Double,
    toLat: Double,   toLon: Double,
    steps: Int = 60,
): List<Pair<Double, Double>> {
    val lat1 = Math.toRadians(fromLat)
    val lon1 = Math.toRadians(fromLon)
    val lat2 = Math.toRadians(toLat)
    val lon2 = Math.toRadians(toLon)

    val d = 2 * asin(sqrt(
        sin((lat2 - lat1) / 2).pow(2) +
        cos(lat1) * cos(lat2) * sin((lon2 - lon1) / 2).pow(2)
    ))

    if (d < 1e-10) return listOf(fromLat to fromLon, toLat to toLon)

    return (0..steps).map { i ->
        val f  = i.toDouble() / steps
        val A  = sin((1 - f) * d) / sin(d)
        val B  = sin(f * d) / sin(d)
        val x  = A * cos(lat1) * cos(lon1) + B * cos(lat2) * cos(lon2)
        val y  = A * cos(lat1) * sin(lon1) + B * cos(lat2) * sin(lon2)
        val z  = A * sin(lat1) + B * sin(lat2)
        val φ  = atan2(z, sqrt(x * x + y * y))
        val λ  = atan2(y, x)
        Math.toDegrees(φ) to Math.toDegrees(λ)
    }
}
