package com.fluxa.personal.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.migration.MIGRATION_2_3
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryKindInUse
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionResult.Saved
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FluxaDatabase::class.java)

    @Test
    fun migratesV2WithCatalogDataValidatesV3AndReopensLedger() = runBlocking(Dispatchers.IO) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "ledger-migration-${UUID.randomUUID()}"
        val account = UUID.randomUUID()
        val category = UUID.randomUUID()
        val id = UUID.randomUUID()
        val date = LocalDate.of(2024, 2, 29)
        try {
            helper.createDatabase(name, 2).use { db ->
                val timestamp = "2024-02-29T10:00:00Z"
                db.execSQL("INSERT INTO local_schema_marker (id) VALUES (42)")
                db.execSQL(
                    """
                    INSERT INTO accounts (id, name, type, openingBalanceCents, openingDate,
                                          includeInAvailable, createdAt, updatedAt)
                    VALUES (?, 'Banco', 'CHECKING', 250, ?, 1, ?, ?)
                    """.trimIndent(),
                    arrayOf(account.toString(), date.toString(), timestamp, timestamp),
                )
                db.execSQL("INSERT INTO categories (id, name, kind) VALUES (?, 'Receita', 'INCOME')", arrayOf(category.toString()))
            }
            helper.runMigrationsAndValidate(name, 3, true, MIGRATION_2_3).use { db ->
                assertEquals(3, db.version)
                db.query("SELECT id FROM local_schema_marker").use { rows ->
                    assertTrue(rows.moveToFirst())
                    assertEquals(42, rows.getInt(0))
                }
            }
            val db = FluxaDatabase.create(context, name)
            try {
                assertEquals(Money(250), AccountRepository(db.accountDao()).observe(account).first()!!.details.openingBalance)
                assertEquals(INCOME, CategoryRepository(db.categoryDao()).observe(category).first()!!.details.kind)
                val ledger = TransactionRepository(db)
                assertTrue(ledger.create(id, TransactionDraft(INCOME, account, category, Money(50), date)) is Saved)
                val error = runCatching {
                    CategoryRepository(db.categoryDao()).update(category, CategoryDetails("Editada", EXPENSE))
                }.exceptionOrNull()
                assertTrue(error is CategoryKindInUse)
            } finally {
                db.close()
            }
            val reopened = FluxaDatabase.create(context, name)
            try {
                val ledger = TransactionRepository(reopened)
                assertEquals(id, ledger.observeHistory().first().single().id)
                assertEquals(Exact(Money(300)), ledger.observeAvailableBalance().first())
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
