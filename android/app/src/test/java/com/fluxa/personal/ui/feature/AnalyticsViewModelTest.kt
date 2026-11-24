package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AnalyticsCategoryTotal
import com.fluxa.personal.domain.model.AnalyticsLedgerTotal
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CategorySpending
import com.fluxa.personal.domain.model.FinancialAnalyticsError
import com.fluxa.personal.domain.model.FinancialAnalyticsSnapshot
import com.fluxa.personal.domain.model.FinancialAnalyticsState
import com.fluxa.personal.domain.model.FinancialInsight
import com.fluxa.personal.domain.model.FinancialInsightType
import com.fluxa.personal.domain.model.FinancialProjection
import com.fluxa.personal.domain.model.SubscriptionAnalytics
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.domain.rules.FinancialAnalyticsCalculator
import com.fluxa.personal.ui.feature.analytics.AnalyticsDataSource
import com.fluxa.personal.ui.feature.analytics.AnalyticsUiState
import com.fluxa.personal.ui.feature.analytics.AnalyticsViewModel
import com.fluxa.personal.ui.feature.analytics.PercentFormat
import com.fluxa.personal.ui.feature.analytics.visibleInsights
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fonte falsa: monta o snapshot com o **calculador do domínio**. Os testes provam que a UI repassa o contrato sem recalcular. */
@OptIn(ExperimentalCoroutinesApi::class)
private class FakeAnalytics(var today: LocalDate) : AnalyticsDataSource {
    val ledger = MutableStateFlow<List<AnalyticsLedgerTotal>>(emptyList())
    val spending = MutableStateFlow<Map<YearMonth, List<AnalyticsCategoryTotal>>>(emptyMap())
    val categories = MutableStateFlow<List<Category>>(emptyList())
    var projection: FinancialProjection? = null
    var subscriptions = SubscriptionAnalytics(0, Money(0), Money(0))
    val fixed = MutableStateFlow<FinancialAnalyticsState?>(null)
    val requested = mutableListOf<YearMonth?>()

    override fun observeCategories(): Flow<List<Category>> = categories

    override fun observe(month: YearMonth?): Flow<FinancialAnalyticsState> =
        combine(ledger, spending, fixed) { rows, byMonth, override ->
            override ?: run {
                val m = month ?: YearMonth.from(today)
                val summary = FinancialAnalyticsCalculator.summary(m, rows)
                val previous = FinancialAnalyticsCalculator.summary(m.minusMonths(1), rows)
                val cats = FinancialAnalyticsCalculator.categories(byMonth[m].orEmpty())
                FinancialAnalyticsState.Ready(
                    FinancialAnalyticsSnapshot(
                        today, summary, cats, FinancialAnalyticsCalculator.evolution(m, rows),
                        projection ?: empty(30), empty(7), subscriptions,
                        FinancialAnalyticsCalculator.insights(summary, previous, cats, today),
                    ),
                )
            }
        }.onStart { requested += month }

    fun empty(days: Long) = FinancialProjection(
        Money(0), Money(0), Money(0), Money(0), Money(0), today, today, today.plusDays(days),
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 11, 24)
    private val november = YearMonth.of(2026, 11)
    private val october = november.minusMonths(1)
    private lateinit var source: FakeAnalytics
    private lateinit var days: MutableStateFlow<LocalDate>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        source = FakeAnalytics(today)
        days = MutableStateFlow(today)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm(): AnalyticsViewModel {
        val vm = AnalyticsViewModel(source, days)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        return vm
    }

    private fun AnalyticsViewModel.content() = uiState.value as AnalyticsUiState.Content
    private fun income(month: YearMonth, cents: Long, count: Long = 1) = AnalyticsLedgerTotal(month, INCOME, Money(cents), count)
    private fun expense(month: YearMonth, cents: Long, count: Long = 1) = AnalyticsLedgerTotal(month, EXPENSE, Money(cents), count)
    private fun category(name: String, cents: Long, id: UUID = UUID.randomUUID()) = AnalyticsCategoryTotal(id, name, Money(cents))

    @Test
    fun startsLoadingThenShowsTheCurrentMonthFromTheContract() = runTest(dispatcher) {
        source.ledger.value = listOf(income(november, 620_000), expense(november, 410_000, 3))
        val cold = AnalyticsViewModel(source, days)
        assertEquals(AnalyticsUiState.Loading, cold.uiState.value)
        val c = vm().content()
        assertEquals(listOf<YearMonth?>(null), source.requested) // atual = null, quem decide o mês é o backend
        assertEquals(november, c.month)
        assertTrue(c.isCurrentMonth)
        assertEquals(Money(620_000), c.summary.incomeRealized)
        assertEquals(Money(410_000), c.summary.expenseRealized)
        assertEquals(Money(210_000), c.summary.netResult)
        assertEquals(4L, c.summary.transactionCount)
        assertEquals("33,9%", PercentFormat.ratio(c.summary.savingsRate!!))
        assertEquals(6, c.evolution.size)
        assertEquals(november, c.evolution.last().month)
        assertEquals(Money(620_000), c.evolutionPeak)
    }

    @Test
    fun previousAndNextNavigateAndNeverPassTheCurrentMonth() = runTest(dispatcher) {
        source.ledger.value = listOf(income(november, 100), expense(october, 5_000))
        val vm = vm()
        vm.nextMonth(); advanceUntilIdle()
        assertEquals(november, vm.content().month)
        vm.previousMonth(); advanceUntilIdle()
        assertEquals(october, vm.content().month)
        assertFalse(vm.content().isCurrentMonth)
        assertEquals(Money(5_000), vm.content().summary.expenseRealized)
        vm.previousMonth(); advanceUntilIdle()
        assertEquals(october.minusMonths(1), vm.content().month)
        assertFalse(vm.content().hasMovements)
        assertFalse(vm.content().isEmpty) // mês passado vazio segue navegável
        vm.nextMonth(); advanceUntilIdle(); vm.nextMonth(); advanceUntilIdle()
        assertTrue(vm.content().isCurrentMonth)
        assertNull(source.requested.last()) // voltou a acompanhar o mês atual
    }

    @Test
    fun negativeResultKeepsItsSign() = runTest(dispatcher) {
        source.ledger.value = listOf(income(november, 100_000), expense(november, 150_000))
        val c = vm().content()
        assertEquals(Money(-50_000), c.summary.netResult)
        assertEquals("−50%", PercentFormat.ratio(c.summary.savingsRate!!))
        assertEquals(FinancialInsightType.NEGATIVE_MONTH, c.insights.first().type)
    }

    @Test
    fun withoutIncomeThereIsNoSavingsRate() = runTest(dispatcher) {
        source.ledger.value = listOf(expense(november, 10_000))
        val c = vm().content()
        assertNull(c.summary.savingsRate)
        assertTrue(c.hasMovements)
    }

    @Test
    fun emptyDatabaseIsAnInvitationAndPartialDataIsContent() = runTest(dispatcher) {
        val vm = vm()
        assertTrue(vm.content().isEmpty)
        source.ledger.value = listOf(income(november, 1_000)); advanceUntilIdle()
        val c = vm.content()
        assertFalse(c.isEmpty)
        assertTrue(c.hasMovements)
        assertFalse(c.hasExpenses)
        assertTrue(c.hasEvolution)
        assertEquals(setOf(FinancialInsightType.POSITIVE_SAVINGS, FinancialInsightType.NO_EXPENSES), c.insights.map { it.type }.toSet())
    }

    @Test
    fun balanceOrSubscriptionsAloneAreNotEmpty() = runTest(dispatcher) {
        source.projection = source.empty(30).copy(availableNow = Money(1))
        assertFalse(vm().content().isEmpty)
        source.projection = null
        source.subscriptions = SubscriptionAnalytics(1, Money(100), Money(1_200))
        assertFalse(vm().content().isEmpty)
    }

    @Test
    fun categoriesAreTheTopFiveWithCatalogLookAndOriginalShares() = runTest(dispatcher) {
        val food = UUID.randomUUID()
        val totals = listOf(category("Alimentação", 30_000, food), category("Casa", 25_000), category("Lazer", 20_000), category("Saúde", 12_000), category("Pet", 8_000), category("Outros gastos", 5_000))
        source.ledger.value = listOf(expense(november, 100_000))
        source.spending.value = mapOf(november to totals)
        source.categories.value = listOf(Category(food, CategoryDetails("Nome do cadastro", EXPENSE, "food", "#B9684F"), null))
        val c = vm().content()
        assertEquals(listOf("Alimentação", "Casa", "Lazer", "Saúde", "Pet"), c.categories.map { it.spending.categoryName })
        assertEquals(listOf(30, 25, 20, 12, 8), c.categories.map { it.spending.percentage }) // não renormaliza o top 5
        assertEquals(1, c.otherCategories)
        assertEquals("food", c.categories.first().icon)
        assertEquals("#B9684F", c.categories.first().color)
        assertNull(c.categories[1].color) // categoria sem cadastro visível não quebra
        assertEquals(food, c.insights.first { it.type == FinancialInsightType.TOP_CATEGORY }.categoryId)
    }

    @Test
    fun projectionAndSubscriptionsArePassedVerbatim() = runTest(dispatcher) {
        // Sentinelas que não fecham entre si: qualquer conta na UI mudaria algum valor.
        val p = FinancialProjection(Money(111), Money(222), Money(333), Money(444), Money(-555), today.plusDays(9), today, today.plusDays(30))
        source.projection = p
        source.subscriptions = SubscriptionAnalytics(3, Money(1_234), Money(99_999))
        val c = vm().content()
        assertEquals(p, c.projection)
        assertEquals(SubscriptionAnalytics(3, Money(1_234), Money(99_999)), c.subscriptions)
        assertEquals(today.plusDays(7), c.remainingMonth.horizonEndExclusive)
    }

    @Test
    fun closedMonthComparisonAppearsOnlyForPastMonths() = runTest(dispatcher) {
        source.ledger.value = listOf(expense(october.minusMonths(1), 10_000), expense(october, 11_800), expense(november, 50_000), income(november, 1))
        val vm = vm()
        assertFalse(vm.content().insights.any { it.type == FinancialInsightType.EXPENSES_INCREASED })
        vm.previousMonth(); advanceUntilIdle()
        val fact = vm.content().insights.first { it.type == FinancialInsightType.EXPENSES_INCREASED }
        assertEquals("+18%", PercentFormat.ratio(fact.changeRate!!, signed = true))
        assertEquals(october.minusMonths(1), fact.comparisonPeriod)
    }

    @Test
    fun contractUpdatesArriveWithoutRefresh() = runTest(dispatcher) {
        val vm = vm()
        source.ledger.value = listOf(expense(november, 900)); advanceUntilIdle()
        assertEquals(Money(900), vm.content().summary.expenseRealized)
        source.ledger.value = emptyList(); advanceUntilIdle() // desfazer
        assertEquals(Money(0), vm.content().summary.expenseRealized)
    }

    @Test
    fun midnightMovesTheCurrentMonthAndKeepsAChosenPastMonth() = runTest(dispatcher) {
        val december = november.plusMonths(1)
        source.ledger.value = listOf(expense(november, 900), income(december, 70))
        val vm = vm()
        source.today = LocalDate.of(2026, 12, 1); days.value = source.today; advanceUntilIdle()
        assertEquals(december, vm.content().month)
        assertTrue(vm.content().isCurrentMonth)
        vm.previousMonth(); advanceUntilIdle()
        assertEquals(november, vm.content().month)
        source.today = LocalDate.of(2026, 12, 2); days.value = source.today; advanceUntilIdle()
        assertEquals(november, vm.content().month)
        // Relógio voltou para novembro: o mês escolhido é o atual de novo, nunca um mês futuro.
        source.today = today; days.value = today; advanceUntilIdle()
        assertTrue(vm.content().isCurrentMonth)
    }

    @Test
    fun hiddenAmountsDropInsightsThatRevealTheResult() {
        val all = FinancialInsightType.entries.map { FinancialInsight(it) }
        assertEquals(all, visibleInsights(all, hidden = false))
        assertEquals(
            listOf(FinancialInsightType.TOP_CATEGORY, FinancialInsightType.NO_EXPENSES),
            visibleInsights(all, hidden = true).map { it.type },
        )
    }

    @Test
    fun unavailableAndFailuresBecomeFriendlyErrors() = runTest(dispatcher) {
        source.fixed.value = FinancialAnalyticsState.Unavailable(FinancialAnalyticsError.OVERFLOW)
        val vm = vm()
        assertEquals(AnalyticsUiState.Error(R.string.analytics_unavailable_overflow), vm.uiState.value)
        source.fixed.value = FinancialAnalyticsState.Unavailable(FinancialAnalyticsError.CONFLICTING_EVENTS); advanceUntilIdle()
        assertEquals(AnalyticsUiState.Error(R.string.analytics_unavailable_conflict), vm.uiState.value)

        val broken = object : AnalyticsDataSource {
            override fun observe(month: YearMonth?): Flow<FinancialAnalyticsState> = flow { error("falha") }
            override fun observeCategories(): Flow<List<Category>> = source.categories.map { it }
        }
        val failing = AnalyticsViewModel(broken, days)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { failing.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(AnalyticsUiState.Error(R.string.error_load), failing.uiState.value)
    }

    @Test
    fun percentagesNeverTurnASmallRealValueIntoZero() {
        assertEquals("0%", PercentFormat.ratio(BigDecimal.ZERO))
        assertEquals("25%", PercentFormat.ratio(BigDecimal("0.25")))
        assertEquals("<0,1%", PercentFormat.ratio(BigDecimal("0.0000001")))
        assertEquals(">−0,1%", PercentFormat.ratio(BigDecimal("-0.0000001")))
        assertEquals("+100%", PercentFormat.ratio(BigDecimal.ONE, signed = true))
        assertEquals("<1%", PercentFormat.share(CategorySpending(UUID.randomUUID(), "x", Money(1), 0)))
        assertEquals("32%", PercentFormat.share(CategorySpending(UUID.randomUUID(), "x", Money(1), 32)))
    }
}
