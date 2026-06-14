# Flights App — Agent Handoff Document

## Project

Native Android flight-tracker app. Logs personal flight history (originally from Google Sheets), with list, map, stats, and add/edit/import screens.

- **Path:** `/Users/iandees/SynologyDrive/Projects/Other/flights-app`
- **Repo:** `git@github.com:iandees/flights-app.git` (branch: `main`)
- **Package:** `com.iandees.flights`
- **Min SDK:** 26 · **Target/Compile SDK:** 35

---

## Build

```bash
# No gradlew in project root; use the wrapper directly:
~/.gradle/wrapper/dists/gradle-8.11.1-bin/*/gradle-8.11.1/bin/gradle \
  -p /Users/iandees/SynologyDrive/Projects/Other/flights-app \
  :app:assembleDebug
```

Or open in Android Studio Meerkat+.

**`gradle.properties`:** `org.gradle.caching=false`, `-Xmx2g`

---

## Key library versions (`gradle/libs.versions.toml`)

| Library | Version |
|---|---|
| Kotlin | 2.1.0 |
| KSP | 2.1.0-1.0.29 |
| AGP | 8.13.2 |
| Hilt | 2.57.1 |
| Room | 2.7.1 |
| Compose BOM | 2025.01.00 |
| Navigation Compose | 2.9.7 |
| MapLibre Android | 11.12.2 |
| Vico | 2.0.1 |
| OkHttp | 4.12.0 |
| kotlinx-datetime | 0.6.1 |
| DataStore | 1.1.2 |

**Critical KSP args** in `app/build.gradle.kts`:
```kotlin
ksp {
    arg("room.generateKotlin", "true")
    arg("dagger.hilt.internal.useAggregatingRootProcessor", "false")
}
```
These work around a KSP ordering issue where Hilt processes `FlightRepository` before Room generates `FlightDao_Impl`. Do not remove them.

---

## Architecture

- **Single-activity** (`MainActivity`) with `enableEdgeToEdge()`
- **Navigation:** `FlightsNavHost` — bottom nav (Flights / Map / Stats) + detail routes
- **DI:** Hilt throughout; `FlightRepository` is provided manually via `DatabaseModule.provideFlightRepository(dao)` — it has no `@Inject constructor` to avoid the KSP ordering issue
- **Root Scaffold:** `contentWindowInsets = WindowInsets(0)` — each screen manages its own insets via its own `TopAppBar`

---

## Screen inventory

| Route | Screen | ViewModel |
|---|---|---|
| `flight_list` | `FlightListScreen` | `FlightListViewModel` |
| `flight_detail/{flightId}` | `FlightDetailScreen` | `FlightDetailViewModel` |
| `add_flight` | `AddEditFlightScreen` | `AddEditFlightViewModel` |
| `edit_flight/{flightId}` | `AddEditFlightScreen` | `AddEditFlightViewModel` |
| `map` | `MapScreen` | `MapViewModel` |
| `stats` | `StatsScreen` | `StatsViewModel` |
| `import_csv` | `ImportCsvScreen` | `ImportCsvViewModel` |
| `settings` | `SettingsScreen` | `SettingsViewModel` |

---

## Database

- **Room schema v2** — `FlightsDatabase`, `FlightEntity`, `FlightDao`
- **`fallbackToDestructiveMigration()`** is set — upgrading schema version wipes the DB
- Schema v2 added `departure_timezone` and `arrival_timezone` columns
- Times stored as epoch-ms (`departure_time_ms`, `arrival_time_ms`)
- DAO sorts **DESC** (newest first) for `getAllFlights()` and `searchFlights()`

---

## Key source files

```
app/src/main/kotlin/com/iandees/flights/
├── FlightsApplication.kt             Hilt app
├── MainActivity.kt                   enableEdgeToEdge()
├── FlightsNavHost.kt                 Nav graph + bottom bar
├── core/
│   ├── database/
│   │   ├── FlightsDatabase.kt
│   │   ├── DatabaseModule.kt         Hilt module; provides FlightRepository manually
│   │   ├── FlightRepository.kt       No @Inject constructor (KSP workaround)
│   │   ├── FlightMappers.kt
│   │   ├── dao/FlightDao.kt
│   │   └── entity/FlightEntity.kt
│   ├── model/
│   │   ├── Flight.kt                 Domain model
│   │   └── FlightStats.kt
│   ├── designsystem/theme/FlightsTheme.kt
│   └── network/
│       ├── AirlineSearchRepository.kt   Loads airline_names.csv; top-11 US carriers pinned
│       ├── AirportSearchRepository.kt   Loads iata_names.csv; autocomplete for From/To
│       ├── AirportTimezoneRepository.kt Loads iata_tz.csv; auto-fills timezone from IATA
│       ├── AppSettingsRepository.kt     DataStore; stores AirLabs API key
│       ├── CsvImporter.kt              Google Sheets CSV → Flight list
│       ├── FlightLookupService.kt      AirLabs /v9/schedules + /v9/flight (parallel)
│       └── TimezoneSearchRepository.kt  IANA zone list; autocomplete for timezone fields
└── feature/
    ├── addedit/
    │   ├── AddEditFlightScreen.kt
    │   └── AddEditFlightViewModel.kt
    ├── flightdetail/
    ├── flightlist/
    │   ├── FlightListScreen.kt
    │   └── FlightListViewModel.kt    Builds List<FlightListItem> (MonthHeader/TodayDivider/FlightRow)
    ├── importcsv/
    ├── map/
    │   ├── AirportCoordsRepository.kt  Loads iata_coords.csv (8,656 airports)
    │   ├── AirportData.kt              Great-circle arc math (legacy; still used for arc generation)
    │   ├── MapScreen.kt                AndroidView wrapping MapLibre MapView
    │   └── MapViewModel.kt
    ├── settings/
    │   ├── SettingsScreen.kt
    │   └── SettingsViewModel.kt
    └── stats/
```

---

## Assets (`app/src/main/assets/`)

| File | Rows | Format |
|---|---|---|
| `iata_tz.csv` | 11,315 | `IATA^IANA_TZ` |
| `iata_names.csv` | 4,456 | `IATA^Name (City)` |
| `iata_coords.csv` | 8,656 | `IATA^lat^lon` |
| `airline_names.csv` | 1,003 | `IATA^Name` — first 11 rows are pinned US majors |

---

## Notable design decisions

### FlightListViewModel — list items
`buildUiState()` converts `List<Flight>` → `List<FlightListItem>` (sealed: `MonthHeader`, `TodayDivider`, `FlightRow`). The list is **DESC**, so `TodayDivider` is inserted when we first see a flight with `depDate < today` (not `>= today`). Month headers and the divider are suppressed when a search query is active.

### AddEditFlightViewModel — dirty tracking
Every field change goes through `fun update(block)` which sets `isDirty = true`. `onBackPressed()` returns `true` (consumed) and shows a confirm dialog when dirty. `confirmDiscard()` sets `isSaved = true` to trigger the navigation `LaunchedEffect`. `loadFlight()` constructs a fresh `AddEditUiState(...)` so loading an existing flight never marks it dirty.

### FlightLookupService — two parallel calls
`/v9/schedules?flight_iata=...` → route + times (filtered by date). `/v9/flight?flight_iata=...` → registration + model. Both fired with `OkHttpClient`; `/v9/flight` is best-effort (errors swallowed). Called on IO dispatcher from ViewModel.

### Alphanumeric keyboard (record locator, seat, registration)
`KeyboardType.Password` **without** `PasswordVisualTransformation`. This maps to `textVisiblePassword` in the IME, which is the only `inputType` that reliably forces all major keyboards (Gboard, SwiftKey, Samsung) to show QWERTY + number row simultaneously.

### Map coordinates
`AirportData.kt` contains the great-circle arc math. `AirportCoordsRepository` loads `iata_coords.csv` lazily at first access. `MapViewModel` passes the coords map to `FlightMapView` via `MapUiState`. Arcs silently skip flights where either airport isn't in the asset.

### Search debounce
`_searchQuery: MutableStateFlow<String>` is exposed directly as `searchQuery: StateFlow<String>` for the text field binding (instant feedback). The `uiState` flow uses `.debounce(200)` before the DB query so the field doesn't feel laggy but DB queries don't fire on every keystroke.

### hiltViewModel() deprecation
`@Suppress("DEPRECATION")` is applied to screens using `hiltViewModel()`. The replacement package (`androidx.hilt.lifecycle.viewmodel.compose`) is declared in the deprecation message but not yet published in `hilt-navigation-compose:1.3.0`. Remove the suppression when the library ships the new package.

---

## Known issues / next steps

1. **Room migrations** — `fallbackToDestructiveMigration()` is set; write proper `Migration` objects before any production release
2. **Flight detail timezone display** — times are shown in the device's local timezone, not the departure/arrival timezone
3. **AirLabs historical lookup** — the API only covers live/upcoming flights (~10h window); past flights can't be auto-filled
4. **Map: flights without a set departure time** — `departureTime == null` flights have no arcs (expected) but also no visual indicator on the map
5. **Stats screen** — uses device-local timezone for date grouping; could use departure timezone instead
6. **No tests** — no unit or instrumented tests exist yet
