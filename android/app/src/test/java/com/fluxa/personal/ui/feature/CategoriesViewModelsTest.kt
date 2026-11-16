package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.ui.feature.accounts.FormCompletion
import com.fluxa.personal.ui.feature.categories.CategoriesViewModel
import com.fluxa.personal.ui.feature.categories.CategoryFormViewModel
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeCategoryDao
    private lateinit var repository: CategoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeCategoryDao()
        repository = CategoryRepository(dao, Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun createsIncomeAndExpenseCategoriesWithOptionalVisuals() = runTest(dispatcher) {
        val income = CategoryFormViewModel(repository, null, INCOME)
        income.onNameChange("Salário")
        income.onIconChange("work")
        income.onColorChange("#35695A")
        income.save()
        val expense = CategoryFormViewModel(repository, null)
        expense.onNameChange("Mercado")
        expense.save()
        advanceUntilIdle()

        assertEquals(FormCompletion.Saved, income.uiState.value.completion)
        assertEquals(FormCompletion.Saved, expense.uiState.value.completion)
        val incomeSaved = repository.observeAll(kind = INCOME).firstValue().single().details
        assertEquals(CategoryDetails("Salário", INCOME, "work", "#35695A"), incomeSaved)
        val expenseSaved = repository.observeAll(kind = EXPENSE).firstValue().single().details
        assertEquals(CategoryDetails("Mercado", EXPENSE, null, null), expenseSaved)
    }

    @Test
    fun duplicateNamesAreAllowedAsInTheContract() = runTest(dispatcher) {
        repeat(2) {
            CategoryFormViewModel(repository, null).apply { onNameChange("Lazer"); save() }
        }
        advanceUntilIdle()
        assertEquals(2, repository.observeAll().firstValue().count { it.details.name == "Lazer" })
    }

    @Test
    fun blankNameIsRejectedByTheDomainAndRepeatedTapsSaveOnce() = runTest(dispatcher) {
        val vm = CategoryFormViewModel(repository, null)
        vm.save()
        assertEquals(R.string.error_name_required, vm.uiState.value.nameError)
        vm.onNameChange("Casa")
        repeat(3) { vm.save() }
        advanceUntilIdle()
        assertEquals(1, dao.inserts)
    }

    @Test
    fun editChangesKindAndVisualsAndKeepsId() = runTest(dispatcher) {
        val id = repository.create(CategoryDetails("Freela", EXPENSE))
        val vm = CategoryFormViewModel(repository, id)
        advanceUntilIdle()
        assertEquals("Freela", vm.uiState.value.name)
        vm.onKindChange(INCOME)
        vm.onIconChange("work")
        vm.save()
        advanceUntilIdle()
        val saved = repository.observe(id).firstValue()!!
        assertEquals(id, saved.id)
        assertEquals(INCOME, saved.details.kind)
        assertEquals("work", saved.details.icon)
    }

    @Test
    fun archiveFromFormThenReactivateFromForm() = runTest(dispatcher) {
        val id = repository.create(CategoryDetails("Viagem", EXPENSE))
        val vm = CategoryFormViewModel(repository, id)
        advanceUntilIdle()
        vm.archive()
        advanceUntilIdle()
        assertEquals(FormCompletion.Archived, vm.uiState.value.completion)
        assertTrue(repository.observe(id).firstValue()!!.archivedAt != null)

        val reopened = CategoryFormViewModel(repository, id)
        advanceUntilIdle()
        assertTrue(reopened.uiState.value.isArchived)
        reopened.reactivate()
        advanceUntilIdle()
        assertFalse(reopened.uiState.value.isArchived)
        assertNull(reopened.uiState.value.completion)
        assertNull(repository.observe(id).firstValue()!!.archivedAt)
    }

    @Test
    fun listFiltersByKindAndReactivatesOnce() = runTest(dispatcher) {
        val archived = repository.create(CategoryDetails("Bônus", INCOME))
        repository.create(CategoryDetails("Salário", INCOME))
        repository.create(CategoryDetails("Mercado", EXPENSE))
        repository.archive(archived)

        val vm = CategoriesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        advanceUntilIdle()
        assertEquals(EXPENSE, vm.uiState.value.kind)
        assertEquals(listOf("Mercado"), vm.uiState.value.active.map { it.details.name })

        vm.selectKind(INCOME)
        advanceUntilIdle()
        assertEquals(listOf("Salário"), vm.uiState.value.active.map { it.details.name })
        assertEquals(listOf("Bônus"), vm.uiState.value.archived.map { it.details.name })

        vm.reactivate(archived)
        vm.reactivate(archived)
        advanceUntilIdle()
        assertEquals(1, dao.reactivations)
        assertEquals(listOf("Bônus", "Salário"), vm.uiState.value.active.map { it.details.name })
        assertTrue(vm.uiState.value.archived.isEmpty())
        assertTrue(vm.uiState.value.reactivating.isEmpty())
    }

    @Test
    fun kindChangeOfAReferencedCategoryShowsItsOwnMessage() = runTest(dispatcher) {
        val id = repository.create(CategoryDetails("Freela", EXPENSE))
        dao.kindLocked = true
        val vm = CategoryFormViewModel(repository, id)
        advanceUntilIdle()
        vm.onKindChange(INCOME)
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_category_kind_in_use, vm.uiState.value.operationError)
        assertNull(vm.uiState.value.completion)

        vm.onKindChange(EXPENSE)
        vm.onNameChange("Freelas")
        vm.save()
        advanceUntilIdle()
        assertEquals(FormCompletion.Saved, vm.uiState.value.completion)
        assertEquals("Freelas", repository.observe(id).firstValue()!!.details.name)
    }

    @Test
    fun failuresAreReportedWithoutLosingState() = runTest(dispatcher) {
        val id = repository.create(CategoryDetails("Saúde", EXPENSE))
        repository.archive(id)
        dao.failWrites = true

        val list = CategoriesViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { list.uiState.collect {} }
        list.reactivate(id)
        advanceUntilIdle()
        assertEquals(R.string.error_reactivate, list.uiState.value.errorMessage)
        assertEquals(listOf("Saúde"), list.uiState.value.archived.map { it.details.name })
        list.dismissError()
        advanceUntilIdle()
        assertNull(list.uiState.value.errorMessage)

        val form = CategoryFormViewModel(repository, null)
        form.onNameChange("Nova")
        form.save()
        advanceUntilIdle()
        assertEquals(R.string.error_save, form.uiState.value.operationError)
        assertEquals("Nova", form.uiState.value.name)
    }
}
