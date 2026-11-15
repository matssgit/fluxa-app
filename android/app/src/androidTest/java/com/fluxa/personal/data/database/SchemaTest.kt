package com.fluxa.personal.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.migration.MIGRATION_1_2
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
    fun migratesExportedV1AndPreservesExistingDataOnReopen() = runBlocking(Dispatchers.IO) {
        val name = legacyDatabase()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            helper.runMigrationsAndValidate(name, 2, true, MIGRATION_1_2).use { migrated ->
                assertEquals(2, migrated.version)
                migrated.query("SELECT id FROM local_schema_marker").use { rows ->
                    assertTrue(rows.moveToFirst())
                    assertEquals(42, rows.getInt(0))
                }
            }

            val details = AccountDetails("Banco", AccountType.SAVINGS, Money(1234), LocalDate.of(2024, 2, 29))
            val categoryDetails = CategoryDetails("Trabalho", TransactionDirection.INCOME)
            val database = FluxaDatabase.create(context, name)
            val (accountId, categoryId) = try {
                val accounts = AccountRepository(database.accountDao())
                val categories = CategoryRepository(database.categoryDao())
                val accountId = accounts.create(details)
                val categoryId = categories.create(categoryDetails)
                assertTrue(categories.archive(categoryId))
                accountId to categoryId
            } finally {
                database.close()
            }

            val reopened = FluxaDatabase.create(context, name)
            try {
                assertEquals(details, AccountRepository(reopened.accountDao()).observe(accountId).first()!!.details)
                val archived = CategoryRepository(reopened.categoryDao()).observe(categoryId).first()!!
                assertEquals(categoryDetails, archived.details)
                assertNotNull(archived.archivedAt)
                reopened.openHelper.writableDatabase.query("SELECT id FROM local_schema_marker").use { rows ->
                    assertTrue(rows.moveToFirst())
                    assertEquals(42, rows.getInt(0))
                }
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
        Unit
    }

    @Test
    fun productionBuilderAutomaticallyMigratesExistingFile() = runBlocking(Dispatchers.IO) {
        val name = legacyDatabase()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = FluxaDatabase.create(context, name)
        try {
            val sqlite = database.openHelper.writableDatabase
            assertEquals(3, sqlite.version)
            sqlite.query("SELECT id FROM local_schema_marker").use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals(42, rows.getInt(0))
            }
            assertTrue(AccountRepository(database.accountDao()).observeAll().first().isEmpty())
            assertTrue(CategoryRepository(database.categoryDao()).observeAll().first().isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun legacyDatabase(): String = "fluxa-schema-${UUID.randomUUID()}".also { name ->
        helper.createDatabase(name, 1).use { database ->
            database.execSQL("INSERT INTO local_schema_marker (id) VALUES (42)")
        }
    }
}
