package com.fluxa.personal.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.migration.MIGRATION_4_5
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.InstallmentPaymentResult.Paid
import com.fluxa.personal.domain.model.InstallmentStatus
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallmentPaymentMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FluxaDatabase::class.java)

    private val account = UUID(0, 1)
    private val category = UUID(0, 2)
    private val oldTransaction = UUID(0, 3)
    private val card = UUID(0, 4)
    private val purchase = UUID(0, 5)
    private val installment = UUID(0, 6)
    private val day = LocalDate.of(2024, 2, 29)

    @Test
    fun migratesV4PreservesEveryFinancialTableAndPersistsNewLinkOnReopen() = runBlocking(Dispatchers.IO) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "payment-migration-${UUID.randomUUID()}"
        try {
            helper.createDatabase(name, 4).use(::seed)
            helper.runMigrationsAndValidate(name, 5, true, MIGRATION_4_5).use { db ->
                db.query("SELECT installmentId FROM financial_transactions").use { rows ->
                    assertEquals(1, rows.count)
                    rows.moveToFirst()
                    assertNull(rows.getString(0))
                }
            }
            val db = FluxaDatabase.create(context, name)
            val payment = try {
                val credit = CreditRepository(db)
                val ledger = TransactionRepository(db)
                assertEquals("Banco", AccountRepository(db.accountDao()).observe(account).first()!!.details.name)
                assertEquals("Compras", CategoryRepository(db.categoryDao()).observe(category).first()!!.details.name)
                assertEquals(Money(300), credit.observeCard(card).first()!!.availableLimit)
                assertEquals(purchase, credit.observePurchases(card).first().single().id)
                assertEquals(installment, credit.observeInstallments(purchase).first().single().id)
                assertEquals(oldTransaction, ledger.observeHistory().first().single().id)
                assertNull(ledger.observeHistory().first().single().installmentId)
                (credit.payInstallment(installment, account, day) as Paid).transaction
            } finally {
                db.close()
            }
            val reopened = FluxaDatabase.create(context, name)
            try {
                val credit = CreditRepository(reopened)
                assertEquals(payment, credit.observeInstallmentPayment(installment).first())
                assertEquals(InstallmentStatus.PAID, credit.observeInstallments(purchase).first().single().status)
                assertEquals(2, TransactionRepository(reopened).observeHistory().first().size)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun seed(db: SupportSQLiteDatabase) {
        val stamp = "2024-02-29T10:00:00Z"
        db.execSQL("INSERT INTO local_schema_marker (id) VALUES (42)")
        db.execSQL(
            "INSERT INTO accounts VALUES (?, 'Banco', 'CHECKING', 250, ?, 1, ?, ?, NULL)",
            arrayOf(account.toString(), day.toString(), stamp, stamp),
        )
        db.execSQL("INSERT INTO categories (id, name, kind) VALUES (?, 'Compras', 'EXPENSE')", arrayOf(category.toString()))
        db.execSQL(
            """
            INSERT INTO financial_transactions
            (id, accountId, categoryId, direction, amountCents, occurredOn, createdAt, updatedAt)
            VALUES (?, ?, ?, 'EXPENSE', 50, ?, ?, ?)
            """.trimIndent(),
            arrayOf(oldTransaction.toString(), account.toString(), category.toString(), day.toEpochDay(), stamp, stamp),
        )
        db.execSQL(
            "INSERT INTO credit_cards VALUES (?, 'Cartão', 1000, 10, 17, NULL, NULL, ?, ?)",
            arrayOf(card.toString(), stamp, stamp),
        )
        db.execSQL(
            """
            INSERT INTO credit_purchases
            (id, cardId, categoryId, title, totalCents, installmentCount, purchaseDate, firstDueDate, createdAt, updatedAt)
            VALUES (?, ?, ?, 'Compra', 700, 1, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(purchase.toString(), card.toString(), category.toString(), day.toEpochDay(), day.toEpochDay(), stamp, stamp),
        )
        db.execSQL(
            "INSERT INTO credit_installments VALUES (?, ?, 1, 700, ?, 'PENDING')",
            arrayOf(installment.toString(), purchase.toString(), day.toEpochDay()),
        )
    }
}
