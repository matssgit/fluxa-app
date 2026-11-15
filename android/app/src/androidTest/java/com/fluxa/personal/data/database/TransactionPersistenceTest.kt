package com.fluxa.personal.data.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryKindInUse
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionError
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.model.TransactionResult.Rejected
import com.fluxa.personal.domain.model.TransactionResult.Saved
import com.fluxa.personal.domain.model.TransactionStatus
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import com.fluxa.personal.domain.rules.CalculatedBalance.Overflow
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionPersistenceTest {
    private val day = LocalDate.of(2026, 11, 15)
    private val clock = Clock.fixed(Instant.parse("2026-11-15T14:00:00Z"), ZoneOffset.UTC)
    private val name = "ledger-${UUID.randomUUID()}"
    private lateinit var context: Context
    private lateinit var database: FluxaDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private lateinit var ledger: TransactionRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = FluxaDatabase.create(context, name)
        accounts = AccountRepository(database.accountDao(), clock)
        categories = CategoryRepository(database.categoryDao(), clock)
        ledger = TransactionRepository(database, clock, FinancialCalendar(clock) { ZoneOffset.UTC })
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun balanceFlowReactsToCreateEditMoveCancelAndAccountArchive() = runBlocking {
        val first = accounts.create(AccountDetails("Banco", AccountType.CHECKING, Money(1000), day))
        val second = accounts.create(AccountDetails("Carteira", AccountType.WALLET, Money(2000), day))
        val income = categories.create(CategoryDetails("Receita", INCOME))
        val expense = categories.create(CategoryDetails("Mercado", EXPENSE))
        val id = UUID.randomUUID()
        val expenseId = UUID.randomUUID()
        val incomeDraft = TransactionDraft(INCOME, first, income, Money(250), day)

        val emissions = Channel<com.fluxa.personal.domain.rules.BalanceSnapshot>(Channel.UNLIMITED)
        val observer = launch { ledger.observeBalances().collect { emissions.send(it) } }
        try {
            withTimeout(15_000) {
                assertEquals(Exact(Money(3000)), emissions.receive().available)
                assertTrue(ledger.create(id, incomeDraft) is Saved)
                assertEquals(Exact(Money(1250)), emissions.receive().accounts[first])
                assertTrue(ledger.create(expenseId, TransactionDraft(EXPENSE, first, expense, Money(100), day)) is Saved)
                assertEquals(Exact(Money(3150)), emissions.receive().available)

                assertTrue(ledger.update(id, incomeDraft.copy(accountId = second, amount = Money(500))) is Saved)
                val moved = emissions.receive()
                assertEquals(mapOf(first to Exact(Money(900)), second to Exact(Money(2500))), moved.accounts)
                assertEquals(Exact(Money(3400)), moved.available)

                assertTrue(ledger.cancel(id) is Saved)
                assertEquals(Exact(Money(2900)), emissions.receive().available)
                assertEquals(TransactionStatus.CANCELLED, ledger.observe(id).first()!!.status)

                assertTrue(accounts.archive(first))
                val archived = emissions.receive()
                assertEquals(Exact(Money(900)), archived.accounts[first])
                assertEquals(Exact(Money(2000)), archived.available)
                assertEquals(Exact(Money(900)), ledger.observeAccountBalance(first).first())
            }
        } finally {
            observer.cancel()
            emissions.cancel()
        }
    }

    @Test
    fun concurrentRetriesAndRejectedEditsLeaveOneRowAndItsOriginalBalance() = runBlocking {
        val account = accounts.create(AccountDetails("Banco", AccountType.CHECKING, Money(0), day))
        val other = accounts.create(AccountDetails("Reserva", AccountType.SAVINGS, Money(9), day, false))
        val category = categories.create(CategoryDetails("Receita", INCOME))
        val expense = categories.create(CategoryDetails("Mercado", EXPENSE))
        val id = UUID.randomUUID()
        val draft = TransactionDraft(INCOME, account, category, Money(250), day)
        val results = List(2) { async { ledger.create(id, draft) } }.awaitAll()
        assertEquals(1, results.count { it is Saved })
        assertEquals(1, results.count { it == Rejected(TransactionError.DUPLICATE_ID) })

        assertEquals(Rejected(TransactionError.CATEGORY_DIRECTION_MISMATCH), ledger.update(id, draft.copy(categoryId = expense)))
        assertEquals(Rejected(TransactionError.ACCOUNT_NOT_FOUND), ledger.update(id, draft.copy(accountId = UUID.randomUUID())))
        assertEquals(Rejected(TransactionError.FUTURE_DATE), ledger.update(id, draft.copy(occurredOn = day.plusDays(1))))
        assertEquals(draft, ledger.observeHistory().first().single().details)
        assertEquals(Exact(Money(250)), ledger.observeAvailableBalance().first())
        assertEquals(Exact(Money(9)), ledger.observeAccountBalance(other).first())
        assertNull(ledger.observeAccountBalance(UUID.randomUUID()).first())
    }

    @Test
    fun historyFiltersInclusiveStartExclusiveEndAndKeepsPreOpeningAndCancelledRows() = runBlocking {
        val account = accounts.create(AccountDetails("Banco", AccountType.CHECKING, Money(100), day.minusDays(1)))
        val other = accounts.create(AccountDetails("Reserva", AccountType.SAVINGS, Money(0), day))
        val income = categories.create(CategoryDetails("Receita", INCOME))
        val early = UUID.randomUUID()
        val boundary = UUID.randomUUID()
        val after = UUID.randomUUID()
        val elsewhere = UUID.randomUUID()
        val draft = TransactionDraft(INCOME, account, income, Money(50), day)
        assertTrue(ledger.create(early, draft.copy(occurredOn = day.minusDays(2))) is Saved)
        assertTrue(ledger.create(boundary, draft.copy(occurredOn = day.minusDays(1))) is Saved)
        assertTrue(ledger.create(after, draft) is Saved)
        assertTrue(ledger.create(elsewhere, draft.copy(accountId = other)) is Saved)
        assertTrue(ledger.cancel(after) is Saved)

        val filter = TransactionHistoryFilter(accountId = account, categoryId = income, fromInclusive = day.minusDays(1), untilExclusive = day)
        assertEquals(listOf(boundary), ledger.observeHistory(filter).first().map { it.id })
        assertEquals(
            listOf(after, boundary, early),
            ledger.observeHistory(filter.copy(fromInclusive = null, untilExclusive = null)).first().map { it.id },
        )
        assertEquals(
            listOf(boundary, early),
            ledger.observeHistory(TransactionHistoryFilter(accountId = account, includeCancelled = false)).first().map { it.id },
        )
        assertEquals(Exact(Money(150)), ledger.observeAccountBalance(account).first())
    }

    @Test
    fun archivedReferencesAndCancelledRowsKeepTheirHistoryAndBlockCategoryKindChange() = runBlocking(Dispatchers.IO) {
        val account = accounts.create(AccountDetails("Banco", AccountType.CHECKING, Money(0), day))
        val category = categories.create(CategoryDetails("Receita", INCOME))
        val id = UUID.randomUUID()
        val draft = TransactionDraft(INCOME, account, category, Money(10), day)
        assertTrue(ledger.create(id, draft) is Saved)
        assertTrue(categories.archive(category))
        assertEquals(Rejected(TransactionError.CATEGORY_ARCHIVED), ledger.create(UUID.randomUUID(), draft))
        assertTrue(accounts.archive(account))
        assertEquals(Rejected(TransactionError.ACCOUNT_ARCHIVED), ledger.update(id, draft))
        assertTrue(ledger.cancel(id) is Saved)
        assertEquals(Rejected(TransactionError.CANCELLED), ledger.cancel(id))
        assertTrue(categories.update(category, CategoryDetails("Receita antiga", INCOME)))
        val error = runCatching { categories.update(category, CategoryDetails("Despesa", EXPENSE)) }.exceptionOrNull()
        assertTrue(error is CategoryKindInUse)
        assertEquals(INCOME, categories.observe(category).first()!!.details.kind)

        for ((table, reference) in listOf("accounts" to account, "categories" to category)) {
            val deleteError = runCatching {
                database.openHelper.writableDatabase.execSQL("DELETE FROM $table WHERE id = ?", arrayOf(reference.toString()))
            }.exceptionOrNull()
            assertTrue(deleteError is SQLiteConstraintException)
        }
        assertEquals(TransactionStatus.CANCELLED, ledger.observeHistory().first().single().status)
    }

    @Test
    fun overflowIsObservableAndRecoversAfterCancellation() = runBlocking {
        val account = accounts.create(AccountDetails("Banco", AccountType.CHECKING, Money(1), day))
        val category = categories.create(CategoryDetails("Receita", INCOME))
        val id = UUID.randomUUID()
        assertTrue(ledger.create(id, TransactionDraft(INCOME, account, category, Money(Long.MAX_VALUE), day)) is Saved)
        assertEquals(Overflow, ledger.observeAvailableBalance().first())
        assertTrue(ledger.cancel(id) is Saved)
        assertEquals(Exact(Money(1)), ledger.observeAvailableBalance().first())
    }
}
