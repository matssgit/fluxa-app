package com.fluxa.personal.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionResolutionDetail
import com.fluxa.personal.domain.model.SubscriptionResult
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionUndoResult
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.rules.CalculatedBalance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Execute cada fase separadamente, com am force-stop com.fluxa.personal entre as fases. */
@RunWith(AndroidJUnit4::class)
class SubscriptionForceStopTest {
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val today = FinancialCalendar().today()
    private val initialBalance = Money(100_000)
    private val totalLimit = Money(50_000)

    @Test
    fun persistsAcrossForceStop() {
        val phase = InstrumentationRegistry.getArguments().getString("subscriptionPhase")
        assumeTrue(phase in listOf("settle", "undo", "verify"))
        ActivityScenario.launch(MainActivity::class.java).use {
            runBlocking {
                if (phase == "settle") seedAndSettle()
                val subscriptions = app.subscriptions.observeSubscriptions().first()
                assertEquals(2, subscriptions.size)
                for (subscription in subscriptions) {
                    val detail = app.subscriptions.observeOccurrenceDetail(subscription.id, subscription.anchorDate).first()!!
                    val reopened = phase == "verify"
                    when (val source = subscription.details.source) {
                        is SubscriptionSource.Account -> {
                            val balance = (app.transactions.observeAccountBalance(source.id).first() as CalculatedBalance.Exact).amount
                            val rows = app.transactions.observeHistory(TransactionHistoryFilter()).first()
                            assertEquals(if (reopened) initialBalance else Money(99_000), balance)
                            assertEquals(if (reopened) 0 else 1, rows.size)
                            if (reopened) assertNull(detail.resolution)
                            else assertTrue(detail.resolution is SubscriptionResolutionDetail.Account)
                        }
                        is SubscriptionSource.Card -> {
                            val summary = app.credit.observeCard(source.id).first()!!
                            assertEquals(if (reopened) totalLimit else Money(49_000), summary.availableLimit)
                            assertEquals(if (reopened) 0 else 1, app.credit.observePurchases(source.id).first().size)
                            if (reopened) assertNull(detail.resolution)
                            else assertTrue(detail.resolution is SubscriptionResolutionDetail.Card)
                        }
                    }
                    if (phase == "undo") {
                        assertTrue(app.subscriptions.undoSubscriptionOccurrence(
                            subscription.id, subscription.anchorDate, detail.occurrence.resultId!!,
                        ) is SubscriptionUndoResult.Reopened)
                    }
                    if (reopened) {
                        val expected = if (subscription.anchorDate < today) SubscriptionOccurrenceStatus.OVERDUE
                        else SubscriptionOccurrenceStatus.PENDING
                        assertEquals(expected, detail.occurrence.status)
                    }
                }
            }
        }
    }

    private suspend fun seedAndSettle() {
        app.database.clearAllTables()
        val category = app.categories.create(CategoryDetails("Streaming", EXPENSE))
        val account = app.accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, initialBalance, today))
        val card = (app.credit.createCard(CreditCardDetails("Cartão Roxo", totalLimit, BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id
        for (source in listOf(SubscriptionSource.Account(account), SubscriptionSource.Card(card))) {
            val result = app.subscriptions.create(
                SubscriptionDetails("Assinatura", Money(1_000), category, source),
                SubscriptionFrequency.MONTHLY, today, BillingDay(today.dayOfMonth),
            ) as SubscriptionResult.Saved
            assertTrue(app.subscriptions.paySubscriptionOccurrence(result.value.id, today, today) is SubscriptionResult.Saved)
        }
    }
}
