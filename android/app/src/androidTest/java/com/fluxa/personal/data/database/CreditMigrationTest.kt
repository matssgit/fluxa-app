package com.fluxa.personal.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.migration.MIGRATION_3_4
import com.fluxa.personal.data.repository.CategoryKindInUse
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult.Saved
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
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
class CreditMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FluxaDatabase::class.java)

    @Test
    fun migrationPreservesLedgerAndCatalogAndReopensPersistedCredit() = runBlocking(Dispatchers.IO) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "credit-migration-${UUID.randomUUID()}"
        val account = UUID.randomUUID()
        val income = UUID.randomUUID()
        val expense = UUID.randomUUID()
        val transaction = UUID.randomUUID()
        val purchase = UUID.randomUUID()
        val date = LocalDate.of(2024, 2, 29)
        try {
            helper.createDatabase(name, 3).use { db ->
                val timestamp = "2024-02-29T10:00:00Z"
                db.execSQL("INSERT INTO local_schema_marker (id) VALUES (42)")
                db.execSQL(
                    "INSERT INTO accounts VALUES (?, 'Banco', 'CHECKING', 250, ?, 1, ?, ?, NULL)",
                    arrayOf(account.toString(), date.toString(), timestamp, timestamp),
                )
                db.execSQL("INSERT INTO categories (id, name, kind) VALUES (?, 'Receita', 'INCOME')", arrayOf(income.toString()))
                db.execSQL("INSERT INTO categories (id, name, kind) VALUES (?, 'Despesa', 'EXPENSE')", arrayOf(expense.toString()))
                db.execSQL(
                    """
                    INSERT INTO financial_transactions
                    (id, accountId, categoryId, direction, amountCents, occurredOn, createdAt, updatedAt)
                    VALUES (?, ?, ?, 'INCOME', 50, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(transaction.toString(), account.toString(), income.toString(), date.toEpochDay(), timestamp, timestamp),
                )
            }
            helper.runMigrationsAndValidate(name, 4, true, MIGRATION_3_4).use { db ->
                assertEquals(4, db.version)
                db.query("SELECT id FROM local_schema_marker").use { rows ->
                    assertTrue(rows.moveToFirst())
                    assertEquals(42, rows.getInt(0))
                }
            }
            val db = FluxaDatabase.create(context, name)
            val cardId = try {
                val ledger = TransactionRepository(db)
                assertEquals(Exact(Money(300)), ledger.observeAvailableBalance().first())
                assertEquals(transaction, ledger.observeHistory().first().single().id)
                val credit = CreditRepository(db)
                val card = (credit.createCard(CreditCardDetails("Cartão", Money(100), BillingDay(10), BillingDay(17))) as Saved).value
                assertTrue(credit.createPurchase(purchase, PurchaseDraft(card.id, expense, "Compra", Money(99), 3, date)) is Saved)
                val categories = CategoryRepository(db.categoryDao())
                assertTrue(runCatching { categories.update(expense, CategoryDetails("Editada", INCOME)) }.exceptionOrNull() is CategoryKindInUse)
                assertTrue(runCatching { categories.update(income, CategoryDetails("Editada", EXPENSE)) }.exceptionOrNull() is CategoryKindInUse)
                card.id
            } finally {
                db.close()
            }
            val reopened = FluxaDatabase.create(context, name)
            try {
                val credit = CreditRepository(reopened)
                assertEquals(purchase, credit.observePurchases(cardId).first().single().id)
                assertEquals(listOf(33L, 33L, 33L), credit.observeInstallments(purchase).first().map { it.amount.cents })
                assertEquals(Money(1), credit.observeCard(cardId).first()!!.availableLimit)
                assertEquals(Exact(Money(300)), TransactionRepository(reopened).observeAvailableBalance().first())
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
