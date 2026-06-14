# Flights Android App — README

A personal Android app for tracking flights you've taken. Log every segment, view them on a world map with great-circle arcs, and see stats about your travel history.

---

## Features

| Screen | Description |
|---|---|
| **Flight List** | Flights sorted newest-first with month headers (e.g. "June 2026") and a "── Today ──" divider separating past from future flights. Auto-scrolls to Today on load. Searchable by airline, flight number, airport, aircraft, or notes. Tap to delete (with confirmation). |
| **Flight Detail** | Full read-only view of a single flight. Edit or delete from the top bar. |
| **Add / Edit Flight** | Full form with autocomplete for airline (top 11 US carriers pinned first), airport codes, and timezone. Combined date+time picker. Auto-fill from AirLabs API (route, times, registration, aircraft model) once airline + flight number + departure date are set. Confirms before discarding unsaved changes. |
| **Map** | World map (MapLibre) with great-circle arcs for every flight. Covers 8,656 airports. |
| **Stats** | Segments by year (bar chart), by airline, by aircraft type, by seat class; top departure airports; top routes. |
| **Import CSV** | Import from a Google Sheets CSV export. File picker → parse preview → confirm. |
| **Settings** | Enter and persist an AirLabs API key for flight data lookup. Reachable via ⋮ menu → Settings. |

---

## Tech Stack

| Layer | Library |
|---|---|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Navigation | Navigation Compose 2.9.7 |
| DI | Hilt 2.57.1 + KSP |
| Database | Room 2.7.1 (schema v2) |
| Map | MapLibre Android 11.12.2 (via `AndroidView`) |
| Charts | Vico 2.0.1 |
| CSV parsing | Apache Commons CSV 1.12 |
| Flight data API | AirLabs REST API (optional, key stored in DataStore) |
| HTTP | OkHttp 4.12 |
| Date/time | kotlinx-datetime 0.6.1 |
| Preferences | DataStore 1.1.2 |

---

## Data Model

### `Flight` (domain model / `FlightEntity` DB table, schema v2)

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Auto-generated PK |
| `airline` | `String` | e.g. `"DL - Delta Air Lines"` or just `"DL"` |
| `flightNumber` | `String` | Numeric portion, e.g. `"123"` |
| `departureAirport` | `String` | IATA code, e.g. `"ATL"` |
| `arrivalAirport` | `String` | IATA code, e.g. `"LAX"` |
| `departureTime` | `Instant?` | Stored as epoch-ms; converted using `departureTimezone` |
| `arrivalTime` | `Instant?` | Stored as epoch-ms; converted using `arrivalTimezone` |
| `departureTimezone` | `String` | IANA ID, e.g. `"America/New_York"` |
| `arrivalTimezone` | `String` | IANA ID, e.g. `"America/Los_Angeles"` |
| `recordLocator` | `String` | PNR / booking code (displayed monospace) |
| `ticketNumber` | `String` | |
| `seat` | `String` | e.g. `"12A"` |
| `boardingGroup` | `String` | Numeric |
| `seatClass` | `String` | `Y`, `W`, `J`, `F`, etc. |
| `planeModel` | `String` | e.g. `"Boeing 737-800"` |
| `registration` | `String` | Aircraft tail number / N-code, e.g. `"N12345"` |
| `mqm` | `Int?` | Medallion Qualifying Miles |
| `mqs` | `Int?` | Medallion Qualifying Segments |
| `mqd` | `Int?` | Medallion Qualifying Dollars |
| `awardMiles` | `Int?` | |
| `notes` | `String` | Free-form (delays, cancellations, etc.) |

---

## Asset Files

Located in `app/src/main/assets/`:

| File | Rows | Format | Source |
|---|---|---|---|
| `iata_tz.csv` | 11,315 | `IATA^IANA_TZ` | [benct/iata-utils](https://github.com/benct/iata-utils) |
| `iata_names.csv` | 4,456 | `IATA^Name (City)` | [OurAirports](https://davidmegginson.github.io/ourairports-data/) |
| `iata_coords.csv` | 8,656 | `IATA^lat^lon` | [OurAirports](https://davidmegginson.github.io/ourairports-data/) |
| `airline_names.csv` | 1,003 | `IATA^Name` | [OpenFlights](https://github.com/jpatokal/openflights) — top 11 US carriers pinned first |

---

## Google Sheets Import

Export your sheet as CSV (File → Download → Comma Separated Values), then use the Import screen to pick the file.

Expected column headers (case-insensitive, extra columns ignored):

```
Airline | From | To | Departure | Arrival | Record Loc | Ticket # | Flight No |
Seat | Group | Class | Plane | Registration | MQM | MQS | MQD | Award Miles | Notes
```

Accepted date formats: `M/d/yyyy H:mm`, `M/d/yyyy`, `yyyy-MM-dd HH:mm:ss`, `yyyy-MM-dd`.

---

## AirLabs Flight Data API (optional)

When adding/editing a flight, set the airline, flight number, and departure date, then tap the 🔍 button to auto-fill:
- From / To airports
- Departure and arrival date + time (local timezone)
- Aircraft registration and model

Two parallel API calls are made: `/v9/schedules` (route + times, filtered to the selected date) and `/v9/flight` (registration + model from the live/nearest instance).

**Limitation:** AirLabs is a live/near-real-time API — it only has data within roughly the next 10 hours. Useful for logging flights today or tomorrow; historical flights will return "not found".

**Setup:** ⋮ menu → Settings → paste your AirLabs API key. Get a free key at [airlabs.co](https://airlabs.co) (no credit card required).

---

## Keyboard behaviour

| Field | Keyboard |
|---|---|
| Record locator, Seat, Registration | `KeyboardType.Password` (no masking) — forces QWERTY + number row on all IMEs |
| Airline, Class | `KeyboardCapitalization.Characters` (caps-lock) |
| Boarding group, MQM/MQS/MQD, Award Miles, Ticket # | Numeric |
| Flight number | Numeric |
| All others | Standard text |

---

## Building

1. Open the project root in **Android Studio Meerkat** or later.
2. Android SDK 35 must be installed.
3. Sync Gradle — no local API keys are required to build.
4. Run on a device or emulator (API 26+).

The app uses `fallbackToDestructiveMigration()` in Room, so upgrading the schema version wipes the local DB on-device. Write proper `Migration` objects before removing that setting for production use.

---

## Known Gaps / Next Steps

- **Room migrations** — currently uses destructive migration; add proper `Migration` objects before shipping
- **Map airport coverage** — arcs only draw for airports in `iata_coords.csv`; flights with unlisted codes are silently skipped
- **Flight detail** — timezone-aware display of times (currently shows device-local time)
- **AirLabs historical data** — the API only covers live/upcoming flights; past flights need manual entry
- **Wear OS / widget** — not implemented
- **iOS / cross-platform** — Android only; Flutter was considered but native Kotlin was chosen
