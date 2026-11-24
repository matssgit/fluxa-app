package com.fluxa.personal.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.*
import com.fluxa.personal.domain.model.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialAnalyticsPersistenceTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val name="analytics-"+UUID.randomUUID()
    private val clock=Clock.fixed(Instant.parse("2024-03-01T01:00:00Z"),ZoneOffset.UTC)
    private val calendar=FinancialCalendar(clock) { ZoneOffset.ofHours(-3) }
    private val today=LocalDate.of(2024,2,29)
    private val month=YearMonth.of(2024,2)
    private lateinit var db: FluxaDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private lateinit var ledger: TransactionRepository
    private lateinit var credit: CreditRepository
    private lateinit var subscriptions: SubscriptionRepository
    private lateinit var goals: GoalRepository
    private lateinit var safe: SafeToSpendRepository
    private lateinit var analytics: FinancialAnalyticsRepository
    private lateinit var account: UUID
    private lateinit var expense: UUID
    private lateinit var income: UUID

    private fun repositories() {
        accounts=AccountRepository(db.accountDao(),clock)
        categories=CategoryRepository(db.categoryDao(),clock)
        ledger=TransactionRepository(db,clock,calendar)
        credit=CreditRepository(db,clock,calendar)
        subscriptions=SubscriptionRepository(db,credit,clock,calendar)
        goals=GoalRepository(db,clock,calendar)
        safe=SafeToSpendRepository(db,goals,clock,calendar)
        analytics=FinancialAnalyticsRepository(db,safe,calendar)
    }
    @Before fun setup()=runBlocking<Unit> {
        db=FluxaDatabase.create(context,name)
        repositories()
        account=accounts.create(AccountDetails("Principal",AccountType.CHECKING,Money(500000),LocalDate.of(2023,1,1)))
        expense=categories.create(CategoryDetails("Mercado",TransactionDirection.EXPENSE))
        income=categories.create(CategoryDetails("Salário",TransactionDirection.INCOME))
    }
    @After fun close() { db.close();context.deleteDatabase(name) }
    private suspend fun snapshot(selected: YearMonth = month, days: Int = 30) =
        (analytics.observeFinancialAnalytics(selected,days).first() as FinancialAnalyticsState.Ready).snapshot
    private suspend fun tx(direction: TransactionDirection, cents: Long, date: LocalDate=today): FinancialTransaction =
        (ledger.create(UUID.randomUUID(),TransactionDraft(direction,account,
            if(direction==TransactionDirection.INCOME) income else expense,Money(cents),date)) as TransactionResult.Saved).transaction
    private suspend fun subscription(source: SubscriptionSource, cents: Long, frequency: SubscriptionFrequency=SubscriptionFrequency.YEARLY,
                                     anchor: LocalDate=today): Subscription =
        (subscriptions.create(SubscriptionDetails("Plano",Money(cents),expense,source),frequency,anchor,BillingDay(anchor.dayOfMonth)) as SubscriptionResult.Saved).value
    private suspend fun card()=(credit.createCard(CreditCardDetails("Cartão",Money(1000000),BillingDay(20),BillingDay(27))) as CreditResult.Saved).value.id

    @Test fun summaryUsesOnlyRealizedLedgerAndLocalMonthsAndPreservesArchivedCategory()=runBlocking<Unit> {
        val empty=snapshot()
        assertEquals(Money(0),empty.monthSummary.incomeRealized)
        assertEquals(0L,empty.monthSummary.transactionCount)
        assertNull(empty.monthSummary.savingsRate)
        assertTrue(empty.categorySpending.isEmpty())
        tx(TransactionDirection.INCOME,10000)
        tx(TransactionDirection.EXPENSE,15000)
        val cancelled=tx(TransactionDirection.EXPENSE,999)
        ledger.cancel(cancelled.id)
        tx(TransactionDirection.INCOME,2000,LocalDate.of(2024,1,31))
        tx(TransactionDirection.EXPENSE,100,LocalDate.of(2023,8,31)) // outside six-month range
        categories.archive(expense)
        val result=snapshot()
        assertEquals(today,result.asOfDate) // UTC is already March 1.
        assertEquals(Money(10000),result.monthSummary.incomeRealized)
        assertEquals(Money(15000),result.monthSummary.expenseRealized)
        assertEquals(Money(-5000),result.monthSummary.netResult)
        assertEquals(2L,result.monthSummary.transactionCount)
        assertEquals(100,result.categorySpending.single().percentage)
        assertEquals(expense,result.categorySpending.single().categoryId)
        assertEquals(YearMonth.of(2023,9),result.sixMonthEvolution.first().month)
        assertEquals(Money(0),result.sixMonthEvolution.first().net)
        assertEquals(Money(2000),result.sixMonthEvolution[4].income)
        assertEquals(LocalDate.of(2024,3,1),result.remainingMonthProjection.horizonEndExclusive)
        assertEquals(today,snapshot(YearMonth.of(2024,1)).projection.horizonStart)
        assertEquals(TransactionResult.Rejected(TransactionError.FUTURE_DATE),ledger.create(UUID.randomUUID(),
            TransactionDraft(TransactionDirection.INCOME,account,income,Money(100),today.plusDays(1))))
        assertEquals(Money(0),result.projection.confirmedIncomeTotal)
    }

    @Test fun liveSnapshotReactsToReserveLedgerUndoCategoryAndAccountWithoutDoubleDeduction()=runBlocking<Unit> {
        val channel=Channel<FinancialAnalyticsSnapshot>(Channel.UNLIMITED)
        val job=launch(start=CoroutineStart.UNDISPATCHED) {
            analytics.observeFinancialAnalytics().collect { channel.send((it as FinancialAnalyticsState.Ready).snapshot) }
        }
        suspend fun await(predicate:(FinancialAnalyticsSnapshot)->Boolean)=withTimeout(15000) {
            var item=channel.receive()
            while(!predicate(item)) item=channel.receive()
            item
        }
        try {
            assertEquals(month,await { true }.monthSummary.month)
            val goal=(goals.createGoal(GoalDetails("Reserva",Money(150000),account)) as GoalResult.Saved).value
            goals.allocateToGoal(goal.id,Money(150000))
            val reserved=await {it.projection.availableNow==Money(350000)}
            assertEquals(Money(350000),reserved.projection.projectedEndingBalance)
            assertEquals(Money(350000),reserved.projection.lowestProjectedBalance)
            assertEquals((safe.observeSafeToSpend().first() as SafeToSpendState.Ready).result.safeAmount,reserved.projection.availableNow)
            val payment=tx(TransactionDirection.EXPENSE,5000)
            assertEquals(Money(345000),await {it.monthSummary.transactionCount==1L}.projection.availableNow)
            categories.update(expense,CategoryDetails("Renomeada",TransactionDirection.EXPENSE))
            await {it.categorySpending.singleOrNull()?.categoryName=="Renomeada"}
            ledger.cancel(payment.id)
            assertEquals(Money(350000),await {it.monthSummary.transactionCount==0L}.projection.availableNow)
            goals.releaseFromGoal(goal.id,Money(50000))
            await {it.projection.availableNow==Money(400000)}
            val excluded=accounts.create(AccountDetails("Excluída",AccountType.SAVINGS,Money(1000000),today, false))
            val other=(goals.createGoal(GoalDetails("Longo prazo",Money(200000),excluded)) as GoalResult.Saved).value
            goals.allocateToGoal(other.id,Money(200000))
            assertEquals(Money(400000),snapshot().projection.availableNow)
            accounts.update(account,AccountDetails("Principal",AccountType.CHECKING,Money(500000),LocalDate.of(2023,1,1),false))
            assertEquals(Money(0),await {it.projection.availableNow==Money(0)}.projection.projectedEndingBalance)
        } finally { job.cancel();channel.close() }
    }

    @Test fun purchasePaymentsAndBothSubscriptionSourcesCountOnlyLedgerRowsAndUndoReacts()=runBlocking<Unit> {
        val card=card()
        val purchase=(credit.createPurchase(UUID.randomUUID(),PurchaseDraft(card,expense,"Parcelada",Money(6000),2,today.minusMonths(2))) as CreditResult.Saved).value
        val parts=credit.observeInstallments(purchase.id).first()
        assertEquals(0L,snapshot().monthSummary.transactionCount)
        assertEquals(Money(6000),snapshot().projection.obligationsTotal)
        assertEquals(today,snapshot().projection.lowestBalanceDate)
        val paid=credit.payInstallment(parts.first().id,account,today) as InstallmentPaymentResult.Paid
        assertEquals(Money(3000),snapshot().monthSummary.expenseRealized)
        val accountSub=subscription(SubscriptionSource.Account(account),4000)
        val paidSub=(subscriptions.paySubscriptionOccurrence(accountSub.id,today,today) as SubscriptionResult.Saved).value
        assertEquals(2L,snapshot().monthSummary.transactionCount)
        assertEquals(Money(7000),snapshot().categorySpending.single().amount)
        subscriptions.undoSubscriptionOccurrence(accountSub.id,today,paidSub.resultId!!)
        assertEquals(1L,snapshot().monthSummary.transactionCount)
        val cardSub=subscription(SubscriptionSource.Card(card),5000)
        val charged=(subscriptions.paySubscriptionOccurrence(cardSub.id,today,today) as SubscriptionResult.Saved).value
        assertEquals(1L,snapshot().monthSummary.transactionCount)
        val generatedPart=credit.observeInstallments(charged.resultId!!).first().single()
        val subPayment=credit.payInstallment(generatedPart.id,account,today) as InstallmentPaymentResult.Paid
        assertEquals(Money(8000),snapshot().monthSummary.expenseRealized)
        credit.undoInstallmentPayment(generatedPart.id,subPayment.transaction.id)
        assertEquals(Money(3000),snapshot().monthSummary.expenseRealized)
        credit.undoInstallmentPayment(parts.first().id,paid.transaction.id)
        assertEquals(0L,snapshot().monthSummary.transactionCount)
        assertTrue(snapshot().categorySpending.isEmpty())
        credit.cancelPurchase(purchase.id)
        assertEquals((safe.observeSafeToSpend().first() as SafeToSpendState.Ready).result.obligationsTotal,
            snapshot().projection.obligationsTotal)
    }

    @Test fun activeBudgetIgnoresPausedCancelledAndCountsAnnualAsBudgetNotCash()=runBlocking<Unit> {
        val monthly=subscription(SubscriptionSource.Account(account),1000,SubscriptionFrequency.MONTHLY,today.plusDays(3))
        subscription(SubscriptionSource.Account(account),1207,SubscriptionFrequency.YEARLY,today.plusDays(3))
        val cancelled=subscription(SubscriptionSource.Account(account),9999,anchor=today.plusDays(3))
        subscriptions.cancel(cancelled.id)
        val before=snapshot()
        assertEquals(2L,before.subscriptionAnalytics.activeCount)
        assertEquals(Money(1101),before.subscriptionAnalytics.monthlyEquivalentCost)
        assertEquals(Money(13207),before.subscriptionAnalytics.annualProjectedCost)
        assertEquals(0L,before.monthSummary.transactionCount)
        subscriptions.pause(monthly.id)
        assertEquals(1L,snapshot().subscriptionAnalytics.activeCount)
        assertEquals(Money(101),snapshot().subscriptionAnalytics.monthlyEquivalentCost)
        subscriptions.resume(monthly.id)
        assertEquals(2L,snapshot().subscriptionAnalytics.activeCount)
    }

    @Test fun horizonIsExclusiveAndReopeningKeepsNoDerivedPersistence()=runBlocking<Unit> {
        subscription(SubscriptionSource.Account(account),1200,anchor=today.plusDays(30))
        assertEquals(Money(0),snapshot().projection.obligationsTotal)
        assertEquals(Money(1200),snapshot(days=31).projection.obligationsTotal)
        assertEquals(Money(0),snapshot().remainingMonthProjection.obligationsTotal)
        tx(TransactionDirection.EXPENSE,10)
        val before=snapshot()
        db.close()
        db=FluxaDatabase.create(context,name)
        repositories()
        assertEquals(before,snapshot())
        assertEquals(7,db.openHelper.writableDatabase.version)
    }

    @Test fun ongoingFlowObservesSubscriptionStatesAndCreditPaymentUndoAtomically()=runBlocking<Unit> {
        val channel=Channel<FinancialAnalyticsSnapshot>(Channel.UNLIMITED)
        val job=launch(start=CoroutineStart.UNDISPATCHED) {
            analytics.observeFinancialAnalytics(month).collect { channel.send((it as FinancialAnalyticsState.Ready).snapshot) }
        }
        suspend fun await(predicate:(FinancialAnalyticsSnapshot)->Boolean)=withTimeout(15000) {
            var item=channel.receive()
            while(!predicate(item)) item=channel.receive()
            item
        }
        try {
            await {true}
            val sub=subscription(SubscriptionSource.Account(account),800,anchor=today.plusDays(3))
            assertEquals(Money(800),await {it.subscriptionAnalytics.activeCount==1L}.projection.obligationsTotal)
            subscriptions.pause(sub.id)
            assertEquals(Money(0),await {it.subscriptionAnalytics.activeCount==0L}.projection.obligationsTotal)
            subscriptions.resume(sub.id)
            await {it.subscriptionAnalytics.activeCount==1L}
            subscriptions.cancel(sub.id)
            await {it.subscriptionAnalytics.activeCount==0L}
            val purchase=(credit.createPurchase(UUID.randomUUID(),PurchaseDraft(card(),expense,"Compra",Money(1200),1,today.minusMonths(1))) as CreditResult.Saved).value
            assertEquals(0L,await {it.projection.obligationsTotal==Money(1200)}.monthSummary.transactionCount)
            val part=credit.observeInstallments(purchase.id).first().single()
            val paid=credit.payInstallment(part.id,account,today) as InstallmentPaymentResult.Paid
            val settled=await {it.monthSummary.transactionCount==1L}
            assertEquals(Money(498800),settled.projection.availableNow)
            assertEquals(Money(0),settled.projection.obligationsTotal)
            credit.undoInstallmentPayment(part.id,paid.transaction.id)
            val reopened=await {it.monthSummary.transactionCount==0L}
            assertEquals(Money(1200),reopened.projection.obligationsTotal)
            assertEquals(Money(500000),reopened.projection.availableNow)
            credit.cancelPurchase(purchase.id)
            await {it.projection.obligationsTotal==Money(0)}
        } finally { job.cancel();channel.close() }
    }

    @Test fun integerAggregateOverflowIsExplicitInsteadOfWrappingOrClamping()=runBlocking<Unit> {
        tx(TransactionDirection.EXPENSE,Long.MAX_VALUE)
        tx(TransactionDirection.EXPENSE,Long.MAX_VALUE)
        assertEquals(FinancialAnalyticsState.Unavailable(FinancialAnalyticsError.OVERFLOW),
            analytics.observeFinancialAnalytics(month).first())
    }
}
