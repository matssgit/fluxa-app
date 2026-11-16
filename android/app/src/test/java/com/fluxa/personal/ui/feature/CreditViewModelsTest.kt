package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import com.fluxa.personal.ui.feature.credit.CardDetailViewModel
import com.fluxa.personal.ui.feature.credit.CardFormViewModel
import com.fluxa.personal.ui.feature.credit.CardsViewModel
import com.fluxa.personal.ui.feature.credit.CreditCompletion
import com.fluxa.personal.ui.feature.credit.PurchaseDetailViewModel
import com.fluxa.personal.ui.feature.credit.PurchaseFormViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreditViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-16T12:00:00Z")
    private val today = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val calendar = FinancialCalendar(clock) { ZoneOffset.UTC }
    private lateinit var categories: CategoryRepository
    private lateinit var credit: FakeCreditSource
    private lateinit var market: UUID

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        categories = CategoryRepository(FakeCategoryDao(), clock)
        credit = FakeCreditSource(categories, now)
        market = categories.create(CategoryDetails("Mercado", EXPENSE))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.collect(flow: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    private suspend fun card(name: String, limitCents: Long, closing: Int = 10, due: Int = 17): UUID =
        (credit.createCard(CreditCardDetails(name, Money(limitCents), BillingDay(closing), BillingDay(due))) as CreditResult.Saved).value.id

    private suspend fun purchase(cardId: UUID, cents: Long, count: Int, title: String = "Compra", date: LocalDate = today): UUID {
        val id = UUID.randomUUID()
        credit.createPurchase(id, PurchaseDraft(cardId, market, title, Money(cents), count, date))
        return id
    }

    // region Cartões

    @Test
    fun emptyListThenActiveAndArchivedWithLimitsFromTheSource() = runTest(dispatcher) {
        val vm = CardsViewModel(credit)
        collect(vm.uiState)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isEmpty)

        val a = card("Principal", 500_000)
        val b = card("Viagem", 100_000)
        purchase(a, 120_000, 3)
        credit.archiveCard(b)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(listOf("Principal"), active.map { it.card.details.name })
            assertEquals(listOf("Viagem"), archived.map { it.card.details.name })
            val s = active.single()
            assertEquals(Money(500_000), s.totalLimit)
            assertEquals(Money(120_000), s.committedAmount)
            assertEquals(Money(380_000), s.availableLimit)
        }
    }

    @Test
    fun cardFormValidatesFormatAndCreates() = runTest(dispatcher) {
        val vm = CardFormViewModel(credit, null)
        vm.save()
        with(vm.uiState.value) {
            assertEquals(R.string.error_name_required, nameError)
            assertEquals(R.string.error_card_limit_required, limitError)
            assertEquals(R.string.error_billing_day, closingDayError)
            assertEquals(R.string.error_billing_day, dueDayError)
        }
        vm.onNameChange("Cartão casa")
        vm.onLimitChange("5.000,00")
        vm.onClosingDayChange("32")
        vm.onDueDayChange("7")
        vm.save()
        assertEquals(R.string.error_billing_day, vm.uiState.value.closingDayError)
        vm.onClosingDayChange("31")
        vm.onColorChange("#35695A")
        vm.save()
        advanceUntilIdle()
        assertEquals(CreditCompletion.Saved, vm.uiState.value.completion)
        val saved = credit.observeCards().first().single().card.details
        assertEquals(CreditCardDetails("Cartão casa", Money(500_000), BillingDay(31), BillingDay(7), "#35695A"), saved)
    }

    @Test
    fun editLoadsAndShowsLimitBelowCommittedWithoutChangingIt() = runTest(dispatcher) {
        val id = card("Principal", 300_000)
        purchase(id, 200_000, 2)
        val vm = CardFormViewModel(credit, id)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals("Principal", name)
            assertEquals("3.000,00", limitText)
            assertEquals("10", closingDayText)
            assertEquals("17", dueDayText)
        }
        vm.onLimitChange("1.000,00")
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_limit_below_committed, vm.uiState.value.limitError)
        assertNull(vm.uiState.value.completion)
        assertEquals(Money(300_000), credit.observeCard(id).first()!!.totalLimit)

        vm.onLimitChange("2.000,00") // exatamente o comprometido é aceito pelo contrato
        vm.onNameChange("Principal renomeado")
        vm.save()
        advanceUntilIdle()
        assertEquals(CreditCompletion.Saved, vm.uiState.value.completion)
        assertEquals("Principal renomeado", credit.observeCard(id).first()!!.card.details.name)
    }

    @Test
    fun archiveKeepsHistory() = runTest(dispatcher) {
        val id = card("Antigo", 100_000)
        val p = purchase(id, 10_000, 1)
        val vm = CardFormViewModel(credit, id)
        advanceUntilIdle()
        vm.archive()
        vm.archive()
        advanceUntilIdle()
        assertEquals(CreditCompletion.Archived, vm.uiState.value.completion)
        assertNotNull(credit.observeCard(id).first()!!.card.archivedAt)
        assertEquals(listOf(p), credit.observePurchases(id).first().map { it.id })
    }

    // endregion

    // region Compra

    private fun form(cardId: UUID? = null) = PurchaseFormViewModel(credit, categories, cardId, calendar)

    @Test
    fun purchaseFormOffersActiveCardsAndExpenseCategoriesOnly() = runTest(dispatcher) {
        val active = card("Ativo", 100_000)
        val old = card("Velho", 100_000)
        credit.archiveCard(old)
        categories.create(CategoryDetails("Salário", INCOME))
        val archivedCategory = categories.create(CategoryDetails("Antiga", EXPENSE))
        categories.archive(archivedCategory)

        val vm = form()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(listOf("Ativo"), cards.map { it.card.details.name })
            assertEquals(active, cardId) // único cartão ativo é pré-selecionado
            assertEquals(listOf("Mercado"), categories.map { it.details.name })
            assertEquals(today, purchaseDate)
        }
    }

    @Test
    fun validFormCreatesInstallmentsPersistedByTheBackend() = runTest(dispatcher) {
        val id = card("Principal", 500_000)
        val vm = form(id)
        advanceUntilIdle()
        vm.onAmountChange("1.000,00")
        vm.onInstallmentsChange("3")
        vm.onCategoryChange(market)
        vm.onTitleChange("Notebook")
        vm.onMerchantChange("Loja")
        vm.save()
        advanceUntilIdle()
        val created = vm.uiState.value.created!!
        assertEquals(id, created.cardId)
        val parts = credit.observeInstallments(created.purchaseId).first()
        assertEquals(listOf(1, 2, 3), parts.map { it.number })
        assertEquals(100_000L, parts.sumOf { it.amount.cents }) // soma no teste, não na UI
        assertEquals(Money(100_000), credit.observeCard(id).first()!!.committedAmount)
    }

    @Test
    fun formErrorsAndBackendRejections() = runTest(dispatcher) {
        val id = card("Pequeno", 5_000)
        val vm = form(id)
        advanceUntilIdle()
        vm.save()
        with(vm.uiState.value) {
            assertEquals(R.string.error_amount_positive, amountError)
            assertEquals(R.string.error_category_required, categoryError)
            assertEquals(R.string.error_title_required, titleError)
        }
        vm.onCategoryChange(market)
        vm.onTitleChange("TV")
        vm.onAmountChange("100,00")
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_insufficient_limit, vm.uiState.value.amountError) // limite disponível 50,00
        assertNull(vm.uiState.value.created)

        vm.onAmountChange("0,02")
        vm.onInstallmentsChange("3") // 3 parcelas de pelo menos 1 centavo não cabem em 2 centavos
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_installments_invalid, vm.uiState.value.installmentsError)
        vm.onInstallmentsChange("0")
        vm.save()
        assertEquals(R.string.error_installments_invalid, vm.uiState.value.installmentsError)
    }

    @Test
    fun archivedCardAndWrongCategoryAreMappedToFieldErrors() = runTest(dispatcher) {
        val id = card("Principal", 500_000)
        val income = categories.create(CategoryDetails("Salário", INCOME))
        val vm = form(id)
        advanceUntilIdle()
        vm.onAmountChange("10,00")
        vm.onTitleChange("Item")
        vm.onCategoryChange(income) // categoria de receita não é oferecida; o backend rejeita mesmo assim
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_purchase_category, vm.uiState.value.categoryError)

        vm.onCategoryChange(market)
        credit.archiveCard(id)
        vm.onCardChange(id)
        vm.save()
        advanceUntilIdle()
        assertEquals(R.string.error_card_archived, vm.uiState.value.cardError)
    }

    @Test
    fun repeatedTapsCreateOnceAndLostResponseReusesTheSameId() = runTest(dispatcher) {
        val id = card("Principal", 500_000)
        val vm = form(id)
        advanceUntilIdle()
        vm.onAmountChange("50,00")
        vm.onCategoryChange(market)
        vm.onTitleChange("Item")
        credit.failAfterWrite = true
        repeat(3) { vm.save() }
        advanceUntilIdle()
        assertEquals(R.string.error_save, vm.uiState.value.operationError)
        repeat(3) { vm.save() }
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.created)
        assertEquals(1, credit.observePurchases(id).first().size)
        assertEquals(2, credit.createPurchaseIds.size)
        assertEquals(credit.createPurchaseIds[0], credit.createPurchaseIds[1])
    }

    // endregion

    // region Detalhe

    @Test
    fun detailShowsPurchasesInstallmentsAndUpcomingFromTheSource() = runTest(dispatcher) {
        val id = card("Principal", 900_000, closing = 10, due = 17)
        val tv = purchase(id, 60_000, 3, "TV", today)
        purchase(id, 10_000, 1, "Livro", today.minusDays(20))
        val vm = CardDetailViewModel(credit, categories, id)
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(Money(70_000), summary!!.committedAmount)
            assertEquals(listOf("TV", "Livro"), purchases.map { it.purchase.details.title })
            val tvItem = purchases.first { it.purchase.id == tv }
            assertEquals(3, tvItem.installments.size)
            assertEquals("Mercado", tvItem.category!!.details.name)
            assertEquals(4, upcoming.size)
            assertEquals(upcoming.map { it.installment.dueDate }.sorted(), upcoming.map { it.installment.dueDate })
        }
    }

    @Test
    fun cancelReleasesLimitKeepsHistoryAndBecomesReadOnly() = runTest(dispatcher) {
        val id = card("Principal", 500_000)
        val p = purchase(id, 30_000, 3, "Fone")
        val detail = PurchaseDetailViewModel(credit, categories, id, p)
        collect(detail.uiState)
        advanceUntilIdle()
        assertTrue(detail.uiState.value.canCancel)
        detail.cancelPurchase()
        detail.cancelPurchase()
        advanceUntilIdle()
        with(detail.uiState.value) {
            assertTrue(cancelled)
            assertTrue(isCancelled)
            assertFalse(canCancel)
            assertTrue(installments.all { it.status == InstallmentStatus.CANCELLED })
        }
        assertEquals(Money(0), credit.observeCard(id).first()!!.committedAmount)
        assertEquals(1, credit.observePurchases(id).first().size)
    }

    @Test
    fun paidInstallmentBlocksCancellation() = runTest(dispatcher) {
        val id = card("Principal", 500_000)
        val p = purchase(id, 30_000, 3)
        credit.markPaid(p, 1)
        val detail = PurchaseDetailViewModel(credit, categories, id, p)
        collect(detail.uiState)
        advanceUntilIdle()
        assertFalse(detail.uiState.value.canCancel)
        detail.cancelPurchase()
        advanceUntilIdle()
        assertNull(credit.observePurchases(id).first().single().cancelledAt)
    }

    // endregion
}
