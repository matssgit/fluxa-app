package com.fluxa.personal.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.data.repository.GoalRepository
import com.fluxa.personal.data.repository.SafeToSpendRepository
import com.fluxa.personal.data.repository.SubscriptionRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.*
import com.fluxa.personal.domain.rules.CalculatedBalance
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafeToSpendPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "safe-to-spend-${UUID.randomUUID()}"
    // UTC is already November 20. The financial date in Sao Paulo is still November 19.
    private val clock = Clock.fixed(Instant.parse("2026-11-20T01:00:00Z"), ZoneOffset.UTC)
    private val calendar = FinancialCalendar(clock) { ZoneOffset.ofHours(-3) }
    private val today = LocalDate.of(2026, 11, 19)
    private lateinit var db: FluxaDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var goals: GoalRepository
    private lateinit var credit: CreditRepository
    private lateinit var ledger: TransactionRepository
    private lateinit var subscriptions: SubscriptionRepository
    private lateinit var safe: SafeToSpendRepository
    private lateinit var accountId: UUID
    private lateinit var categoryId: UUID

    @Before
    fun setup() = runBlocking {
        db = FluxaDatabase.create(context, name)
        accounts = AccountRepository(db.accountDao(), clock)
        goals = GoalRepository(db, clock, calendar)
        credit = CreditRepository(db, clock, calendar)
        ledger = TransactionRepository(db, clock, calendar)
        subscriptions = SubscriptionRepository(db, credit, clock, calendar)
        safe = SafeToSpendRepository(db, goals, clock, calendar)
        accountId = account("Principal", 500_000)
        categoryId = CategoryRepository(db.categoryDao(), clock).create(CategoryDetails("Despesa", TransactionDirection.EXPENSE))
    }

    @After
    fun cleanup() {
        db.close()
        context.deleteDatabase(name)
    }

    @Test
    fun reserveAllocationReleaseAndLedgerChangesReactWithoutDoubleDeduction() = runBlocking {
        val goal = (goals.createGoal(GoalDetails("Viagem", Money(150_000), accountId)) as GoalResult.Saved).value
        val events = Channel<SafeToSpendResult>(Channel.UNLIMITED)
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            safe.observeSafeToSpend().collect { events.send((it as SafeToSpendState.Ready).result) }
        }
        try {
            assertEquals(today, events.next().horizonStart)
            assertTrue(goals.allocateToGoal(goal.id, Money(150_000)) is GoalResult.Saved)
            val reserved = events.next()
            assertEquals(Money(350_000), reserved.safeAmount)
            assertEquals(Money(350_000), reserved.availableNow)
            assertEquals(Money(0), reserved.obligationsTotal)
            assertTrue(reserved.breakdown.isEmpty())

            val excluded = account("Investimentos", 1_000_000, included = false)
            val excludedGoal = (goals.createGoal(GoalDetails("Longo prazo", Money(400_000), excluded)) as GoalResult.Saved).value
            assertTrue(goals.allocateToGoal(excludedGoal.id, Money(400_000)) is GoalResult.Saved)
            assertEquals(Money(350_000), current().safeAmount)
            assertTrue(goals.releaseFromGoal(goal.id, Money(50_000)) is GoalResult.Saved)
            assertEquals(Money(400_000), events.next().safeAmount)

            val tx = (ledger.create(UUID.randomUUID(), TransactionDraft(
                TransactionDirection.EXPENSE, accountId, categoryId, Money(450_000), today,
            )) as TransactionResult.Saved).transaction
            val negative = events.next()
            assertEquals(Money(-50_000), negative.availableNow)
            assertEquals(Money(0), negative.safeAmount)
            assertTrue(ledger.cancel(tx.id) is TransactionResult.Saved)
            assertEquals(Money(400_000), events.next().safeAmount)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun subscriptionAccountPaymentUndoAndSkipReplaceTheSameCashObligationReactively() = runBlocking {
        val subscription = subscription(SubscriptionSource.Account(accountId), today.minusDays(1), 30_000, SubscriptionFrequency.YEARLY)
        val states = Channel<SafeToSpendResult>(Channel.UNLIMITED)
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            safe.observeSafeToSpend().collect { states.send((it as SafeToSpendState.Ready).result) }
        }
        try {
            val pending = states.next()
            assertEquals(Money(30_000), pending.obligationsTotal)
            assertEquals(today, pending.breakdown.single().date)
            assertEquals(Money(470_000), pending.safeAmount)
            val paid = (subscriptions.paySubscriptionOccurrence(subscription.id, today.minusDays(1), today) as SubscriptionResult.Saved).value
            val afterPay = states.next()
            assertEquals(Money(470_000), afterPay.availableNow)
            assertEquals(Money(0), afterPay.obligationsTotal)
            assertTrue(afterPay.breakdown.isEmpty())
            assertTrue(subscriptions.undoSubscriptionOccurrence(subscription.id, today.minusDays(1), paid.resultId!!) is SubscriptionUndoResult.Reopened)
            val reopened = states.next()
            assertEquals(Money(500_000), reopened.availableNow)
            assertEquals(Money(30_000), reopened.obligationsTotal)
            assertEquals(pending.safeAmount, reopened.safeAmount)
            assertTrue(subscriptions.skipSubscriptionOccurrence(subscription.id, today.minusDays(1)) is SubscriptionResult.Saved)
            assertEquals(Money(500_000), states.next().safeAmount)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun subscriptionCardChargeCountsOnlyItsPendingInstallmentThroughPaymentAndUndo() = runBlocking {
        val cardId = card()
        val subscription = subscription(SubscriptionSource.Card(cardId), today, 70_000)
        val states = Channel<SafeToSpendResult>(Channel.UNLIMITED)
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            safe.observeSafeToSpend().collect { states.send((it as SafeToSpendState.Ready).result) }
        }
        try {
            val pending = states.next()
            assertTrue(pending.breakdown.single().source is ProjectedCashSource.Subscription)
            val charged = (subscriptions.paySubscriptionOccurrence(subscription.id, today, today) as SubscriptionResult.Saved).value
            val afterCharge = states.next()
            assertEquals(Money(70_000), afterCharge.obligationsTotal)
            assertEquals(Money(430_000), afterCharge.safeAmount)
            val source = afterCharge.breakdown.single().source as ProjectedCashSource.Installment
            assertEquals(charged.resultId, source.purchaseId)

            val paid = credit.payInstallment(source.installmentId, accountId, today) as InstallmentPaymentResult.Paid
            val afterPay = states.next()
            assertEquals(Money(430_000), afterPay.availableNow)
            assertEquals(Money(0), afterPay.obligationsTotal)
            assertTrue(credit.undoInstallmentPayment(source.installmentId, paid.transaction.id) is InstallmentPaymentResult.Undone)
            val afterUndoPayment = states.next()
            assertEquals(Money(500_000), afterUndoPayment.availableNow)
            assertEquals(Money(70_000), afterUndoPayment.obligationsTotal)
            assertTrue(subscriptions.undoSubscriptionOccurrence(subscription.id, today, charged.resultId!!) is SubscriptionUndoResult.Reopened)
            assertTrue(states.next().breakdown.single().source is ProjectedCashSource.Subscription)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun purchaseCancellationAndOverdueInstallmentsRespectTheHorizon() = runBlocking {
        val cardId = card()
        val purchase = (credit.createPurchase(UUID.randomUUID(), PurchaseDraft(
            cardId, categoryId, "Compra", Money(120_000), 2, today.minusMonths(2),
        )) as CreditResult.Saved).value
        val parts = credit.observeInstallments(purchase.id).first()
        assertTrue(parts.all { it.dueDate < today })
        val overdue = current()
        assertEquals(Money(120_000), overdue.obligationsTotal)
        assertEquals(today, overdue.lowestSlackDate)
        assertTrue(overdue.breakdown.all { it.date == today })
        assertEquals(2, overdue.breakdown.size)
        assertTrue(credit.cancelPurchase(purchase.id) is CreditResult.Saved)
        assertEquals(Money(500_000), current().safeAmount)

        val currentPurchase = (credit.createPurchase(UUID.randomUUID(), PurchaseDraft(
            cardId, categoryId, "Compra atual", Money(120_000), 2, today,
        )) as CreditResult.Saved).value
        val due = credit.observeInstallments(currentPurchase.id).first().first().dueDate
        val untilDue = java.time.temporal.ChronoUnit.DAYS.between(today, due).toInt()
        assertEquals(Money(0), current(untilDue).obligationsTotal)
        assertEquals(Money(60_000), current(untilDue + 1).obligationsTotal)
        assertEquals(Money(60_000), current().obligationsTotal)
    }

    @Test
    fun excludedAccountObligationsAndTerminalSubscriptionStatesUseEligibleCashOnly() = runBlocking {
        val excluded = account("Excluída", 1_000_000, included = false)
        subscription(SubscriptionSource.Account(excluded), today, 40_000)
        assertEquals(Money(500_000), current().safeAmount)
        val future = subscription(SubscriptionSource.Account(accountId), today.plusDays(3), 40_000)
        assertEquals(Money(460_000), current().safeAmount)
        assertTrue(subscriptions.pause(future.id) is SubscriptionResult.Saved)
        assertEquals(Money(500_000), current().safeAmount)
        assertTrue(subscriptions.resume(future.id) is SubscriptionResult.Saved)
        assertEquals(Money(460_000), current().safeAmount)
        assertTrue(subscriptions.cancel(future.id) is SubscriptionResult.Saved)
        assertEquals(Money(500_000), current().safeAmount)
    }

    @Test
    fun reopenedCancelledAnnualOccurrenceKeepsItsSnapshotAndOldDebtIsNotLost() = runBlocking {
        val anchor = LocalDate.of(2024, 2, 29)
        val annual = subscription(SubscriptionSource.Account(accountId), anchor, 60_000, SubscriptionFrequency.YEARLY)
        val paid = (subscriptions.paySubscriptionOccurrence(annual.id, anchor, today) as SubscriptionResult.Saved).value
        assertTrue(subscriptions.update(annual.id, annual.details.copy(amount = Money(65_000))) is SubscriptionResult.Saved)
        assertTrue(subscriptions.cancel(annual.id) is SubscriptionResult.Saved)
        assertTrue(subscriptions.undoSubscriptionOccurrence(annual.id, anchor, paid.resultId!!) is SubscriptionUndoResult.Reopened)
        val result = current()
        // Unpaid 2025 and 2026 occurrences remain, together with the reopened 2024 occurrence.
        assertEquals(Money(180_000), result.obligationsTotal)
        assertEquals(Money(320_000), result.safeAmount)
        assertEquals(setOf(anchor, LocalDate.of(2025, 2, 28), LocalDate.of(2026, 2, 28)),
            result.breakdown.map { (it.source as ProjectedCashSource.Subscription).key.occurrenceDate }.toSet())
    }

    @Test
    fun actualLedgerHasNoPendingOrFutureIncomeContractAndOverflowRemainsExplicit() = runBlocking {
        val incomeCategory = CategoryRepository(db.categoryDao(), clock).create(CategoryDetails("Receita", TransactionDirection.INCOME))
        val future = ledger.create(UUID.randomUUID(), TransactionDraft(
            TransactionDirection.INCOME, accountId, incomeCategory, Money(100_000), today.plusDays(1),
        ))
        assertEquals(TransactionResult.Rejected(TransactionError.FUTURE_DATE), future)
        assertEquals(Money(0), current().confirmedIncomeTotal)
        account("Extrema", Long.MAX_VALUE)
        assertEquals(SafeToSpendState.Unavailable(SafeToSpendError.OVERFLOW), safe.observeSafeToSpend().first())
    }

    @Test
    fun aggregateAvailabilityMatchesTheExistingBalanceContractWithWideIntermediateAmounts() = runBlocking {
        val account = account("Ampla", Long.MAX_VALUE)
        val incomeCategory = CategoryRepository(db.categoryDao(), clock).create(CategoryDetails("Receita", TransactionDirection.INCOME))
        val expense = ledger.create(UUID.randomUUID(), TransactionDraft(
            TransactionDirection.EXPENSE, account, categoryId, Money(Long.MAX_VALUE), today,
        ))
        val income = ledger.create(UUID.randomUUID(), TransactionDraft(
            TransactionDirection.INCOME, account, incomeCategory, Money(Long.MAX_VALUE), today,
        ))
        assertTrue(expense is TransactionResult.Saved)
        assertTrue(income is TransactionResult.Saved)
        assertEquals(CalculatedBalance.Overflow, goals.observeReserveBalances().first().availableBalance)
        assertEquals(SafeToSpendState.Unavailable(SafeToSpendError.OVERFLOW), safe.observeSafeToSpend().first())
        assertTrue(accounts.archive(account))
        assertEquals(Money(500_000), current().safeAmount)
    }

    private suspend fun current(days: Int = 30): SafeToSpendResult =
        (safe.observeSafeToSpend(days).first() as SafeToSpendState.Ready).result

    private suspend fun Channel<SafeToSpendResult>.next(): SafeToSpendResult = withTimeout(10_000) { receive() }

    private suspend fun account(name: String, cents: Long, included: Boolean = true): UUID =
        accounts.create(AccountDetails(name, AccountType.CHECKING, Money(cents), LocalDate.of(2024, 1, 1), included))

    private suspend fun card(): UUID =
        (credit.createCard(CreditCardDetails("Cartão", Money(1_000_000), BillingDay(20), BillingDay(27))) as CreditResult.Saved).value.id

    private suspend fun subscription(
        source: SubscriptionSource,
        anchor: LocalDate,
        cents: Long,
        frequency: SubscriptionFrequency = SubscriptionFrequency.MONTHLY,
    ): Subscription = (subscriptions.create(
        SubscriptionDetails("Assinatura", Money(cents), categoryId, source), frequency, anchor, BillingDay(anchor.dayOfMonth),
    ) as SubscriptionResult.Saved).value
}
