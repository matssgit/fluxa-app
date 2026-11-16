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
import kotlinx.coroutines.withTimeout
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

    @Test
    fun undoAccountRecomposesCashAndReopensTheSameAnnualSnapshot() = runBlocking {
        val date = clock.day.minusMonths(1)
        val subscription = create(date, SubscriptionSource.Account(account), 60000, SubscriptionFrequency.YEARLY)
        val paid = pay(subscription, date)
        val transaction = ledger.observeHistory().first().single()
        assertEquals(TransactionResult.Rejected(TransactionError.MANAGED_BY_SUBSCRIPTION), ledger.update(transaction.id, transaction.details))
        assertEquals(Exact(Money(40000)), ledger.observeAccountBalance(account).first())
        assertEquals(SubscriptionResolutionDetail.Account(transaction.id, clock.day),
            subscriptions.observeOccurrenceDetail(subscription.id, date).first()?.resolution)
        assertTrue(subscriptions.update(subscription.id, subscription.details.copy(amount = Money(64000))) is Saved)

        val balances = async {
            withTimeout(5000) {
                ledger.observeAccountBalance(account).first { it == Exact(Money(100000)) }
            }
        }
        val reopened = (undo(paid) as SubscriptionUndoResult.Reopened).occurrence
        assertEquals(Exact(Money(100000)), balances.await())
        assertTrue(ledger.observeHistory().first().isEmpty())
        assertEquals(Exact(Money(0)), ledger.observePeriodSummary(clock.day, clock.day.plusDays(1)).first().expenses)
        assertEquals(SubscriptionOccurrenceStatus.OVERDUE, reopened.status)
        assertEquals(paid.details, reopened.details)
        assertEquals(reopened, subscriptions.observeOverdueOccurrences().first().single())
        assertEquals(SubscriptionOccurrenceDetail(reopened, null), subscriptions.observeOccurrenceDetail(subscription.id, date).first())
        assertEquals(SubscriptionUndoResult.Rejected(SubscriptionUndoError.NOT_RESOLVED), undo(paid))

        val paidAgain = pay(subscription, date)
        assertNotEquals(paid.resultId, paidAgain.resultId)
        assertEquals(SubscriptionUndoResult.Rejected(SubscriptionUndoError.CONFLICT), undo(paid))
        assertEquals(paidAgain.resultId, ledger.observeHistory().first().single().id)
        assertEquals(date.plusYears(1), subscriptions.observeNextOccurrence(subscription.id).first()?.key?.occurrenceDate)
    }

    @Test
    fun undoCardRestoresLimitAndCanChargeAgain() = runBlocking {
        val card = card(1000)
        val subscription = create(clock.day, SubscriptionSource.Card(card), 700)
        val charge = pay(subscription)
        val id = requireNotNull(charge.resultId)
        val part = credit.observeInstallments(id).first().single()
        assertEquals(SubscriptionResolutionDetail.Card(id, part.id, clock.day),
            subscriptions.observeOccurrenceDetail(subscription.id, clock.day).first()?.resolution)
        assertEquals(Money(300), credit.observeCard(card).first()?.availableLimit)
        assertEquals(CreditResult.Rejected(CreditError.MANAGED_BY_SUBSCRIPTION), credit.cancelPurchase(id))
        val reopened = (undo(charge) as SubscriptionUndoResult.Reopened).occurrence
        assertEquals(reopened, subscriptions.observeUpcomingOccurrences(7).first().single())
        assertEquals(reopened, subscriptions.observeNextOccurrence(subscription.id).first())
        assertEquals(Money(1000), credit.observeCard(card).first()?.availableLimit)
        assertTrue(credit.observePurchases(card).first().isEmpty())
        assertTrue(credit.observeInstallments(id).first().isEmpty())
        val restored = pay(subscription)
        assertNotEquals(id, restored.resultId)
        assertEquals(restored.resultId, credit.observePurchases(card).first().single().id)
    }

    @Test
    fun payingAnInstallmentBlocksUndoUntilTheInstallmentPaymentIsUndone() = runBlocking {
        val card = card(1000)
        val subscription = create(clock.day, SubscriptionSource.Card(card), 700)
        val charge = pay(subscription)
        val part = credit.observeInstallments(requireNotNull(charge.resultId)).first().single()
        val paid = credit.payInstallment(part.id, account, clock.day) as InstallmentPaymentResult.Paid
        assertEquals(SubscriptionUndoResult.Rejected(SubscriptionUndoError.CARD_CHARGE_ALREADY_PAID), undo(charge))
        assertEquals(charge, subscriptions.observeOccurrence(subscription.id, clock.day).first())
        assertTrue(credit.undoInstallmentPayment(part.id, paid.transaction.id) is InstallmentPaymentResult.Undone)
        assertTrue(undo(charge) is SubscriptionUndoResult.Reopened)
    }

    @Test
    fun legacyCancelledChargeCanBeReconciledAfterCardArchiveAndSubscriptionCancellation() = runBlocking {
        val card = card(1000)
        val subscription = create(clock.day, SubscriptionSource.Card(card), 700)
        val charge = pay(subscription)
        val id = requireNotNull(charge.resultId).toString()
        val dao = database.creditDao()
        val original = checkNotNull(dao.findPurchase(id))
        // Recreate the state allowed by the v6 repository before Sprint 5-C.
        dao.updatePurchase(original.copy(cancelledAt = clock.instant().toString()))
        dao.cancelPendingInstallments(id)
        assertTrue(credit.archiveCard(card))
        assertTrue(subscriptions.cancel(subscription.id) is Saved)
        val reopened = (undo(charge) as SubscriptionUndoResult.Reopened).occurrence
        assertEquals(reopened, subscriptions.observeOccurrence(subscription.id, clock.day).first())
        assertEquals(SubscriptionStatus.CANCELLED, subscriptions.observeSubscription(subscription.id).first()?.status)
        assertNull(dao.findPurchase(id))
    }

    @Test
    fun scopedHistoryContainsOnlyItsOwnDatesAndOutcomes() = runBlocking {
        val start = clock.day.minusMonths(2)
        val a = create(start, SubscriptionSource.Account(account), 700)
        val b = create(start, SubscriptionSource.Account(account), 700)
        val paid = pay(a, start)
        assertTrue(subscriptions.skipSubscriptionOccurrence(a.id, start.plusMonths(1)) is Saved)
        pay(b)
        val history = subscriptions.observeOccurrences(a.id, start, clock.day.plusDays(1)).first()
        assertEquals(listOf(SubscriptionOccurrenceStatus.PENDING, SubscriptionOccurrenceStatus.SKIPPED, SubscriptionOccurrenceStatus.PAID),
            history.map { it.status })
        assertTrue(history.all { it.key.subscriptionId == a.id })
        assertEquals(history.take(2), subscriptions.observeOccurrences(a.id, start, clock.day.plusDays(1), limit = 2).first())
        assertEquals(SubscriptionUndoResult.Rejected(SubscriptionUndoError.SKIPPED),
            subscriptions.undoSubscriptionOccurrence(a.id, start.plusMonths(1), UUID.randomUUID()))

        assertTrue(AccountRepository(database.accountDao(), clock).archive(account))
        assertTrue(subscriptions.pause(a.id) is Saved)
        assertTrue(undo(paid) is SubscriptionUndoResult.Reopened)
        assertEquals(SubscriptionStatus.PAUSED, subscriptions.observeSubscription(a.id).first()?.status)
        assertEquals(SubscriptionOccurrenceStatus.OVERDUE, subscriptions.observeOccurrence(a.id, start).first()?.status)
        assertEquals(Rejected(SubscriptionError.INVALID_SOURCE),
            subscriptions.paySubscriptionOccurrence(b.id, start, clock.day))
    }

    @Test
    fun concurrentUndoAndResolutionKeepExactlyOneMatchingEffectOrAnOpenOccurrence() = runBlocking {
        for (source in listOf(SubscriptionSource.Account(account), SubscriptionSource.Card(card(1000)))) {
            val subscription = create(clock.day, source, 700)
            val original = pay(subscription)
            val settling = async(Dispatchers.Default) { subscriptions.paySubscriptionOccurrence(subscription.id, clock.day, clock.day) }
            val reopening = async(Dispatchers.Default) { undo(original) }
            val outcome = settling.await()
            assertTrue(reopening.await() is SubscriptionUndoResult.Reopened)
            val final = subscriptions.observeOccurrence(subscription.id, clock.day).first()
            val ids = when (source) {
                is SubscriptionSource.Account -> ledger.observeHistory().first().filter { it.subscriptionOccurrence == original.key }.map { it.id }
                is SubscriptionSource.Card -> credit.observePurchases(source.id).first().filter { it.subscriptionOccurrence == original.key }.map { it.id }
            }
            if (outcome is Saved) {
                assertEquals(outcome.value, final)
                assertEquals(listOf(outcome.value.resultId), ids)
            } else {
                assertEquals(Rejected(SubscriptionError.ALREADY_RESOLVED), outcome)
                assertEquals(SubscriptionOccurrenceStatus.PENDING, final?.status)
                assertTrue(ids.isEmpty())
            }
        }
    }

    @Test
    fun concurrentUndoAndInstallmentPaymentKeepPurchaseAndCashConsistent() = runBlocking {
        val card = card(1000)
        val subscription = create(clock.day, SubscriptionSource.Card(card), 700)
        val original = pay(subscription)
        val part = credit.observeInstallments(requireNotNull(original.resultId)).first().single()
        val paying = async(Dispatchers.Default) { credit.payInstallment(part.id, account, clock.day) }
        val undoing = async(Dispatchers.Default) { undo(original) }
        when (val outcome = undoing.await()) {
            is SubscriptionUndoResult.Reopened -> {
                assertEquals(InstallmentPaymentResult.Rejected(InstallmentPaymentError.INSTALLMENT_NOT_FOUND), paying.await())
                assertEquals(outcome.occurrence, subscriptions.observeOccurrence(subscription.id, clock.day).first())
                assertTrue(credit.observePurchases(card).first().isEmpty())
                assertTrue(ledger.observeHistory().first().isEmpty())
            }
            is SubscriptionUndoResult.Rejected -> {
                assertEquals(SubscriptionUndoError.CARD_CHARGE_ALREADY_PAID, outcome.error)
                val paid = paying.await() as InstallmentPaymentResult.Paid
                assertEquals(original, subscriptions.observeOccurrence(subscription.id, clock.day).first())
                assertEquals(original.resultId, credit.observePurchases(card).first().single().id)
                assertEquals(paid.transaction.id, ledger.observeHistory().first().single().id)
            }
        }
    }

    @Test
    fun aFailedReopenRollsBackTheRemovalOfItsCardPurchaseAndInstallment() = runBlocking {
        val card = card(1000)
        val subscription = create(clock.day, SubscriptionSource.Card(card), 700)
        val charge = pay(subscription)
        val purchaseId = requireNotNull(charge.resultId)
        val part = credit.observeInstallments(purchaseId).first().single()
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_reopen BEFORE UPDATE ON subscription_resolutions BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
        }
        assertTrue(runCatching { undo(charge) }.exceptionOrNull() is SQLiteConstraintException)
        assertEquals(charge, subscriptions.observeOccurrence(subscription.id, clock.day).first())
        assertEquals(PurchaseStatus.ACTIVE, credit.observePurchases(card).first().single().status)
        assertEquals(part, credit.observeInstallments(purchaseId).first().single())
        assertEquals(Money(300), credit.observeCard(card).first()?.availableLimit)
    }

    private suspend fun pay(subscription: Subscription, date: LocalDate = clock.day): SubscriptionOccurrence =
        (subscriptions.paySubscriptionOccurrence(subscription.id, date, clock.day) as Saved).value

    private suspend fun undo(occurrence: SubscriptionOccurrence) = subscriptions.undoSubscriptionOccurrence(
        occurrence.key.subscriptionId, occurrence.key.occurrenceDate, requireNotNull(occurrence.resultId),
    )

    private suspend fun card(limit: Long): UUID =
        (credit.createCard(CreditCardDetails("Cartão", Money(limit), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id

    private suspend fun create(
        anchor: LocalDate, source: SubscriptionSource, cents: Long, frequency: SubscriptionFrequency = SubscriptionFrequency.MONTHLY,
    ): Subscription = (subscriptions.create(
        SubscriptionDetails("Assinatura", Money(cents), category, source), frequency, anchor, BillingDay(anchor.dayOfMonth),
    ) as Saved).value
}
