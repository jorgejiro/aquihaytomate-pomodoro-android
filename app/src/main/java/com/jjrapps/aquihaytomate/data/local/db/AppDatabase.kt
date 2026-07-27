package com.jjrapps.aquihaytomate.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Version 1: a single table, `focus_session`.
 *
 * The schema is exported to `app/schemas/` and versioned in git. When bumping [DATABASE_VERSION],
 * write the migration **and** its test in `androidTest/`; `AppDatabaseMigrationTest` is wired up from
 * v1 precisely so that the second version cannot land without one.
 */
@Database(
    entities = [FocusSessionEntity::class],
    version = AppDatabase.DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(LocalDateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "aquihaytomate.db"
    }
}
