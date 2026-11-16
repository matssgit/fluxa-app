package com.fluxa.personal.data.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.data.repository.SubscriptionRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.*
import com.fluxa.personal.domain.model.SubscriptionResult.Rejected
import com.fluxa.personal.domain.model.SubscriptionResult.Saved
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionPersistenceTest {
    private class TestClock(var day: LocalDate) : Clock() {
        override fun instant(): Instant = day.atStartOfDay().toInstant(ZoneOffset.UTC)
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant(), zone)
    }

    private val clock = TestClock(LocalDate.of(2026, 11, 16))
    private val name = "subscriptions-${UUID.randomUUID()}"
    private lateinit var context: Context
    private lateinit var database: FluxaDatabase
    private lateinit var credit: CreditRepository
    private lateinit var ledger: TransactionRepository
    private lateinit var subscriptions: SubscriptionRepository
    private lateinit var account: UUID
    private lateinit var category: UUID

    @Before
    fun setUp() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = FluxaDatabase.create(context, name)
        val calendar = FinancialCalendar(clock) { ZoneOffset.UTC }
        credit = CreditRepository(database, clock, calendar)
        ledger = TransactionRepository(database, clock, calendar)
        subscriptions = SubscriptionRepository(database, credit, clock, calendar)
        account = AccountRepository(database.accountDao(), clock).create(
            AccountDetails("Banco", AccountType.CHECKING, Money(100000), LocalDate.of(2024, 1, 1)),
        )
        category = CategoryRepository(database.categoryDao(), clock).create(CategoryDetails("Compras", TransactionDirection.EXPENSE))
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun yearlyPaysFullAmountOnceAndKeepsSnapshotAfterEdits() = runBlocking {
        val date = LocalDate.of(2026, 10, 15)
        val subscription = create(date, SubscriptionSource.Account(account), 60000, SubscriptionFrequency.YEARLY)
        val pending = subscriptions.observeOverdueOccurrences().first().single()
        assertEquals(date, pending.key.occurrenceDate)
        assertEquals(SubscriptionOccurrenceStatus.OVERDUE, pending.status)
        assertEquals(listOf(date), subscriptions.observeOccurrences(date, date.plusYears(1)).first().map { it.key.occurrenceDate })

        val attempts = List(2) { async { subscriptions.paySubscriptionOccurrence(subscription.id, date, clock.day) } }.awaitAll()
        val paid = (attempts.single { it is Saved } as Saved).value
        assertEquals(1, attempts.count { it == Rejected(SubscriptionError.ALREADY_RESOLVED) })
        assertEquals(SubscriptionOccurrenceStatus.PAID, paid.status)
        val transaction = ledger.observeHistory().first().single()
        assertEquals(paid.resultId, transaction.id)
        assertEquals(paid.key, transaction.subscriptionOccurrence)
        assertEquals(Money(60000), transaction.details.amount)
        assertEquals(clock.day, transaction.details.occurredOn)
        assertEquals(Exact(Money(60000)), ledger.observePeriodSummary(clock.day, clock.day.plusDays(1)).first().expenses)
        assertEquals(TransactionResult.Rejected(TransactionError.MANAGED_BY_SUBSCRIPTION), ledger.cancel(transaction.id))

        val card = card(300000)
        assertTrue(subscriptions.update(subscription.id, subscription.details.copy(
            name = "Revisada", amount = Money(64000), source = SubscriptionSource.Card(card),
        )) is Saved)
        assertEquals(paid, subscriptions.observeOccurrence(subscription.id, date).first())
        val next = subscriptions.observeNextOccurrence(subscription.id).first()!!
        assertEquals(date.plusYears(1), next.key.occurrenceDate)
        assertEquals(Money(64000), next.details.amount)
        assertEquals(SubscriptionSource.Card(card), next.details.source)
    }

    @Test
    fun cardChargeCreatesOneParcelAndRejectsDuplicateIncludingDirectCrossChannelInsert() = runBlocking {
        val card = card(1000)
        val subscription = create(clock.day, SubscriptionSource.Card(card), 700)
        val results = List(2) { async {
            subscriptions.paySubscriptionOccurrence(subscription.id, clock.day, clock.day)
        } }.awaitAll()
        val charge = (results.single { it is Saved } as Saved).value
        assertEquals(1, results.count { it == Rejected(SubscriptionError.ALREADY_RESOLVED) })
        assertEquals(SubscriptionOccurrenceStatus.CHARGED, charge.status)
        val purchase = credit.observePurchases(card).first().single()
        assertEquals(charge.resultId, purchase.id)
        assertEquals(charge.key, purchase.subscriptionOccurrence)
        assertEquals(1, purchase.details.installmentCount)
        val installment = credit.observeInstallments(purchase.id).first().single()
        assertEquals(Money(700), installment.amount)
        assertEquals(InstallmentOrigin(installment.id, purchase.id, card), credit.observeInstallmentOrigin(installment.id).first())
        assertNull(credit.observeInstallmentOrigin(UUID.randomUUID()).first())
        assertEquals(Money(300), credit.observeCard(card).first()!!.availableLimit)
        assertTrue(ledger.observeHistory().first().isEmpty())
        val conflict = runCatching {
            withContext(Dispatchers.IO) {
                val stamp = clock.instant().toString()
                database.openHelper.writableDatabase.execSQL(
                    """
                    INSERT INTO financial_transactions
                    (id,accountId,categoryId,direction,amountCents,occurredOn,createdAt,updatedAt,subscriptionId,occurrenceDate)
                    VALUES (?,?,?,'EXPENSE',700,?,?,?,?,?)
                    """.trimIndent(),
                    arrayOf(UUID.randomUUID().toString(), account.toString(), category.toString(), clock.day.toEpochDay(),
                        stamp, stamp, subscription.id.toString(), clock.day.toEpochDay()),
                )
            }
        }.exceptionOrNull()
        assertTrue(conflict is SQLiteConstraintException)
    }

    @Test
    fun sourceEditRacingWithPaymentCannotChangeTheDueOccurrence() = runBlocking {
        val card = card(1000)
        val date = clock.day
        val subscription = create(date, SubscriptionSource.Account(account), 700)
        val editing = async { subscriptions.update(subscription.id, subscription.details.copy(source = SubscriptionSource.Card(card))) }
        val resolving = async { subscriptions.paySubscriptionOccurrence(subscription.id, date, date) }
        assertTrue(editing.await() is Saved)
        assertEquals(SubscriptionOccurrenceStatus.PAID, (resolving.await() as Saved).value.status)
        assertEquals(account, ledger.observeHistory().first().single().details.accountId)
        assertEquals(Rejected(SubscriptionError.ALREADY_RESOLVED), subscriptions.paySubscriptionOccurrence(subscription.id, date, date))
        assertTrue(credit.observePurchases(card).first().isEmpty())
    }

    @Test
    fun insufficientLimitAndFailedResolutionRollbackNestedCreditWrites() = runBlocking {
        val card = card(500)
        val unaffordable = create(clock.day, SubscriptionSource.Card(card), 501)
        assertEquals(Rejected(SubscriptionError.INSUFFICIENT_CARD_LIMIT),
            subscriptions.paySubscriptionOccurrence(unaffordable.id, clock.day, clock.day))
        assertEquals(SubscriptionOccurrenceStatus.PENDING, subscriptions.observeOccurrence(unaffordable.id, clock.day).first()!!.status)
        val affordable = create(clock.day, SubscriptionSource.Card(card), 400)
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_resolution BEFORE INSERT ON subscription_resolutions BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
        }
        assertTrue(runCatching {
            subscriptions.paySubscriptionOccurrence(affordable.id, clock.day, clock.day)
        }.exceptionOrNull() is SQLiteConstraintException)
        assertTrue(credit.observePurchases(card).first().isEmpty())
        assertEquals(Money(500), credit.observeCard(card).first()!!.availableLimit)
        assertEquals(SubscriptionOccurrenceStatus.PENDING, subscriptions.observeOccurrence(affordable.id, clock.day).first()!!.status)
    }

    @Test
    fun skipPauseResumeAndCancelPreserveHistoryWithoutBackfillingPause() = runBlocking {
        val start = clock.day.minusDays(1)
        val subscription = create(start, SubscriptionSource.Account(account), 300)
        assertEquals(SubscriptionOccurrenceStatus.OVERDUE, subscriptions.observeOverdueOccurrences().first().single().status)
        val skipped = (subscriptions.skipSubscriptionOccurrence(subscription.id, start) as Saved).value
        assertEquals(SubscriptionOccurrenceStatus.SKIPPED, skipped.status)
        assertTrue(subscriptions.observeOverdueOccurrences().first().isEmpty())
        assertEquals(Rejected(SubscriptionError.SKIPPED), subscriptions.paySubscriptionOccurrence(subscription.id, start, clock.day))
        assertTrue(subscriptions.pause(subscription.id) is Saved)
        val missed = start.plusMonths(1)
        clock.day = missed
        assertTrue(subscriptions.resume(subscription.id) is Saved)
        assertNull(subscriptions.observeOccurrence(subscription.id, missed).first())
        val next = start.plusMonths(2)
        assertEquals(next, subscriptions.observeUpcomingOccurrences(32).first().single().key.occurrenceDate)
        assertTrue(subscriptions.cancel(subscription.id) is Saved)
        assertEquals(Rejected(SubscriptionError.CANCELLED), subscriptions.resume(subscription.id))
        assertNull(subscriptions.observeNextOccurrence(subscription.id).first())
        assertEquals(skipped, subscriptions.observeOccurrence(subscription.id, start).first())
        assertTrue(ledger.observeHistory().first().isEmpty())
    }

    private suspend fun card(limit: Long): UUID =
        (credit.createCard(CreditCardDetails("Cartão", Money(limit), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id

    private suspend fun create(
        anchor: LocalDate, source: SubscriptionSource, cents: Long, frequency: SubscriptionFrequency = SubscriptionFrequency.MONTHLY,
    ): Subscription = (subscriptions.create(
        SubscriptionDetails("Assinatura", Money(cents), category, source), frequency, anchor, BillingDay(anchor.dayOfMonth),
    ) as Saved).value
}
