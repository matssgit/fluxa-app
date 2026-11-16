package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.feature.cash.CashViewModel
import com.fluxa.personal.ui.feature.cash.DashboardViewModel
import com.fluxa.personal.ui.feature.cash.DirectionFilter
import com.fluxa.personal.ui.feature.cash.HistoryPeriod
import com.fluxa.personal.ui.feature.transactions.TransactionCompletion
import com.fluxa.personal.ui.feature.transactions.TransactionFormViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-15T15:00:00Z")
    private val today = LocalDate.of(2026, 11, 15)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val calendar = FinancialCalendar(clock) { ZoneOffset.UTC }
    private lateinit var accountDao: FakeAccountDao
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private lateinit var source: FakeTransactionSource

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        accountDao = FakeAccountDao()
        categoryDao = FakeCategoryDao()
        accounts = AccountRepository(accountDao, clock)
        categories = CategoryRepository(categoryDao, clock)
        source = FakeTransactionSource(accounts, categories, today, now)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun account(name: String, cents: Long, include: Boolean = true, opening: LocalDate = LocalDate.of(2026, 1, 1)) =
        accounts.create(AccountDetails(name, AccountType.CHECKING, Money(cents), opening, include))

    private fun form(id: UUID? = null, direction: com.fluxa.personal.domain.model.TransactionDirection = EXPENSE) =
        TransactionFormViewModel(source, accounts, categories, id, direction, calendar)

    private suspend fun balanceOf(id: UUID) = source.observeBalances().firstValue().accounts[id]
    private suspend fun available() = source.observeBalances().firstValue().available

    private fun TestScope.collect(flow: kotlinx.coroutines.flow.Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    @Test
    fun createsExpenseWithPositiveAmountAndBalanceComesFromTheSource() = runTest(dispatcher) {
        val wallet = account("Carteira", 100_000)
        val market = categories.create(CategoryDetails("Mercado", EXPENSE))
        categories.create(CategoryDetails("Salário", INCOME))
        val vm = form()
        advanceUntilIdle()

        with(vm.uiState.value) {
            assertEquals(today, occurredOn)
            assertEquals(wallet, accountId) // única conta ativa é pré-selecionada
            assertEquals(listOf("Mercado"), categories.map { it.details.name })
        }
        vm.onCategoryChange(market)
        vm.onAmountChange("250,50")
        vm.onDescriptionChange("  Feira  ")
        vm.save()
        advanceUntilIdle()

        assertEquals(TransactionCompletion.Created, vm.uiState.value.completion)
        val saved = source.rows.value.values.single().details
        assertEquals(Money(25_050), saved.amount)
        assertEquals(EXPENSE, saved.direction)
        assertEquals("Feira", saved.description)
        assertEquals(CalculatedBalance.Exact(Money(74_950)), balanceOf(wallet))
    }

    @Test
    fun incomeShowsOnlyIncomeCategoriesAndSwitchingDirectionClearsCategory() = runTest(dispatcher) {
        account("Conta", 0)
        val salary = categories.create(CategoryDetails("Salário", INCOME))
        categories.create(CategoryDetails("Mercado", EXPENSE))
        val archivedIncome = categories.create(CategoryDetails("Bônus antigo", INCOME))
        categories.archive(archivedIncome)

        val vm = form(direction = INCOME)
        advanceUntilIdle()
        assertEquals(listOf("Salário"), vm.uiState.value.categories.map { it.details.name })
        vm.onCategoryChange(salary)
        vm.onDirectionChange(EXPENSE)
        advanceUntilIdle()
        assertNull(vm.uiState.value.categoryId)
        assertEquals(listOf("Mercado"), vm.uiState.value.categories.map { it.details.name })
    }

    @Test
    fun archivedAccountsAreNotOffered() = runTest(dispatcher) {
        account("Ativa", 0)
        val old = account("Arquivada", 0)
        account("Outra", 0)
        accounts.archive(old)
        val vm = form()
        advanceUntilIdle()
        assertEquals(listOf("Ativa", "Outra"), vm.uiState.value.accounts.map { it.details.name })
        assertNull(vm.uiState.value.accountId) // mais de uma conta: o usuário escolhe
    }

    @Test
    fun validationCoversAmountAccountCategoryAndDate() = runTest(dispatcher) {
        account("A", 0)
        account("B", 0)
        val vm = form()
        advanceUntilIdle()

        vm.save()
        with(vm.uiState.value) {
            assertEquals(R.string.error_amount_positive, amountError)
            assertEquals(R.string.error_account_required, accountError)
            assertEquals(R.string.error_category_required, categoryError)
        }
        vm.onAmountChange("0,00")
        vm.save()
        assertEquals(R.string.error_amount_positive, vm.uiState.value.amountError)
        vm.onAmountChange("10,999")
        vm.save()
        assertEquals(R.string.error_amount_decimals, vm.uiState.value.amountError)
        vm.onAmountChange("99999999999999")
        vm.save()
        assertEquals(R.string.error_amount_too_large, vm.uiState.value.amountError)
        vm.onDateChange(today.plusDays(1))
        vm.save()
        assertEquals(R.string.error_future_date, vm.uiState.value.dateError)
        assertTrue(source.createdIds.isEmpty())
    }

    @Test
    fun repeatedTapsCreateOnceAndRetryReusesTheSameId() = runTest(dispatcher) {
        account("Conta", 0)
        val c = categories.create(CategoryDetails("Mercado", EXPENSE))
        val vm = form()
        advanceUntilIdle()
        vm.onCategoryChange(c)
        vm.onAmountChange("10")

        source.failWrites = true
        repeat(3) { vm.save() }
        advanceUntilIdle()
        assertEquals(R.string.error_save, vm.uiState.value.operationError)
        assertNull(vm.uiState.value.completion)

        source.failWrites = false
        repeat(3) { vm.save() }
        advanceUntilIdle()
        assertEquals(TransactionCompletion.Created, vm.uiState.value.completion)
        assertEquals(1, source.rows.value.size)
        assertEquals(1, source.createdIds.size)
    }

    @Test
    fun lostResponseIsRecoveredThroughDuplicateIdWithoutASecondRow() = runTest(dispatcher) {
        account("Conta", 0)
        val c = categories.create(CategoryDetails("Mercado", EXPENSE))
        val vm = form()
        advanceUntilIdle()
        vm.onCategoryChange(c)
        vm.onAmountChange("10")
        source.failAfterWrite = true
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_save, vm.uiState.value.operationError)

        vm.save()
        advanceUntilIdle()
        assertEquals(TransactionCompletion.Created, vm.uiState.value.completion)
        assertEquals(1, source.rows.value.size)
        assertEquals(2, source.createdIds.size)
        assertEquals(source.createdIds[0], source.createdIds[1])
    }

    @Test
    fun editLoadsAndUpdatesAndBalanceFollows() = runTest(dispatcher) {
        val acc = account("Conta", 50_000)
        val c = categories.create(CategoryDetails("Mercado", EXPENSE))
        val id = UUID.randomUUID()
        source.create(id, TransactionDraft(EXPENSE, acc, c, Money(10_000), today.minusDays(2), "Antes"))

        val vm = form(id)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals("100,00", amountText)
            assertEquals("Antes", description)
            assertEquals(today.minusDays(2), occurredOn)
            assertFalse(isReadOnly)
        }
        vm.onAmountChange("30,00")
        vm.save()
        advanceUntilIdle()
        assertEquals(TransactionCompletion.Updated, vm.uiState.value.completion)
        assertEquals(CalculatedBalance.Exact(Money(47_000)), balanceOf(acc))
    }

    @Test
    fun cancelKeepsHistoryRemovesEffectAndBlocksEditing() = runTest(dispatcher) {
        val acc = account("Conta", 50_000)
        val c = categories.create(CategoryDetails("Salário", INCOME))
        val id = UUID.randomUUID()
        source.create(id, TransactionDraft(INCOME, acc, c, Money(20_000), today, null))
        assertEquals(CalculatedBalance.Exact(Money(70_000)), balanceOf(acc))

        val vm = form(id)
        advanceUntilIdle()
        vm.cancelTransaction()
        vm.cancelTransaction()
        advanceUntilIdle()
        assertEquals(TransactionCompletion.Cancelled, vm.uiState.value.completion)
        assertEquals(CalculatedBalance.Exact(Money(50_000)), balanceOf(acc))
        val history = source.observeHistory(TransactionHistoryFilter()).firstValue()
        assertNotNull(history.single().cancelledAt)

        val reopened = form(id)
        advanceUntilIdle()
        assertTrue(reopened.uiState.value.isReadOnly)
        reopened.onAmountChange("1")
        reopened.save()
        advanceUntilIdle()
        assertNull(reopened.uiState.value.completion)
        assertEquals(Money(20_000), source.rows.value[id]!!.details.amount)
    }

    @Test
    fun archivedAccountOnAnEditedItemMustBeReplaced() = runTest(dispatcher) {
        val acc = account("Antiga", 0)
        account("Nova", 0)
        val c = categories.create(CategoryDetails("Mercado", EXPENSE))
        val id = UUID.randomUUID()
        source.create(id, TransactionDraft(EXPENSE, acc, c, Money(500), today, null))
        accounts.archive(acc)

        val vm = form(id)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.selectedAccountInactive)
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_account_archived, vm.uiState.value.accountError)
        assertNull(vm.uiState.value.completion)
    }

    @Test
    fun cashShowsRepositoryBalancesAndFiltersHistory() = runTest(dispatcher) {
        val main = account("Principal", 100_000)
        val reserve = account("Reserva", 500_000, include = false)
        val old = account("Antiga", 70_000)
        val food = categories.create(CategoryDetails("Mercado", EXPENSE))
        val salary = categories.create(CategoryDetails("Salário", INCOME))
        source.create(UUID.randomUUID(), TransactionDraft(INCOME, main, salary, Money(300_000), today, "Pagamento"))
        source.create(UUID.randomUUID(), TransactionDraft(EXPENSE, main, food, Money(20_000), today.minusDays(1), "Feira"))
        source.create(UUID.randomUUID(), TransactionDraft(EXPENSE, reserve, food, Money(1_000), today, null))
        source.create(UUID.randomUUID(), TransactionDraft(EXPENSE, main, food, Money(5_000), LocalDate.of(2026, 10, 20), "Outubro"))
        val cancelled = UUID.randomUUID()
        source.create(cancelled, TransactionDraft(EXPENSE, main, food, Money(9_999), today, "Cancelada"))
        source.cancel(cancelled)
        accounts.archive(old)

        val vm = CashViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            // 1.000 + 3.000 − 200 − 50 (outubro) = 3.750; reserva fora; arquivada fora.
            assertEquals(CalculatedBalance.Exact(Money(375_000)), available)
            assertEquals(listOf("Principal", "Reserva"), accounts.map { it.account.details.name })
            assertEquals(CalculatedBalance.Exact(Money(499_000)), accounts.single { it.account.id == reserve }.balance)
            assertEquals(4, entryCount) // novembro, inclui o cancelado
            assertEquals(listOf(today, today.minusDays(1)), days.map { it.date })
        }
        vm.setDirection(DirectionFilter.INCOME)
        advanceUntilIdle()
        assertEquals(listOf("Pagamento"), vm.uiState.value.days.flatMap { it.entries }.map { it.transaction.details.description })
        vm.setDirection(DirectionFilter.ALL)
        vm.setAccount(reserve)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.entryCount)
        vm.setAccount(null)
        vm.setPeriod(HistoryPeriod.LAST_MONTH)
        advanceUntilIdle()
        assertEquals(listOf("Outubro"), vm.uiState.value.days.flatMap { it.entries }.map { it.transaction.details.description })
    }

    @Test
    fun overflowIsSurfacedNotShownAsZero() = runTest(dispatcher) {
        val big = account("Grande", Long.MAX_VALUE - 10)
        val salary = categories.create(CategoryDetails("Salário", INCOME))
        source.create(UUID.randomUUID(), TransactionDraft(INCOME, big, salary, Money(100), today, null))
        val vm = CashViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        assertEquals(CalculatedBalance.Overflow, vm.uiState.value.available)
        assertEquals(CalculatedBalance.Overflow, vm.uiState.value.accounts.single().balance)
    }

    @Test
    fun cashReportsHistoryFailure() = runTest(dispatcher) {
        account("Conta", 0)
        source.failReads = true
        val vm = CashViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        assertEquals(R.string.error_load, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun dashboardShowsAvailableAndOnlyRecentCompletedEntries() = runTest(dispatcher) {
        val acc = account("Conta", 0)
        val salary = categories.create(CategoryDetails("Salário", INCOME))
        repeat(7) { i -> source.create(UUID.randomUUID(), TransactionDraft(INCOME, acc, salary, Money(100), today.minusDays(i.toLong()), "Item $i")) }
        source.create(UUID.randomUUID(), TransactionDraft(INCOME, acc, salary, Money(100), today.minusDays(40), "Antigo"))
        val cancelled = UUID.randomUUID()
        source.create(cancelled, TransactionDraft(INCOME, acc, salary, Money(100), today, "Cancelado"))
        source.cancel(cancelled)

        val vm = DashboardViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CalculatedBalance.Exact(Money(800)), available)
            assertEquals(1, activeAccounts)
            assertEquals(DashboardViewModel.RECENT_LIMIT, recent.size)
            assertTrue(recent.none { it.transaction.cancelledAt != null })
            assertTrue(recent.none { it.transaction.details.description == "Antigo" })
        }
    }
}
