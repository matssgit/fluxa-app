package com.fluxa.personal.data.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.migration.MIGRATION_6_7
import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.data.repository.GoalRepository
import com.fluxa.personal.data.repository.SubscriptionRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalResult.Saved
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), FluxaDatabase::class.java)

    @Test
    fun upgradesV6WithoutLosingSubscriptionLedgerAndReopensV7WithReserveHistory() = runBlocking(Dispatchers.IO) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "goal-migration-${UUID.randomUUID()}"
        val date = LocalDate.of(2024, 2, 29)
        val day = date.toEpochDay()
        val stamp = "2024-02-29T10:00:00Z"
        val (account, category, subscription, payment) = (1L..4L).map { UUID(0, it) }
        try {
            helper.createDatabase(name, 6).use { db ->
                db.execSQL("INSERT INTO local_schema_marker (id) VALUES (42)")
                db.execSQL("INSERT INTO accounts VALUES (?, 'Conta', 'CHECKING', 500000, ?, 1, ?, ?, NULL)",
                    arrayOf(account.toString(), date.toString(), stamp, stamp))
                db.execSQL("INSERT INTO categories (id, name, kind) VALUES (?, 'Despesas', 'EXPENSE')", arrayOf(category.toString()))
                db.execSQL("""
                    INSERT INTO subscriptions
                    (id, frequency, anchorDate, billingDay, status, createdAt, updatedAt, name, amountCents, categoryId, accountId)
                    VALUES (?, 'MONTHLY', ?, 29, 'ACTIVE', ?, ?, 'Assinatura', 50000, ?, ?)
                """.trimIndent(), arrayOf(subscription.toString(), day, stamp, stamp, category.toString(), account.toString()))
                db.execSQL("""
                    INSERT INTO subscription_terms
                    (subscriptionId, effectiveOn, enabled, name, amountCents, categoryId, accountId)
                    VALUES (?, ?, 1, 'Assinatura', 50000, ?, ?)
                """.trimIndent(), arrayOf(subscription.toString(), day, category.toString(), account.toString()))
                db.execSQL("""
                    INSERT INTO financial_transactions
                    (id,accountId,categoryId,direction,amountCents,occurredOn,createdAt,updatedAt,subscriptionId,occurrenceDate)
                    VALUES (?,?,?,'EXPENSE',50000,?,?,?,?,?)
                """.trimIndent(),
                    arrayOf(payment.toString(), account.toString(), category.toString(), day, stamp, stamp, subscription.toString(), day))
                db.execSQL("""
                    INSERT INTO subscription_resolutions
                    (subscriptionId, occurrenceDate, status, resultId, name, amountCents, categoryId, accountId)
                    VALUES (?, ?, 'PAID', ?, 'Assinatura', 50000, ?, ?)
                """.trimIndent(), arrayOf(subscription.toString(), day, payment.toString(), category.toString(), account.toString()))
            }
            helper.runMigrationsAndValidate(name, 7, true, MIGRATION_6_7).use { db ->
                db.query("SELECT id FROM local_schema_marker").use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals(42, cursor.getInt(0))
                }
            }
            val db = FluxaDatabase.create(context, name)
            val goalId = try {
                val ledger = TransactionRepository(db)
                val occurrence = SubscriptionRepository(db, CreditRepository(db)).observeOccurrence(subscription, date).first()!!
                assertEquals(SubscriptionOccurrenceStatus.PAID, occurrence.status)
                assertEquals(payment, occurrence.resultId)
                assertEquals(occurrence.key, ledger.observeHistory().first().single().subscriptionOccurrence)
                val goals = GoalRepository(db)
                assertTrue(goals.observeGoals().first().isEmpty())
                val goal = (goals.createGoal(GoalDetails("Reserva", Money(150_000), account)) as Saved).value
                assertTrue(goals.allocateToGoal(goal.id, Money(150_000)) is Saved)
                val sqlite = db.openHelper.writableDatabase
                assertTrue(runCatching {
                    sqlite.execSQL("DELETE FROM goals WHERE id = ?", arrayOf(goal.id.toString()))
                }.exceptionOrNull() is SQLiteConstraintException)
                assertTrue(runCatching {
                    sqlite.execSQL("UPDATE goals SET targetAmountCents = 0 WHERE id = ?", arrayOf(goal.id.toString()))
                }.exceptionOrNull() is SQLiteConstraintException)
                goal.id
            } finally {
                db.close()
            }
            val reopened = FluxaDatabase.create(context, name)
            try {
                val goals = GoalRepository(reopened)
                assertEquals(Money(150_000), goals.observeGoal(goalId).first()?.reservedAmount)
                assertEquals(1, goals.observeMovements(goalId).first().size)
                assertEquals(Exact(Money(450_000)), goals.observeReserveBalances().first().totalBalance)
                assertEquals(Exact(Money(300_000)), goals.observeReserveBalances().first().availableBalance)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
