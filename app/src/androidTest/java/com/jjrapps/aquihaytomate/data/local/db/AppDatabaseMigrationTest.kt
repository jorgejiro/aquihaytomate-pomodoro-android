package com.jjrapps.aquihaytomate.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wired up from v1 on purpose, before there is anything to migrate.
 *
 * Its job today is to prove the exported schema in `app/schemas/` is real and openable, and to fail
 * loudly the moment someone bumps [AppDatabase.DATABASE_VERSION] without adding a migration and its
 * test here. Getting that harness in place before it is needed is what stops the second version from
 * shipping with `fallbackToDestructiveMigration` and wiping somebody's history.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun version1SchemaIsExportedAndOpenable() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            assertTrue(db.isOpen)
        }
    }

    @Test
    fun version1HasTheFocusSessionTableWithItsUniqueIndex() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.query(
                "SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'focus_session'",
            ).use { cursor ->
                val indices = mutableListOf<String>()
                while (cursor.moveToNext()) indices += cursor.getString(0)

                assertTrue(
                    "UNIQUE(session_id, slot_index) is architecture, not an optimisation. " +
                        "See CLAUDE.md §5. Found: $indices",
                    indices.any { it.contains("session_id") && it.contains("slot_index") },
                )
            }
        }
    }

    /**
     * A reminder with teeth. When the schema changes: bump the version, add the `Migration`, add a
     * `migrate1To2` test next to this one, and update the number here.
     */
    @Test
    fun everyVersionBumpNeedsAMigrationTest() {
        assertEquals(
            "The database version changed. Add the migration and its test before updating this " +
                "number.",
            1,
            AppDatabase.DATABASE_VERSION,
        )
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
