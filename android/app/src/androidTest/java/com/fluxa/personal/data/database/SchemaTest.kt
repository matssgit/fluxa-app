package com.fluxa.personal.data.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SchemaTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        FluxaDatabase::class.java,
    )

    @Test
    fun exportedSchemaCanCreateVersionOne() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "fluxa-schema-test"
        helper.createDatabase(databaseName, 1).close()
        val database = Room.databaseBuilder(context, FluxaDatabase::class.java, databaseName).build()
        try {
            assertEquals(1, database.openHelper.writableDatabase.version)
        } finally {
            database.close()
        }
    }
}
