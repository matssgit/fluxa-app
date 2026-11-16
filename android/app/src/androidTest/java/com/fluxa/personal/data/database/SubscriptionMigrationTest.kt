package com.fluxa.personal.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.migration.MIGRATION_5_6
import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.data.repository.SubscriptionRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.InstallmentOrigin
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionResult.Saved
import com.fluxa.personal.domain.model.SubscriptionSource
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
class SubscriptionMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FluxaDatabase::class.java)

    @Test
    fun preservesV5CreditAndLedgerLinksAndReopensV6Subscription() = runBlocking(Dispatchers.IO) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "subscription-migration-${UUID.randomUUID()}"
        val date = LocalDate.of(2024, 2, 29)
        val day = date.toEpochDay()
        val stamp = "2024-02-29T10:00:00Z"
        val (account, category, card, purchase, installment) = (1L..5L).map { UUID(0, it) }
        try {
            helper.createDatabase(name, 5).use { db ->
                db.execSQL("INSERT INTO local_schema_marker (id) VALUES (42)")
                db.execSQL("INSERT INTO accounts VALUES (?, 'Conta', 'CHECKING', 100, ?, 1, ?, ?, NULL)",
                    arrayOf(account.toString(), date.toString(), stamp, stamp))
                db.execSQL("INSERT INTO categories (id, name, kind) VALUES (?, 'Despesas', 'EXPENSE')", arrayOf(category.toString()))
                db.execSQL("INSERT INTO credit_cards VALUES (?, 'Cartão', 1000, 10, 17, NULL, NULL, ?, ?)", arrayOf(card.toString(), stamp, stamp))
                db.execSQL(
                    """INSERT INTO credit_purchases
                        (id,cardId,categoryId,title,totalCents,installmentCount,purchaseDate,firstDueDate,createdAt,updatedAt)
                        VALUES (?,?,?,'Histórica',50,1,?,?,?,?)""",
                    arrayOf(purchase.toString(), card.toString(), category.toString(), day, day, stamp, stamp),
                )
                db.execSQL("INSERT INTO credit_installments VALUES (?,?,1,50,?,'PAID')", arrayOf(installment.toString(), purchase.toString(), day))
                db.execSQL(
                    """INSERT INTO financial_transactions
                        (id,accountId,categoryId,direction,amountCents,occurredOn,createdAt,updatedAt,installmentId)
                        VALUES (?,?,?,'EXPENSE',50,?,?,?,?)""",
                    arrayOf(UUID(0, 6).toString(), account.toString(), category.toString(), day, stamp, stamp, installment.toString()),
                )
            }
            helper.runMigrationsAndValidate(name, 6, true, MIGRATION_5_6).use { db ->
                db.query("SELECT id FROM local_schema_marker").use {
                    it.moveToFirst()
                    assertEquals(42, it.getInt(0))
                }
            }
            val db = FluxaDatabase.create(context, name)
            val id = try {
                val credit = CreditRepository(db)
                val ledger = TransactionRepository(db)
                assertEquals(InstallmentOrigin(installment, purchase, card), credit.observeInstallmentOrigin(installment).first())
                val original = ledger.observeHistory().first().single()
                assertEquals(installment, original.installmentId)
                assertNull(original.subscriptionOccurrence)
                assertNull(credit.observePurchases(card).first().single().subscriptionOccurrence)
                val repository = SubscriptionRepository(db, credit)
                val subscription = (repository.create(
                    SubscriptionDetails("Anual", Money(60000), category, SubscriptionSource.Account(account)),
                    SubscriptionFrequency.YEARLY, date, BillingDay(29),
                ) as Saved).value
                val payment = (repository.paySubscriptionOccurrence(subscription.id, date, date) as Saved).value
                assertEquals(payment.key, ledger.observe(payment.resultId!!).first()!!.subscriptionOccurrence)
                subscription.id
            } finally {
                db.close()
            }
            val reopened = FluxaDatabase.create(context, name)
            try {
                val repository = SubscriptionRepository(reopened, CreditRepository(reopened))
                assertEquals(Money(60000), repository.observeOccurrence(id, date).first()!!.details.amount)
                assertEquals(2, TransactionRepository(reopened).observeHistory().first().size)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
