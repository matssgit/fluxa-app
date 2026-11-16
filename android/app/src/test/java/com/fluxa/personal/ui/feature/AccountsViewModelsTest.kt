package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.ui.feature.accounts.AccountFormViewModel
import com.fluxa.personal.ui.feature.accounts.AccountsViewModel
import com.fluxa.personal.ui.feature.accounts.FormCompletion
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
class AccountsViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC)
    private lateinit var dao: FakeAccountDao
    private lateinit var repository: AccountRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeAccountDao()
        repository = AccountRepository(dao, clock)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun form(id: UUID? = null) = AccountFormViewModel(repository, id, clock)

    private fun transactions() = FakeTransactionSource(
        repository,
        com.fluxa.personal.data.repository.CategoryRepository(FakeCategoryDao(), clock),
        LocalDate.of(2026, 11, 15),
        Instant.parse("2026-11-15T12:00:00Z"),
    )

    @Test
    fun createsAccountWithDomainValuesAndCompletes() = runTest(dispatcher) {
        val vm = form()
        assertEquals(LocalDate.of(2026, 11, 15), vm.uiState.value.openingDate)
        vm.onNameChange("  Conta principal ")
        vm.onTypeChange(AccountType.SAVINGS)
        vm.onAmountChange("1.250,90")
        vm.onIncludeInAvailableChange(false)
        vm.save()
        advanceUntilIdle()

        assertEquals(FormCompletion.Saved, vm.uiState.value.completion)
        val saved = repository.observeAll().firstValue().single().details
        assertEquals("Conta principal", saved.name)
        assertEquals(AccountType.SAVINGS, saved.type)
        assertEquals(Money(125_090), saved.openingBalance)
        assertFalse(saved.includeInAvailable)
    }

    @Test
    fun zeroAndNegativeOpeningBalancesAreAccepted() = runTest(dispatcher) {
        val zero = form().apply { onNameChange("Carteira"); onAmountChange("0,00"); save() }
        val negative = form().apply { onNameChange("Cheque especial"); onAmountChange("320,15"); onNegativeChange(true); save() }
        advanceUntilIdle()
        assertEquals(FormCompletion.Saved, zero.uiState.value.completion)
        assertEquals(FormCompletion.Saved, negative.uiState.value.completion)
        val balances = repository.observeAll().firstValue().associate { it.details.name to it.details.openingBalance }
        assertEquals(Money(0), balances["Carteira"])
        assertEquals(Money(-32_015), balances["Cheque especial"])
    }

    @Test
    fun validationErrorsBlockSavingAndKeepTheForm() = runTest(dispatcher) {
        val vm = form()
        vm.onNameChange("   ")
        vm.onAmountChange("12,345")
        vm.save()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(R.string.error_name_required, state.nameError)
        assertEquals(R.string.error_amount_decimals, state.amountError)
        assertNull(state.completion)
        assertEquals(0, dao.inserts)

        vm.onNameChange("Conta")
        assertNull(vm.uiState.value.nameError)
        vm.onAmountChange("")
        vm.save()
        assertEquals(R.string.error_amount_required, vm.uiState.value.amountError)
    }

    @Test
    fun repeatedTapsSaveOnlyOnce() = runTest(dispatcher) {
        val vm = form()
        vm.onNameChange("Conta")
        repeat(5) { vm.save() }
        assertTrue(vm.uiState.value.isSaving)
        advanceUntilIdle()
        vm.save()
        advanceUntilIdle()
        assertEquals(1, dao.inserts)
    }

    @Test
    fun persistenceFailureKeepsInputAndReportsError() = runTest(dispatcher) {
        dao.failWrites = true
        val vm = form()
        vm.onNameChange("Conta")
        vm.onAmountChange("10,00")
        vm.save()
        advanceUntilIdle()
        val state = vm.uiState.value
        assertEquals(R.string.error_save, state.operationError)
        assertFalse(state.isSaving)
        assertNull(state.completion)
        assertEquals("Conta", state.name)
        assertEquals("10,00", state.amountText)

        dao.failWrites = false
        vm.dismissError()
        vm.save()
        advanceUntilIdle()
        assertEquals(FormCompletion.Saved, vm.uiState.value.completion)
    }

    @Test
    fun editLoadsUpdatesAndArchives() = runTest(dispatcher) {
        val id = repository.create(AccountDetails("Banco", AccountType.CHECKING, Money(-1_050), LocalDate.of(2026, 1, 2)))
        val vm = form(id)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals("Banco", name)
            assertEquals("10,50", amountText)
            assertTrue(negative)
            assertEquals(LocalDate.of(2026, 1, 2), openingDate)
        }
        vm.onNameChange("Banco renomeado")
        vm.onNegativeChange(false)
        vm.save()
        advanceUntilIdle()
        val updated = repository.observe(id).firstValue()!!
        assertEquals("Banco renomeado", updated.details.name)
        assertEquals(Money(1_050), updated.details.openingBalance)
        assertEquals(id, updated.id)

        val archiveVm = form(id)
        advanceUntilIdle()
        archiveVm.archive()
        archiveVm.archive()
        advanceUntilIdle()
        assertEquals(FormCompletion.Archived, archiveVm.uiState.value.completion)
        assertNotNull(repository.observe(id).firstValue()!!.archivedAt)
    }

    @Test
    fun archivingAnAlreadyArchivedAccountReportsWithoutCrashing() = runTest(dispatcher) {
        val id = repository.create(AccountDetails("Antiga", AccountType.WALLET, Money(0), LocalDate.of(2026, 1, 1)))
        val vm = form(id)
        advanceUntilIdle()
        repository.archive(id) // arquivada em outro lugar depois de abrir o formulário
        vm.archive()
        advanceUntilIdle()
        assertEquals(R.string.error_already_archived, vm.uiState.value.operationError)
        assertNull(vm.uiState.value.completion)
    }

    @Test
    fun missingAccountShowsNotFound() = runTest(dispatcher) {
        val vm = form(UUID.randomUUID())
        advanceUntilIdle()
        assertTrue(vm.uiState.value.notFound)
        vm.save()
        advanceUntilIdle()
        assertEquals(0, dao.inserts)
    }

    @Test
    fun listSplitsActiveAndArchivedAndFollowsTheFlow() = runTest(dispatcher) {
        val vm = AccountsViewModel(repository, transactions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)

        val a = repository.create(AccountDetails("b conta", AccountType.CHECKING, Money(100), LocalDate.of(2026, 1, 1)))
        repository.create(AccountDetails("A conta", AccountType.WALLET, Money(0), LocalDate.of(2026, 1, 1)))
        advanceUntilIdle()
        assertEquals(listOf("A conta", "b conta"), vm.uiState.value.active.map { it.details.name })

        repository.archive(a)
        advanceUntilIdle()
        assertEquals(listOf("A conta"), vm.uiState.value.active.map { it.details.name })
        assertEquals(listOf("b conta"), vm.uiState.value.archived.map { it.details.name })
        // Saldo atual vem do repositório de lançamentos (inclusive para a arquivada).
        assertEquals(com.fluxa.personal.domain.rules.CalculatedBalance.Exact(Money(100)), vm.uiState.value.balances[a])
        assertFalse(vm.uiState.value.showArchived)
        vm.setShowArchived(true)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showArchived)
    }

    @Test
    fun listReportsLoadFailure() = runTest(dispatcher) {
        dao.failReads = true
        val vm = AccountsViewModel(repository, transactions())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(R.string.error_load, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }
}
