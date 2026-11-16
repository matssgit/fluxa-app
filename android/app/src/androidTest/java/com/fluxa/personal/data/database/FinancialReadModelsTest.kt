package com.fluxa.personal.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.PeriodSummary
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.model.TransactionHistoryPage
import com.fluxa.personal.domain.model.TransactionResult.Saved
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import com.fluxa.personal.domain.rules.CalculatedBalance.Overflow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinancialReadModelsTest {
    private val start = LocalDate.of(2026, 11, 1)
    private val end = start.plusMonths(1)
    private val clock = Clock.fixed(Instant.parse("2026-12-15T12:00:00Z"), ZoneOffset.UTC)
    private lateinit var database: FluxaDatabase
    private lateinit var ledger: TransactionRepository
    private lateinit var accountA: UUID
    private lateinit var accountB: UUID
    private lateinit var income: UUID
    private lateinit var expense: UUID

    @Before
    fun setUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, FluxaDatabase::class.java).build()
        ledger = TransactionRepository(database, clock, FinancialCalendar(clock) { ZoneOffset.UTC })
        val accounts = AccountRepository(database.accountDao(), clock)
        accountA = accounts.create(AccountDetails("A", AccountType.CHECKING, Money(100_000), start.plusDays(3)))
        accountB = accounts.create(AccountDetails("B", AccountType.WALLET, Money(200_000), start))
        val categories = CategoryRepository(database.categoryDao(), clock)
        income = categories.create(CategoryDetails("Entrada", INCOME))
        expense = categories.create(CategoryDetails("Saída", EXPENSE))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun novemberSummaryAndHistoryUseEffectiveDatesAndCombineFilters() = runBlocking {
        assertEquals(summary(0, 0), ledger.observePeriodSummary(start, end).first())
        assertEquals(summary(0, 0), ledger.observePeriodSummary(start, start).first())
        entry(1, EXPENSE, start.minusDays(1), 800, accountA)
        entry(2, INCOME, start, 100, accountA)
        entry(3, EXPENSE, start.plusDays(14), 120, accountB)
        entry(4, INCOME, end.minusDays(1), 60, accountB)
        entry(5, EXPENSE, end, 900, accountB)
        entry(6, INCOME, start.plusDays(19), 5000, accountB)
        assertTrue(ledger.cancel(id(6)) is Saved)

        assertEquals(summary(160, 120), ledger.observePeriodSummary(start, end).first())
        assertEquals(summary(0, 120), ledger.observePeriodSummary(start.plusDays(14), start.plusDays(15)).first())
        assertEquals(summary(100, 0), ledger.observePeriodSummary(start, start.plusDays(1)).first())
        assertThrows(IllegalArgumentException::class.java) { ledger.observePeriodSummary(end, start) }
        val filter = TransactionHistoryFilter(fromInclusive = start, untilExclusive = end)
        assertEquals(listOf(id(4), id(6), id(3), id(2)), history(filter))
        assertEquals(listOf(id(4), id(3), id(2)), history(filter.copy(includeCancelled = false)))
        assertEquals(listOf(id(4), id(6)), history(filter.copy(accountId = accountB, direction = INCOME, categoryId = income)))
        assertEquals(listOf(id(3)), history(filter.copy(accountId = accountB, direction = EXPENSE)))
        assertTrue(history(filter.copy(accountId = accountA, direction = EXPENSE)).isEmpty())
    }

    @Test
    fun pagesAndRecentUseStableOrderAndLimitAtTheDatabase() = runBlocking {
        for (number in 10 downTo 1) {
            entry(number, INCOME, if (number <= 4) start.plusDays(3) else start, 10, accountA)
        }
        entry(11, EXPENSE, start.plusDays(5), 20, accountB)
        assertTrue(ledger.cancel(id(11)) is Saved)
        val filter = TransactionHistoryFilter(direction = INCOME, accountId = accountA)
        val first = ledger.observeHistoryPage(filter, TransactionHistoryPage(4)).first()
        val second = ledger.observeHistoryPage(filter, TransactionHistoryPage(4, 4)).first()
        val last = ledger.observeHistoryPage(filter, TransactionHistoryPage(4, 8)).first()
        assertEquals((1..10).map(::id), (first + second + last).map { it.id })
        assertEquals(listOf(4, 4, 2), listOf(first.size, second.size, last.size))
        assertTrue(ledger.observeHistoryPage(filter, TransactionHistoryPage(4, 10)).first().isEmpty())

        val recent = ledger.observeRecentTransactions(5).first()
        assertEquals((1..5).map(::id), recent.map { it.id })
        assertThrows(IllegalArgumentException::class.java) { ledger.observeRecentTransactions(0) }
        Unit
    }

    @Test
    fun periodFlowRecoversFromSqliteOverflowAndPreservesExactNet() = runBlocking {
        val emissions = Channel<PeriodSummary>(Channel.UNLIMITED)
        val observer = launch { ledger.observePeriodSummary(start, end).collect { emissions.send(it) } }
        try {
            withTimeout(15_000) {
                assertEquals(summary(0, 0), emissions.receive())
                entry(1, INCOME, start, Long.MAX_VALUE, accountA)
                assertEquals(summary(Long.MAX_VALUE, 0), emissions.receive())
                entry(2, INCOME, start, 2, accountB)
                val failure = runCatching {
                    database.transactionDao().readPeriodTotals(start.toEpochDay(), end.toEpochDay())
                }.exceptionOrNull()
                assertTrue(failure?.message?.contains("integer overflow", ignoreCase = true) == true)
                assertEquals(PeriodSummary(Overflow, Exact(Money(0)), Overflow), emissions.receive())
                entry(3, EXPENSE, start, Long.MAX_VALUE, accountB)
                assertEquals(PeriodSummary(Overflow, Exact(Money(Long.MAX_VALUE)), Exact(Money(2))), emissions.receive())
                assertTrue(ledger.cancel(id(2)) is Saved)
                assertEquals(summary(Long.MAX_VALUE, Long.MAX_VALUE), emissions.receive())
            }
        } finally {
            observer.cancel()
            emissions.cancel()
        }
    }

    private suspend fun history(filter: TransactionHistoryFilter) =
        ledger.observeHistory(filter).first().map { it.id }

    private suspend fun entry(number: Int, direction: TransactionDirection, date: LocalDate, cents: Long, account: UUID) {
        val category = if (direction == INCOME) income else expense
        assertTrue(ledger.create(id(number), TransactionDraft(direction, account, category, Money(cents), date)) is Saved)
    }

    private fun id(number: Int) = UUID(0, number.toLong())

    private fun summary(income: Long, expenses: Long) =
        PeriodSummary(Exact(Money(income)), Exact(Money(expenses)), Exact(Money(income - expenses)))
}
