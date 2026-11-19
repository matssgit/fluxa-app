package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.ProjectedCashEvent
import com.fluxa.personal.domain.model.ProjectedCashEventType.CONFIRMED_INCOME
import com.fluxa.personal.domain.model.ProjectedCashEventType.OBLIGATION
import com.fluxa.personal.domain.model.ProjectedCashSource
import com.fluxa.personal.domain.model.SafeToSpendError
import com.fluxa.personal.domain.model.SafeToSpendResult
import com.fluxa.personal.domain.model.SafeToSpendState
import com.fluxa.personal.domain.model.SubscriptionOccurrenceKey
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.rules.SafeToSpendCalculator
import com.fluxa.personal.ui.feature.safe.CashEventLabels
import com.fluxa.personal.ui.feature.safe.InstallmentLabel
import com.fluxa.personal.ui.feature.safe.SafeSituation
import com.fluxa.personal.ui.feature.safe.SafeToSpendDataSource
import com.fluxa.personal.ui.feature.safe.SafeToSpendUiState
import com.fluxa.personal.ui.feature.safe.SafeToSpendViewModel
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fonte falsa do limite seguro: devolve o estado calculado pelo **`SafeToSpendCalculator` do domínio** (ou um estado
 * sentinela fixo). A UI nunca calcula nada; os testes provam que ela mostra exatamente o que o contrato deu.
 */
@OptIn(ExperimentalCoroutinesApi::class)
private class FakeSafeToSpendSource(var today: LocalDate) : SafeToSpendDataSource {
    val available = MutableStateFlow(Money(0))
    val events = MutableStateFlow<List<ProjectedCashEvent>>(emptyList())
    /** Quando definido, ignora o calculador e devolve este estado verbatim. */
    val override = MutableStateFlow<SafeToSpendState?>(null)
    var subscriptions = 0
    var lastHorizon = 0

    override fun observeSafeToSpend(horizonDays: Int): Flow<SafeToSpendState> {
        lastHorizon = horizonDays
        return override.flatMapLatest { fixed ->
            if (fixed != null) MutableStateFlow(fixed)
            else available.flatMapLatest { a -> events.map { e -> SafeToSpendCalculator.calculate(a, e, today, horizonDays) } }
        }.onStart { subscriptions++ }
    }
}

private class FakeLabels : CashEventLabels {
    val subscriptions = MutableStateFlow<Map<UUID, String>>(emptyMap())
    val installments = MutableStateFlow<Map<UUID, InstallmentLabel>>(emptyMap())
    override fun subscriptionName(id: UUID, occurrenceDate: LocalDate) = subscriptions.map { it[id] }
    override fun installmentLabel(source: ProjectedCashSource.Installment) = installments.map { it[source.installmentId] }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SafeToSpendViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.of(2026, 11, 19)
    private lateinit var source: FakeSafeToSpendSource
    private lateinit var labels: FakeLabels
    private lateinit var days: MutableStateFlow<LocalDate>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        source = FakeSafeToSpendSource(today)
        labels = FakeLabels()
        days = MutableStateFlow(today)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.vm(): SafeToSpendViewModel {
        val vm = SafeToSpendViewModel(source, labels, days)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        return vm
    }

    private fun SafeToSpendViewModel.content() = uiState.value as SafeToSpendUiState.Content

    private fun reais(r: Long) = Money(r * 100)

    private fun subscription(date: LocalDate, cents: Long, id: UUID = UUID.randomUUID(), occurrence: LocalDate = date, card: Boolean = false) =
        ProjectedCashEvent(
            date, OBLIGATION, Money(cents),
            ProjectedCashSource.Subscription(
                SubscriptionOccurrenceKey(id, occurrence),
                if (card) SubscriptionSource.Card(UUID.randomUUID()) else SubscriptionSource.Account(UUID.randomUUID()),
            ),
        )

    private fun installment(date: LocalDate, cents: Long, id: UUID = UUID.randomUUID()) =
        ProjectedCashEvent(date, OBLIGATION, Money(cents), ProjectedCashSource.Installment(id, UUID.randomUUID(), UUID.randomUUID()))

    private fun income(date: LocalDate, cents: Long) =
        ProjectedCashEvent(date, CONFIRMED_INCOME, Money(cents), ProjectedCashSource.ConfirmedIncome(UUID.randomUUID()))

    // region Situações

    @Test
    fun startsLoadingAndUsesThirtyDayHorizon() = runTest(dispatcher) {
        val vm = SafeToSpendViewModel(source, labels, days)
        assertEquals(SafeToSpendUiState.Loading, vm.uiState.value)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(30, source.lastHorizon)
        assertEquals(30, vm.content().horizonDays)
    }

    @Test
    fun safeBelowAvailableIsReducedAndCommittedIsOnlyTheDifferenceForDisplay() = runTest(dispatcher) {
        source.available.value = reais(2_000)
        source.events.value = listOf(subscription(today.plusDays(3), 50_000))
        val vm = vm(); advanceUntilIdle()
        val c = vm.content()
        assertEquals(reais(1_500), c.safeAmount)
        assertEquals(reais(2_000), c.availableNow)
        assertEquals(SafeSituation.Reduced, c.situation)
        assertEquals(reais(500), c.committedForDisplay)
    }

    @Test
    fun safeEqualToAvailableMeansNoObligations() = runTest(dispatcher) {
        source.available.value = reais(2_000)
        val vm = vm(); advanceUntilIdle()
        val c = vm.content()
        assertEquals(reais(2_000), c.safeAmount)
        assertEquals(SafeSituation.NoObligations, c.situation)
        assertNull(c.committedForDisplay)
        assertTrue(c.breakdown.isEmpty())
        assertTrue(c.lowestSlackIsToday)
    }

    @Test
    fun obligationsUsingEverythingGiveZero() = runTest(dispatcher) {
        source.available.value = reais(1_000)
        source.events.value = listOf(installment(today.plusDays(2), 70_000), subscription(today.plusDays(4), 50_000))
        val vm = vm(); advanceUntilIdle()
        val c = vm.content()
        assertEquals(Money(0), c.safeAmount)
        assertEquals(SafeSituation.ZeroByObligations, c.situation)
        assertEquals(reais(1_000), c.committedForDisplay)
        assertEquals(reais(-200), c.lowestProjectedBalance)
        assertEquals(today.plusDays(4), c.lowestSlackDate)
    }

    @Test
    fun negativeAvailableShowsZeroAndNegativeSituationWithoutCommittedLine() = runTest(dispatcher) {
        source.available.value = reais(-400)
        val vm = vm(); advanceUntilIdle()
        val c = vm.content()
        assertEquals(Money(0), c.safeAmount)
        assertEquals(reais(-400), c.availableNow)
        assertEquals(SafeSituation.NegativeAvailable, c.situation)
        assertNull(c.committedForDisplay)
    }

    @Test
    fun unavailableStatesBecomeClearMessagesNotGenericErrors() = runTest(dispatcher) {
        source.override.value = SafeToSpendState.Unavailable(SafeToSpendError.OVERFLOW)
        val vm = vm(); advanceUntilIdle()
        assertEquals(SafeToSpendUiState.Unavailable(R.string.safe_unavailable_overflow), vm.uiState.value)
        source.override.value = SafeToSpendState.Unavailable(SafeToSpendError.CONFLICTING_EVENTS)
        advanceUntilIdle()
        assertEquals(SafeToSpendUiState.Unavailable(R.string.safe_unavailable_conflict), vm.uiState.value)
    }

    // endregion

    // region Contrato verbatim (testes críticos)

    /** Real 5.000, reservado 1.500 → disponível 3.500 (já líquido). O limite é 3.500, **nunca** 2.000. */
    @Test
    fun reservesAreNeverDeductedTwice() = runTest(dispatcher) {
        source.available.value = reais(3_500) // availableBalance do contrato, já sem as reservas
        val vm = vm(); advanceUntilIdle()
        val c = vm.content()
        assertEquals(reais(3_500), c.safeAmount)
        assertEquals(reais(3_500), c.availableNow)
        assertNotEquals(reais(2_000), c.safeAmount)
        assertNull(c.committedForDisplay)
    }

    /**
     * Disponível 2.000; amanhã −500; +5d −900; +10d +1.000 (receita confirmada). O backend dá 600 com menor folga em +5d.
     * A UI mostra 600 e **não** "recupera" para 1.600 com a receita posterior; o breakdown para na menor folga.
     */
    @Test
    fun temporalOrderIsRespectedAndLaterIncomeIsNotUsed() = runTest(dispatcher) {
        source.available.value = reais(2_000)
        source.events.value = listOf(
            installment(today.plusDays(1), 50_000),
            subscription(today.plusDays(5), 90_000),
            income(today.plusDays(10), 100_000),
        )
        val vm = vm(); advanceUntilIdle()
        val c = vm.content()
        assertEquals(reais(600), c.safeAmount)
        assertNotEquals(reais(1_600), c.safeAmount)
        assertEquals(today.plusDays(5), c.lowestSlackDate)
        assertEquals(reais(600), c.lowestProjectedBalance)
        assertEquals(listOf(today.plusDays(1), today.plusDays(5)), c.breakdown.map { it.event.date })
        assertFalse(c.breakdown.any { it.isIncome })
        assertEquals(reais(1_400), c.committedForDisplay)
    }

    /** Sentinela: nenhum número é recalculado; tudo vem do resultado como está (até valores "impossíveis"). */
    @Test
    fun resultIsDisplayedVerbatim() = runTest(dispatcher) {
        val e1 = subscription(today.plusDays(2), 12_345)
        source.override.value = SafeToSpendState.Ready(
            SafeToSpendResult(
                safeAmount = Money(77_777), availableNow = Money(99_999), safetyMargin = Money(0),
                lowestProjectedBalance = Money(11_111), lowestSlackDate = today.plusDays(9),
                horizonStart = today, horizonEndExclusive = today.plusDays(30),
                obligationsTotal = Money(1), confirmedIncomeTotal = Money(2), breakdown = listOf(e1),
            ),
        )
        val vm = vm(); advanceUntilIdle()
        val c = vm.content()
        assertEquals(Money(77_777), c.safeAmount)
        assertEquals(Money(99_999), c.availableNow)
        assertEquals(Money(11_111), c.lowestProjectedBalance)
        assertEquals(today.plusDays(9), c.lowestSlackDate)
        assertEquals(listOf(e1), c.breakdown.map { it.event })
        assertEquals(Money(99_999 - 77_777), c.committedForDisplay)
    }

    /** Eventos do mesmo dia mantêm exatamente a ordem do backend (a UI não reordena). */
    @Test
    fun sameDayOrderIsNotReordered() = runTest(dispatcher) {
        val b = subscription(today.plusDays(3), 20_000)
        val a = installment(today.plusDays(3), 10_000)
        source.override.value = SafeToSpendState.Ready(
            SafeToSpendResult(
                safeAmount = Money(0), availableNow = Money(30_000), safetyMargin = Money(0),
                lowestProjectedBalance = Money(0), lowestSlackDate = today.plusDays(3),
                horizonStart = today, horizonEndExclusive = today.plusDays(30),
                obligationsTotal = Money(30_000), confirmedIncomeTotal = Money(0), breakdown = listOf(b, a),
            ),
        )
        val vm = vm(); advanceUntilIdle()
        assertEquals(listOf(b, a), vm.content().breakdown.map { it.event })
    }

    // endregion

    // region Breakdown e rótulos

    @Test
    fun breakdownGetsNamesByStructuralIdsAndKeepsAmounts() = runTest(dispatcher) {
        val netflix = UUID.randomUUID()
        val parcela = UUID.randomUUID()
        labels.subscriptions.value = mapOf(netflix to "Netflix")
        labels.installments.value = mapOf(parcela to InstallmentLabel("Notebook", 3, 10, today.plusDays(1)))
        source.available.value = reais(5_000)
        source.events.value = listOf(installment(today.plusDays(1), 40_000, parcela), subscription(today.plusDays(2), 5_590, netflix, card = true))
        val vm = vm(); advanceUntilIdle()
        val items = vm.content().breakdown
        assertEquals(listOf("Notebook", "Netflix"), items.map { it.title })
        assertEquals(3, items[0].installmentNumber)
        assertEquals(10, items[0].installmentCount)
        assertEquals(listOf(Money(40_000), Money(5_590)), items.map { it.event.amount })
        assertFalse(items.any { it.isOverdue })
    }

    @Test
    fun missingLabelsStillShowTheEvent() = runTest(dispatcher) {
        source.available.value = reais(1_000)
        source.events.value = listOf(subscription(today.plusDays(1), 10_000))
        val vm = vm(); advanceUntilIdle()
        val item = vm.content().breakdown.single()
        assertNull(item.title)
        assertEquals(Money(10_000), item.event.amount)
    }

    @Test
    fun overdueObligationCountsTodayAndKeepsOriginalDate() = runTest(dispatcher) {
        val id = UUID.randomUUID()
        labels.subscriptions.value = mapOf(id to "Academia")
        source.available.value = reais(1_000)
        source.events.value = listOf(subscription(today.minusDays(3), 12_000, id))
        val vm = vm(); advanceUntilIdle()
        val c = vm.content()
        val item = c.breakdown.single()
        assertEquals(today, item.event.date)
        assertEquals(today.minusDays(3), item.originalDate)
        assertTrue(item.isOverdue)
        assertEquals(reais(880), c.safeAmount)
        assertTrue(c.lowestSlackIsToday)
    }

    @Test
    fun renamingASubscriptionUpdatesTheLabelWithoutTouchingAmounts() = runTest(dispatcher) {
        val id = UUID.randomUUID()
        labels.subscriptions.value = mapOf(id to "Spotify")
        source.available.value = reais(1_000)
        source.events.value = listOf(subscription(today.plusDays(1), 2_190, id))
        val vm = vm(); advanceUntilIdle()
        val before = vm.content().safeAmount
        labels.subscriptions.value = mapOf(id to "Spotify Família")
        advanceUntilIdle()
        assertEquals("Spotify Família", vm.content().breakdown.single().title)
        assertEquals(before, vm.content().safeAmount)
    }

    /** Títulos vêm só dos nomes cadastrados: nenhum valor monetário entra em texto auxiliar. */
    @Test
    fun labelsNeverCarryAmounts() = runTest(dispatcher) {
        val id = UUID.randomUUID()
        labels.subscriptions.value = mapOf(id to "Netflix")
        source.available.value = reais(1_000)
        source.events.value = listOf(subscription(today.plusDays(1), 5_590, id))
        val vm = vm(); advanceUntilIdle()
        val title = vm.content().breakdown.single().title!!
        assertFalse(title.contains("R$"))
        assertFalse(title.contains("55,90"))
    }

    // endregion

    // region Atualização automática e virada do dia

    @Test
    fun newObligationAndPaymentUpdateAutomatically() = runTest(dispatcher) {
        source.available.value = reais(2_000)
        val vm = vm(); advanceUntilIdle()
        assertEquals(reais(2_000), vm.content().safeAmount)
        val ob = subscription(today.plusDays(5), 30_000)
        source.events.value = listOf(ob)
        advanceUntilIdle()
        assertEquals(reais(1_700), vm.content().safeAmount)
        // Pagamento: a obrigação sai da projeção e o disponível cai pelo lançamento real.
        source.events.value = emptyList()
        source.available.value = reais(1_700)
        advanceUntilIdle()
        assertEquals(reais(1_700), vm.content().safeAmount)
        assertEquals(SafeSituation.NoObligations, vm.content().situation)
    }

    @Test
    fun dayChangeResubscribesAndMovesToday() = runTest(dispatcher) {
        source.available.value = reais(1_000)
        source.events.value = listOf(subscription(today.plusDays(1), 10_000))
        val vm = vm(); advanceUntilIdle()
        assertEquals(1, source.subscriptions)
        assertFalse(vm.content().lowestSlackIsToday)
        source.today = today.plusDays(1)
        days.value = today.plusDays(1)
        advanceUntilIdle()
        assertEquals(2, source.subscriptions)
        val c = vm.content()
        assertEquals(today.plusDays(1), c.today)
        assertEquals(today.plusDays(1), c.horizonStart)
        assertTrue(c.lowestSlackIsToday)
    }

    // endregion
}
