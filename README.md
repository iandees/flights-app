# Flights Android App — README

A personal Android app for tracking flights you've taken. Log every segment, view them on a world map with great-circle arcs, and see stats about your travel history.

---

## Features

| Screen | Description |
|---|---|
| **Flight List** | Scrollable list of all flights, sorted by departure date. Searchable by airline, flight number, airport, aircraft, or notes. Swipe/tap to delete. |
| **Flight Detail** | Full read-only view of a single flight. Edit or delete from the top bar. |
| **Add / Edit Flight** | Full form with autocomplete for airline, airport codes, and timezone. Combined date+time picker (tap once → date dialog → time dialog). Auto-fill from AirLabs API when airline + flight number are set. |
| **Map** | World map (MapLibre) with great-circle arcs connecting departure and arrival airports for every flight. |
| **Stats** | Segments by year (bar chart), by airline, by aircraft type, by seat class; top departure airports; top routes. |
| **Import CSV** | Import from a Google Sheets CSV export. File picker → parse preview → confirm. |

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
| `recordLocator` | `String` | PNR / booking code |
| `ticketNumber` | `String` | |
| `seat` | `String` | e.g. `"12A"` |
| `boardingGroup` | `String` | |
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

When adding/editing a flight, tapping the 🔍 button next to the flight number fetches schedule data from the [AirLabs API](https://airlabs.co/) and auto-fills:
- From / To airports
- Departure and arrival date + time (local timezone)
- Aircraft registration and model

**Setup:** Store your AirLabs API key in the app's Settings screen (not yet built — the key is persisted via `AppSettingsRepository` in DataStore under the key `airlabs_api_key`). The free tier gives enough requests for personal use.

---

## Building

1. Open the project root in **Android Studio Meerkat** or later.
2. Android SDK 35 must be installed.
3. Sync Gradle — no local API keys are required to build.
4. Run on a device or emulator (API 26+).

The app uses `fallbackToDestructiveMigration()` in Room, so upgrading the schema version wipes the local DB on-device. For production use, write proper Room migration scripts before removing that setting.

---

## Known Gaps / Next Steps

- **Settings screen** — needed to enter the AirLabs API key
- **Room migrations** — currently uses destructive migration; add proper `Migration` objects before shipping
- **Map airport coverage** — arcs only draw for airports in `iata_coords.csv`; flights with unlisted codes are silently skipped
- **Flight detail** — timezone-aware display of times (currently shows device-local time)
- **Wear OS / widget** — not implemented
- **iOS / cross-platform** — Android only; Flutter was considered but native Kotlin was chosen for long-term Android maintainability
