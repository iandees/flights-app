package com.iandees.flights.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All Room schema migrations in one place.
 *
 * v1 → v2: Added departure_timezone and arrival_timezone TEXT columns (both NOT NULL DEFAULT '').
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE flights ADD COLUMN departure_timezone TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE flights ADD COLUMN arrival_timezone   TEXT NOT NULL DEFAULT ''")
    }
}
