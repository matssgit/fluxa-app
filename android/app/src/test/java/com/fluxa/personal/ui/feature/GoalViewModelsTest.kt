package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountReserveSummary
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalMovementType
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.GoalStatus
import com.fluxa.personal.domain.model.ReserveBalanceSnapshot
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.common.ReserveTotals
import com.fluxa.personal.ui.feature.cash.CashViewModel
import com.fluxa.personal.ui.feature.cash.DashboardViewModel
import com.fluxa.personal.ui.feature.credit.CommandStatus
import com.fluxa.personal.ui.feature.goals.GoalAllocationViewModel
import com.fluxa.personal.ui.feature.goals.GoalCompletion
import com.fluxa.personal.ui.feature.goals.GoalDetailViewModel
import com.fluxa.personal.ui.feature.goals.GoalFormViewModel
import com.fluxa.personal.ui.feature.goals.GoalsViewModel
import com.fluxa.personal.ui.feature.goals.MovementMode
import com.fluxa.personal.ui.feature.goals.percent
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

/**
 * Sprint 6-B: metas são reservas dentro de uma conta. O fake usa `GoalRules` e `ReserveBalanceCalculator` do domínio;
 * os testes provam que a UI exibe exatamente reservado/restante/progresso/disponível do contrato e nunca aplica uma
 * segunda dedução de reservas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-19T12:00:00Z")
    private val today = LocalDate.of(2026, 11, 19)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val calendar = FinancialCalendar(clock) { ZoneOffset.UTC }
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private lateinit var ledger: FakeTransactionSource
    private lateinit var goals: FakeGoalDataSource
    private lateinit var main: UUID
    private lateinit var market: UUID

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        accounts = AccountRepository(FakeAccountDao(), clock)
        categories = CategoryRepository(FakeCategoryDao(), clock)
        ledger = FakeTransactionSource(accounts, categories, today, now)
        goals = FakeGoalDataSource(accounts, ledger, now, today)
        main = accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(500_000), LocalDate.of(2026, 1, 1)))
        market = categories.create(CategoryDetails("Mercado", EXPENSE))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.collect(flow: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    private suspend fun goal(name: String, targetCents: Long, account: UUID = main): UUID =
        (goals.createGoal(GoalDetails(name, Money(targetCents), account)) as GoalResult.Saved).value.id

    private fun exact(cents: Long) = CalculatedBalance.Exact(Money(cents))

    // region Lista e detalhe

    @Test
    fun listSplitsActiveCompletedAndArchived() = runTest(dispatcher) {
        goal("Viagem", 500_000)
        val done = goal("Notebook", 100_000)
        goals.allocateToGoal(done, Money(100_000))
        goals.completeGoal(done)
        val old = goal("Antiga", 10_000)
        goals.archiveGoal(old)
        val vm = GoalsViewModel(goals)
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(listOf("Viagem"), active.map { it.goal.details.name })
            assertEquals(listOf("Notebook"), completed.map { it.goal.details.name })
            assertEquals(listOf("Antiga"), archived.map { it.goal.details.name })
            assertFalse(showArchived)
        }
    }

    @Test
    fun detailShowsContractProgressReservedRemainingAndAccountReserves() = runTest(dispatcher) {
        val id = goal("Viagem", 500_000)
        goals.allocateToGoal(id, Money(200_000))
        val vm = GoalDetailViewModel(goals, id)
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(Money(200_000), summary!!.reservedAmount)
            assertEquals(Money(300_000), summary!!.remainingAmount)
            assertEquals(4_000, summary!!.progressBasisPoints)
            assertEquals(40, summary!!.percent)
            assertEquals("Conta Principal", summary!!.account.details.name)
            assertEquals(exact(500_000), reserves!!.accountBalance) // saldo real não muda ao reservar
            assertEquals(exact(200_000), reserves!!.reservedAmount)
            assertEquals(exact(300_000), reserves!!.availableAmount)
            assertEquals(GoalMovementType.ALLOCATE, movements.single().type)
            assertTrue(canAllocate)
            assertTrue(canRelease)
        }
    }

    // endregion

    // region Reservar

    private fun allocation(id: UUID) = GoalAllocationViewModel(goals, id)

    @Test
    fun allocatingRecordsMovementWithoutTouchingTheLedger() = runTest(dispatcher) {
        val id = goal("Viagem", 500_000)
        val vm = allocation(id)
        vm.start(MovementMode.Allocate)
        vm.onAmountChange("500,00")
        vm.submit()
        vm.submit() // toque duplo
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Success, status)
            assertEquals(Money(50_000), lastAmount)
            assertEquals(MovementMode.Allocate, lastMode)
        }
        assertEquals(1, goals.allocateCalls.size)
        assertTrue(ledger.rows.value.isEmpty()) // reservar não cria Transaction
    }

    @Test
    fun allocationAboveAvailableIsRejectedByContract() = runTest(dispatcher) {
        val small = accounts.create(AccountDetails("Carteira", AccountType.WALLET, Money(100_000), LocalDate.of(2026, 1, 1)))
        val id = goal("Carro", 1_000_000, small)
        val vm = allocation(id)
        vm.start(MovementMode.Allocate)
        vm.onAmountChange("1.500,00")
        vm.submit()
        advanceUntilIdle()
        assertEquals(R.string.error_goal_insufficient_available, vm.uiState.value.amountError)
        assertEquals(Money(0), goals.observeGoal(id).first()!!.reservedAmount)
    }

    @Test
    fun allocationAboveTargetIsRejectedByContract() = runTest(dispatcher) {
        val id = goal("Fone", 30_000)
        val vm = allocation(id)
        vm.start(MovementMode.Allocate)
        vm.onAmountChange("400,00")
        vm.submit()
        advanceUntilIdle()
        assertEquals(R.string.error_goal_exceeds_target, vm.uiState.value.amountError)
        vm.onAmountChange("0")
        vm.submit()
        advanceUntilIdle()
        assertEquals(R.string.error_amount_positive, vm.uiState.value.amountError) // preenchimento, sem chamar o contrato
    }

    // endregion

    // region Liberar

    @Test
    fun releasingPartialTotalAndAboveReserved() = runTest(dispatcher) {
        val id = goal("Viagem", 500_000)
        goals.allocateToGoal(id, Money(100_000))
        val vm = allocation(id)
        vm.start(MovementMode.Release)
        vm.onAmountChange("1.500,00")
        vm.submit()
        advanceUntilIdle()
        assertEquals(R.string.error_goal_release_exceeds, vm.uiState.value.amountError)
        vm.onAmountChange("400,00")
        vm.submit()
        advanceUntilIdle()
        assertEquals(Money(60_000), goals.observeGoal(id).first()!!.reservedAmount) // parcial
        vm.start(MovementMode.Release)
        vm.onAmountChange("600,00")
        vm.submit()
        advanceUntilIdle()
        assertEquals(Money(0), goals.observeGoal(id).first()!!.reservedAmount) // total
        assertEquals(MovementMode.Release, vm.uiState.value.lastMode)
        assertTrue(ledger.rows.value.isEmpty()) // liberar não cria receita
    }

    // endregion

    // region Concluir e arquivar

    @Test
    fun completingRequiresReachedTargetAndKeepsReserve() = runTest(dispatcher) {
        val id = goal("Fone", 30_000)
        val vm = GoalDetailViewModel(goals, id)
        collect(vm.uiState)
        advanceUntilIdle()
        vm.complete()
        advanceUntilIdle()
        assertEquals(R.string.error_goal_target_not_reached, vm.uiState.value.actionError)
        vm.dismissError()
        goals.allocateToGoal(id, Money(30_000))
        vm.complete()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(GoalStatus.COMPLETED, status)
            assertEquals(Money(30_000), summary!!.reservedAmount) // concluir não gasta nem libera
            assertFalse(canAllocate)
            assertTrue(canRelease)
        }
    }

    @Test
    fun archivingIsRejectedWhileReservedAndAllowedAfterRelease() = runTest(dispatcher) {
        val id = goal("Viagem", 500_000)
        goals.allocateToGoal(id, Money(10_000))
        val vm = GoalDetailViewModel(goals, id)
        collect(vm.uiState)
        advanceUntilIdle()
        vm.archive()
        advanceUntilIdle()
        assertEquals(R.string.error_goal_has_reserved, vm.uiState.value.actionError)
        goals.releaseFromGoal(id, Money(10_000))
        vm.dismissError()
        vm.archive()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(GoalStatus.ARCHIVED, status)
            assertEquals(2, movements.size) // histórico preservado
            assertFalse(canEdit)
        }
    }

    // endregion

    // region Formulário

    @Test
    fun formCreatesGoalAndReportsInvalidFields() = runTest(dispatcher) {
        val vm = GoalFormViewModel(goals, accounts, null)
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(R.string.error_name_required, nameError)
            assertEquals(R.string.error_goal_target, targetError)
            assertNull(completion)
        }
        vm.onNameChange("  Viagem ")
        vm.onTargetChange("5.000,00")
        vm.onTargetDateChange(today.plusMonths(6))
        vm.save()
        advanceUntilIdle()
        val id = (vm.uiState.value.completion as GoalCompletion.Created).id
        with(goals.observeGoal(id).first()!!.goal.details) {
            assertEquals("Viagem", name)
            assertEquals(Money(500_000), targetAmount)
            assertEquals(main, accountId) // conta única escolhida automaticamente
            assertEquals(today.plusMonths(6), targetDate)
        }
    }

    @Test
    fun editRejectsTargetBelowReservedAndAccountChangeWithHistory() = runTest(dispatcher) {
        val other = accounts.create(AccountDetails("Poupança", AccountType.SAVINGS, Money(0), LocalDate.of(2026, 1, 1)))
        val id = goal("Viagem", 500_000)
        goals.allocateToGoal(id, Money(200_000))
        val vm = GoalFormViewModel(goals, accounts, id)
        advanceUntilIdle()
        vm.onTargetChange("1.000,00")
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_goal_target_below_reserved, vm.uiState.value.targetError)
        vm.onTargetChange("6.000,00")
        vm.onAccountChange(other)
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_goal_account_change, vm.uiState.value.accountError)
        assertEquals(main, goals.observeGoal(id).first()!!.goal.details.accountId) // nenhuma transferência implícita
    }

    // endregion

    // region Dashboard, Caixa e disponível negativo

    @Test
    fun dashboardShowsExactlyContractTotalsWithoutDoubleDeduction() = runTest(dispatcher) {
        val id = goal("Viagem", 500_000)
        goals.allocateToGoal(id, Money(150_000))
        val vm = DashboardViewModel(ledger, accounts, categories, calendar, reserves = goals.observeReserveBalances())
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(ReserveTotals(exact(500_000), exact(150_000), exact(350_000)), reserveTotals)
            assertEquals(exact(350_000), available) // nunca R$ 2.000 (dupla dedução)
        }
    }

    @Test
    fun dashboardShowsReceivedValuesVerbatimForExcludedAccounts() = runTest(dispatcher) {
        // Valores "sentinela": o contrato já excluiu a conta fora do disponível; a UI não recalcula nada.
        val excluded = UUID.randomUUID()
        val sentinel = ReserveBalanceSnapshot(
            mapOf(excluded to AccountReserveSummary(excluded, false, exact(200_000), exact(50_000), exact(150_000))),
            totalBalance = exact(700_000), totalReserved = exact(80_000), availableBalance = exact(470_000),
        )
        val vm = DashboardViewModel(ledger, accounts, categories, calendar, reserves = MutableStateFlow(sentinel))
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(ReserveTotals(exact(700_000), exact(80_000), exact(470_000)), reserveTotals)
            assertEquals(exact(470_000), available)
        }
    }

    @Test
    fun negativeAvailabilityIsShownAndReservePreserved() = runTest(dispatcher) {
        val id = goal("Reserva", 500_000)
        goals.allocateToGoal(id, Money(400_000))
        ledger.create(UUID.randomUUID(), TransactionDraft(EXPENSE, main, market, Money(200_000), today, "Conserto"))
        val cash = CashViewModel(ledger, accounts, categories, calendar, reserves = goals.observeReserveBalances())
        collect(cash.uiState)
        advanceUntilIdle()
        with(cash.uiState.value) {
            val reserve = accounts.single().reserve!!
            assertEquals(exact(300_000), reserve.accountBalance)
            assertEquals(exact(400_000), reserve.reservedAmount) // a reserva não some
            assertEquals(exact(-100_000), reserve.availableAmount) // negativo, não zerado
            assertEquals(exact(-100_000), available)
        }
        assertEquals(Money(400_000), goals.observeGoal(id).first()!!.reservedAmount)
    }

    // endregion
}
