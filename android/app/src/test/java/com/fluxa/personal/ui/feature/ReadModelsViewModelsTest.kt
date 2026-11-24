package com.fluxa.personal.ui.feature

import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.PeriodSummary
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionHistoryPage
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.feature.cash.CashViewModel
import com.fluxa.personal.ui.feature.cash.DashboardViewModel
import com.fluxa.personal.ui.feature.cash.DirectionFilter
import com.fluxa.personal.ui.feature.cash.HistoryPeriod
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Sprint 3-D: os ViewModels consomem os read models do Codex. Os testes usam valores "sentinela" do fake para
 * provar que a UI repassa o que o repositório entrega, sem somar, filtrar, recortar ou reordenar por conta própria.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReadModelsViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-15T15:00:00Z")
    private val today = LocalDate.of(2026, 11, 15)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val calendar = FinancialCalendar(clock) { ZoneOffset.UTC }
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private lateinit var source: FakeTransactionSource
    private lateinit var main: UUID
    private lateinit var wallet: UUID
    private lateinit var salary: UUID
    private lateinit var market: UUID

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        accounts = AccountRepository(FakeAccountDao(), clock)
        categories = CategoryRepository(FakeCategoryDao(), clock)
        source = FakeTransactionSource(accounts, categories, today, now)
        main = accounts.create(AccountDetails("Principal", AccountType.CHECKING, Money(100_000), LocalDate.of(2026, 1, 1)))
        wallet = accounts.create(AccountDetails("Carteira", AccountType.WALLET, Money(0), LocalDate.of(2026, 1, 1)))
        salary = categories.create(CategoryDetails("Salário", INCOME))
        market = categories.create(CategoryDetails("Mercado", EXPENSE))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.collect(flow: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    private suspend fun add(direction: TransactionDirection, account: UUID, cents: Long, day: LocalDate, description: String): UUID {
        val id = UUID.randomUUID()
        source.create(id, TransactionDraft(direction, account, if (direction == INCOME) salary else market, Money(cents), day, description))
        return id
    }

    private fun exact(cents: Long) = CalculatedBalance.Exact(Money(cents))

    // region Dashboard

    @Test
    fun dashboardAsksForTheCurrentCalendarMonthAndShowsEmptySummaryAsZero() = runTest(dispatcher) {
        val vm = DashboardViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        assertEquals(listOf(LocalDate.of(2026, 11, 1) to LocalDate.of(2026, 12, 1)), source.summaryRequests.distinct())
        with(vm.uiState.value) {
            assertEquals(PeriodSummary(exact(0), exact(0), exact(0)), summary)
            assertNull(errorMessage) // período vazio não é erro
            assertEquals(LocalDate.of(2026, 11, 1), monthStart)
        }
    }

    @Test
    fun dashboardFollowsTheMonthWhenTheDayChanges() = runTest(dispatcher) {
        val days = MutableStateFlow(today)
        val vm = DashboardViewModel(source, accounts, categories, calendar, days = days)
        collect(vm.uiState)
        advanceUntilIdle()
        days.value = LocalDate.of(2026, 12, 1)
        advanceUntilIdle()
        assertEquals(LocalDate.of(2026, 12, 1), vm.uiState.value.monthStart)
        assertEquals(LocalDate.of(2026, 12, 1) to LocalDate.of(2027, 1, 1), source.summaryRequests.last())
    }

    @Test
    fun dashboardShowsIncomeExpensesAndPositiveNetFromTheContract() = runTest(dispatcher) {
        add(INCOME, main, 500_000, today, "Salário")
        add(EXPENSE, wallet, 120_050, today.minusDays(3), "Mercado")
        add(EXPENSE, main, 9_999, LocalDate.of(2026, 10, 31), "Outubro") // fora do mês
        val cancelled = add(EXPENSE, main, 7_777, today, "Cancelado")
        source.cancel(cancelled)

        val vm = DashboardViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        assertEquals(PeriodSummary(exact(500_000), exact(120_050), exact(379_950)), vm.uiState.value.summary)
    }

    @Test
    fun dashboardShowsNegativeNetAndReactsToNewEntries() = runTest(dispatcher) {
        add(INCOME, main, 10_000, today, "Pouco")
        add(EXPENSE, main, 30_000, today, "Muito")
        val vm = DashboardViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        assertEquals(exact(-20_000), vm.uiState.value.summary!!.net)

        add(INCOME, wallet, 50_000, today, "Reforço")
        advanceUntilIdle()
        assertEquals(PeriodSummary(exact(60_000), exact(30_000), exact(30_000)), vm.uiState.value.summary)
    }

    @Test
    fun dashboardPassesTheSummaryThroughWithoutRecomputingIt() = runTest(dispatcher) {
        // Valores impossíveis de obter somando os lançamentos: se a UI somasse, o teste falharia.
        add(INCOME, main, 1, today, "Um centavo")
        val sentinel = PeriodSummary.fromCents(BigInteger.valueOf(123_456), BigInteger.valueOf(654_321))
        source.summaryOverride = sentinel
        val vm = DashboardViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        assertEquals(sentinel, vm.uiState.value.summary)

        source.summaryOverride = PeriodSummary.Overflow
        add(INCOME, main, 1, today, "Dispara o Flow")
        advanceUntilIdle()
        assertEquals(PeriodSummary.Overflow, vm.uiState.value.summary)
    }

    @Test
    fun dashboardRecentComesFromTheRecentContractWithFiveCompletedEntries() = runTest(dispatcher) {
        repeat(7) { i -> add(INCOME, main, 100, today.minusDays(i.toLong()), "Item $i") }
        add(INCOME, main, 100, today.minusDays(200), "Antigo")
        val cancelled = add(INCOME, main, 100, today.plusDays(0), "Cancelado")
        source.cancel(cancelled)

        val vm = DashboardViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        assertEquals(listOf(DashboardViewModel.RECENT_LIMIT), source.recentRequests.distinct())
        val recent = vm.uiState.value.recent.map { it.transaction.details.description }
        assertEquals(listOf("Item 0", "Item 1", "Item 2", "Item 3", "Item 4"), recent)
        assertTrue(source.historyRequests.isEmpty()) // não carrega histórico para recortar
    }

    // endregion

    // region Caixa

    private suspend fun seedCash() {
        add(INCOME, main, 300_000, today, "Pagamento")
        add(EXPENSE, main, 20_000, today.minusDays(1), "Feira")
        add(EXPENSE, wallet, 1_500, today, "Padaria")
        add(INCOME, wallet, 2_000, today.minusDays(2), "Troco")
        add(EXPENSE, main, 5_000, LocalDate.of(2026, 10, 20), "Outubro")
        add(INCOME, main, 7_000, LocalDate.of(2026, 10, 5), "Bônus outubro")
    }

    private fun CashViewModel.descriptions() = uiState.value.days.flatMap { it.entries }.map { it.transaction.details.description }

    @Test
    fun cashSendsEveryFilterToTheRepositoryQuery() = runTest(dispatcher) {
        seedCash()
        val vm = CashViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        with(source.historyRequests.last()) {
            assertNull(first.direction)
            assertNull(first.accountId)
            assertEquals(LocalDate.of(2026, 11, 1), first.fromInclusive)
            assertEquals(LocalDate.of(2026, 12, 1), first.untilExclusive)
            assertTrue(first.includeCancelled)
            assertEquals(TransactionHistoryPage(limit = CashViewModel.PAGE_SIZE + 1), second)
        }
        assertEquals(4, vm.uiState.value.entryCount)

        vm.setDirection(DirectionFilter.INCOME)
        advanceUntilIdle()
        assertEquals(INCOME, source.historyRequests.last().first.direction)
        assertEquals(listOf("Pagamento", "Troco"), vm.descriptions())

        vm.setDirection(DirectionFilter.EXPENSE)
        advanceUntilIdle()
        assertEquals(EXPENSE, source.historyRequests.last().first.direction)
        assertEquals(listOf("Padaria", "Feira"), vm.descriptions())

        vm.setDirection(DirectionFilter.ALL)
        advanceUntilIdle()
        assertNull(source.historyRequests.last().first.direction)
        assertEquals(4, vm.uiState.value.entryCount)
    }

    @Test
    fun cashCombinesAccountDirectionAndPeriod() = runTest(dispatcher) {
        seedCash()
        val vm = CashViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        vm.setAccount(wallet)
        vm.setDirection(DirectionFilter.EXPENSE)
        advanceUntilIdle()
        with(source.historyRequests.last().first) {
            assertEquals(wallet, accountId)
            assertEquals(EXPENSE, direction)
        }
        assertEquals(listOf("Padaria"), vm.descriptions())

        vm.setAccount(null)
        vm.setDirection(DirectionFilter.INCOME)
        vm.setPeriod(HistoryPeriod.LAST_MONTH)
        advanceUntilIdle()
        with(source.historyRequests.last().first) {
            assertNull(accountId)
            assertEquals(INCOME, direction)
            assertEquals(LocalDate.of(2026, 10, 1), fromInclusive)
            assertEquals(LocalDate.of(2026, 11, 1), untilExclusive)
        }
        assertEquals(listOf("Bônus outubro"), vm.descriptions())
    }

    @Test
    fun cashDoesNotFilterOrReorderWhatTheRepositoryReturns() = runTest(dispatcher) {
        seedCash()
        val vm = CashViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        // A "consulta" devolve uma lista mista e fora de ordem, mesmo com filtro de direção: a UI deve exibi-la como veio.
        val mixed = source.rows.value.values.sortedBy { it.details.description }
        source.historyOverride = mixed
        vm.setDirection(DirectionFilter.INCOME)
        advanceUntilIdle()
        val shown = vm.uiState.value.days.flatMap { it.entries }.map { it.transaction.id }.toSet()
        assertEquals(mixed.map { it.id }.toSet(), shown)
        assertEquals(mixed.size, vm.uiState.value.entryCount)
    }

    @Test
    fun loadMoreGrowsTheLimitFromOffsetZeroAndFilterChangesResetIt() = runTest(dispatcher) {
        repeat(CashViewModel.PAGE_SIZE + 5) { i -> add(EXPENSE, main, 100, today.minusDays((i % 10).toLong()), "Gasto $i") }
        val vm = CashViewModel(source, accounts, categories, calendar)
        collect(vm.uiState)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.hasMore)
        assertEquals(CashViewModel.PAGE_SIZE, vm.uiState.value.entryCount)

        vm.loadMore()
        advanceUntilIdle()
        assertEquals(TransactionHistoryPage(limit = 2 * CashViewModel.PAGE_SIZE + 1, offset = 0), source.historyRequests.last().second)
        assertFalse(vm.uiState.value.hasMore)
        assertEquals(CashViewModel.PAGE_SIZE + 5, vm.uiState.value.entryCount)

        vm.loadMore() // sem mais páginas: não altera a consulta
        advanceUntilIdle()
        assertEquals(TransactionHistoryPage(limit = 2 * CashViewModel.PAGE_SIZE + 1), source.historyRequests.last().second)

        vm.setDirection(DirectionFilter.EXPENSE)
        advanceUntilIdle()
        assertEquals(TransactionHistoryPage(limit = CashViewModel.PAGE_SIZE + 1), source.historyRequests.last().second)
    }

    @Test
    fun newEntryUpdatesCashAndDashboardThroughTheirFlows() = runTest(dispatcher) {
        val cash = CashViewModel(source, accounts, categories, calendar)
        val dashboard = DashboardViewModel(source, accounts, categories, calendar)
        collect(cash.uiState)
        collect(dashboard.uiState)
        advanceUntilIdle()
        assertEquals(0, cash.uiState.value.entryCount)

        add(EXPENSE, main, 4_200, today, "Novo")
        advanceUntilIdle()
        assertEquals(listOf("Novo"), cash.descriptions())
        assertEquals(exact(95_800), cash.uiState.value.available)
        assertEquals(exact(4_200), dashboard.uiState.value.summary!!.expenses)
        assertEquals(listOf("Novo"), dashboard.uiState.value.recent.map { it.transaction.details.description })
    }

    // endregion
}
