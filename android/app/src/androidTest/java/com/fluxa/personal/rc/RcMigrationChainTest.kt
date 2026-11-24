package com.fluxa.personal.rc

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.database.migration.*
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RcMigrationChainTest {
    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FluxaDatabase::class.java)

    @Test fun historicalV1MigratesThroughEveryExportedSchemaToV7AndReopens() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "rc-migration-" + UUID.randomUUID()
        try {
            helper.createDatabase(name, 1).use {
                it.execSQL("INSERT INTO local_schema_marker (id) VALUES (42)")
            }
            helper.runMigrationsAndValidate(
                name, 7, true, MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
            ).use { db ->
                assertEquals(7, db.version)
                db.query("SELECT id FROM local_schema_marker").use { row ->
                    assertEquals(true, row.moveToFirst())
                    assertEquals(42, row.getInt(0))
                }
            }
            val reopened = FluxaDatabase.create(context, name)
            try { assertEquals(7, reopened.openHelper.writableDatabase.version) }
            finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }
}
