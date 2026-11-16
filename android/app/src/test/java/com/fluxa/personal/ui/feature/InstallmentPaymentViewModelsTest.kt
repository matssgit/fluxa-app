package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.InstallmentPaymentError
import com.fluxa.personal.domain.model.InstallmentPaymentResult
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.feature.credit.CommandStatus
import com.fluxa.personal.ui.feature.credit.InstallmentDetailViewModel
import com.fluxa.personal.ui.feature.credit.InstallmentPaymentViewModel
import com.fluxa.personal.ui.feature.credit.PaymentOutcome
import com.fluxa.personal.ui.feature.credit.PaymentStep
import com.fluxa.personal.ui.feature.credit.PurchaseDetailViewModel
import com.fluxa.personal.ui.feature.transactions.TransactionFormViewModel
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

/**
 * Sprint 4-D: pagamento e estorno de parcelas na UI. O fake aplica `InstallmentPaymentRules`/`CreditLimit` do
 * domínio e grava a saída num ledger em memória; os testes verificam que a UI só repassa comandos e lê os Flows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InstallmentPaymentViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-16T12:00:00Z")
    private val today = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val calendar = FinancialCalendar(clock) { ZoneOffset.UTC }
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private lateinit var ledger: FakeTransactionSource
    private lateinit var credit: FakeCreditSource
    private lateinit var market: UUID
    private lateinit var main: UUID
    private lateinit var wallet: UUID

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        accounts = AccountRepository(FakeAccountDao(), clock)
        categories = CategoryRepository(FakeCategoryDao(), clock)
        ledger = FakeTransactionSource(accounts, categories, today, now)
        credit = FakeCreditSource(categories, now, accounts, ledger, today)
        market = categories.create(CategoryDetails("Mercado", EXPENSE))
        main = accounts.create(AccountDetails("Principal", AccountType.CHECKING, Money(50_000), LocalDate.of(2026, 1, 1)))
        wallet = accounts.create(AccountDetails("Carteira", AccountType.WALLET, Money(0), LocalDate.of(2026, 1, 1)))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.collect(flow: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    private class Scenario(val card: UUID, val purchase: UUID, val installments: List<UUID>)

    private suspend fun scenario(limitCents: Long = 500_000, totalCents: Long = 30_000, count: Int = 3, title: String = "Fone"): Scenario {
        val card = (credit.createCard(CreditCardDetails("Principal", Money(limitCents), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id
        val purchase = UUID.randomUUID()
        credit.createPurchase(purchase, PurchaseDraft(card, market, title, Money(totalCents), count, today.minusDays(40)))
        return Scenario(card, purchase, credit.observeInstallments(purchase).first().map { it.id })
    }

    private fun paymentVm(installment: UUID) = InstallmentPaymentViewModel(credit, accounts, ledger.observeBalances(), installment, calendar)
    private fun detailVm(s: Scenario, installment: UUID) = InstallmentDetailViewModel(credit, accounts, s.card, s.purchase, installment)

    // region Pagamento

    @Test
    fun initialStateListsActiveAccountsWithBalancesAndDefaultsToToday() = runTest(dispatcher) {
        val archived = accounts.create(AccountDetails("Antiga", AccountType.SAVINGS, Money(0), LocalDate.of(2026, 1, 1)))
        accounts.archive(archived)
        val s = scenario()
        val vm = paymentVm(s.installments[0])
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(today, paidOn)
            assertEquals(PaymentStep.Form, step)
            assertEquals(CommandStatus.Idle, status)
            assertNull(accountId) // duas contas ativas: o usuário escolhe
            assertEquals(setOf("Principal", "Carteira"), accounts.map { it.account.details.name }.toSet())
            assertEquals(CalculatedBalance.Exact(Money(50_000)), accounts.first { it.account.id == main }.balance)
        }
    }

    @Test
    fun selectingAccountAndDateFeedsTheConfirmationSummary() = runTest(dispatcher) {
        val s = scenario()
        val vm = paymentVm(s.installments[0])
        advanceUntilIdle()
        vm.review()
        assertEquals(R.string.error_payment_account_required, vm.uiState.value.accountError)
        assertEquals(PaymentStep.Form, vm.uiState.value.step)

        vm.onAccountChange(wallet)
        vm.onDateChange(today.plusDays(1))
        vm.review()
        assertEquals(R.string.error_payment_future_date, vm.uiState.value.dateError) // UX bloqueia data futura
        assertEquals(PaymentStep.Form, vm.uiState.value.step)

        vm.onDateChange(today.minusDays(20)) // pagamento atrasado/antecipado é válido
        vm.review()
        with(vm.uiState.value) {
            assertEquals(PaymentStep.Confirm, step)
            assertEquals("Carteira", selected!!.account.details.name)
            assertEquals(today.minusDays(20), paidOn)
            assertNull(dateError)
        }
        assertTrue(credit.payCalls.isEmpty()) // revisar não envia nada
    }

    @Test
    fun confirmingPaysOnceAndFlowsReflectLedgerAndLimit() = runTest(dispatcher) {
        val s = scenario()
        val vm = paymentVm(s.installments[1])
        collect(vm.uiState)
        advanceUntilIdle()
        vm.onAccountChange(main)
        vm.onDateChange(today.minusDays(3))
        vm.review()
        vm.confirm()
        vm.confirm() // toque duplo
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Success, status)
            assertEquals(PaymentOutcome.Paid, outcome)
            assertEquals(1, completedCount)
        }
        assertEquals(1, credit.payCalls.size)
        val tx = ledger.rows.value.values.single()
        assertEquals(s.installments[1], tx.installmentId)
        assertEquals(EXPENSE, tx.details.direction)
        assertEquals(Money(10_000), tx.details.amount)
        assertEquals(today.minusDays(3), tx.details.occurredOn)
        assertEquals(InstallmentStatus.PAID, credit.observeInstallments(s.purchase).first()[1].status)
        assertEquals(Money(20_000), credit.observeCard(s.card).first()!!.committedAmount) // limite vem do contrato
        assertEquals(CalculatedBalance.Exact(Money(40_000)), vm.uiState.value.accounts.first { it.account.id == main }.balance)
    }

    @Test
    fun alreadyPaidIsAnIdempotentSuccessWithoutDuplicatingTheExpense() = runTest(dispatcher) {
        val s = scenario()
        val first = credit.payInstallment(s.installments[0], main, today)
        assertTrue(first is InstallmentPaymentResult.Paid)
        val vm = paymentVm(s.installments[0])
        advanceUntilIdle()
        vm.onAccountChange(wallet)
        vm.review()
        vm.confirm()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Success, status)
            assertEquals(PaymentOutcome.AlreadyPaid, outcome)
            assertNull(operationError)
        }
        assertEquals(1, ledger.rows.value.size)
        assertEquals(main, ledger.rows.value.values.single().details.accountId) // prevalece a primeira baixa
    }

    @Test
    fun accountRejectedByBackendReturnsToAccountChoice() = runTest(dispatcher) {
        val s = scenario()
        val vm = paymentVm(s.installments[0])
        advanceUntilIdle()
        vm.onAccountChange(main)
        vm.review()
        credit.rejectNextPayment = InstallmentPaymentError.ACCOUNT_ARCHIVED
        vm.confirm()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Error, status)
            assertEquals(PaymentStep.Form, step)
            assertEquals(R.string.error_payment_account_invalid, accountError)
        }
        assertTrue(ledger.rows.value.isEmpty())
    }

    @Test
    fun accountArchivedWhileConfirmingGoesBackToTheForm() = runTest(dispatcher) {
        val s = scenario()
        val vm = paymentVm(s.installments[0])
        advanceUntilIdle()
        vm.onAccountChange(wallet)
        vm.review()
        assertEquals(PaymentStep.Confirm, vm.uiState.value.step)
        accounts.archive(wallet)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(PaymentStep.Form, step)
            assertNull(accountId)
            assertEquals(R.string.error_payment_account_invalid, accountError)
        }
    }

    @Test
    fun dateBeforeAccountOpeningRejectedByBackendShowsDateError() = runTest(dispatcher) {
        val s = scenario()
        val vm = paymentVm(s.installments[0])
        advanceUntilIdle()
        vm.onAccountChange(main)
        vm.onDateChange(LocalDate.of(2025, 12, 1)) // antes da abertura (01/01/2026)
        vm.review()
        vm.confirm()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Error, status)
            assertEquals(PaymentStep.Form, step)
            assertEquals(R.string.error_payment_before_opening, dateError)
        }
        assertEquals(InstallmentStatus.PENDING, credit.observeInstallments(s.purchase).first()[0].status)
    }

    @Test
    fun unexpectedFailureAllowsRetry() = runTest(dispatcher) {
        val s = scenario()
        val vm = paymentVm(s.installments[0])
        advanceUntilIdle()
        vm.onAccountChange(main)
        vm.review()
        credit.failPayments = true
        vm.confirm()
        advanceUntilIdle()
        assertEquals(CommandStatus.Error, vm.uiState.value.status)
        assertEquals(R.string.error_pay_installment, vm.uiState.value.operationError)
        credit.failPayments = false
        vm.confirm()
        advanceUntilIdle()
        assertEquals(CommandStatus.Success, vm.uiState.value.status)
        assertEquals(1, ledger.rows.value.size)
    }

    // endregion

    // region Detalhe e estorno

    @Test
    fun pendingInstallmentOffersPaymentOnly() = runTest(dispatcher) {
        val s = scenario()
        val vm = detailVm(s, s.installments[0])
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(InstallmentStatus.PENDING, status)
            assertTrue(canPay)
            assertFalse(canUndo)
            assertNull(payment)
            assertEquals(3, installmentCount)
        }
    }

    @Test
    fun paidInstallmentShowsPaymentFromObserveInstallmentPayment() = runTest(dispatcher) {
        val s = scenario()
        val vm = detailVm(s, s.installments[0])
        collect(vm.uiState)
        credit.payInstallment(s.installments[0], wallet, today.minusDays(5))
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(InstallmentStatus.PAID, status)
            assertFalse(canPay)
            assertTrue(canUndo)
            assertEquals(today.minusDays(5), payment!!.details.occurredOn)
            assertEquals("Carteira", paymentAccount!!.details.name)
            assertNotNull(installment!!.dueDate) // vencimento original preservado, separado do "Pago em"
            assertTrue(installment!!.dueDate != payment!!.details.occurredOn)
        }
    }

    @Test
    fun cancelledInstallmentIsReadOnly() = runTest(dispatcher) {
        val s = scenario()
        credit.cancelPurchase(s.purchase)
        val vm = detailVm(s, s.installments[2])
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(InstallmentStatus.CANCELLED, status)
            assertFalse(canPay)
            assertFalse(canUndo)
        }
    }

    @Test
    fun undoRemovesTheExpenseAndRecommitsTheLimit() = runTest(dispatcher) {
        val s = scenario()
        credit.payInstallment(s.installments[0], main, today)
        val vm = detailVm(s, s.installments[0])
        collect(vm.uiState)
        advanceUntilIdle()
        vm.undoPayment()
        vm.undoPayment() // toque duplo
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Success, undoStatus)
            assertEquals(1, undoneCount)
            assertEquals(InstallmentStatus.PENDING, status)
            assertNull(payment)
            assertTrue(canPay)
        }
        assertTrue(ledger.rows.value.isEmpty())
        assertEquals(Money(30_000), credit.observeCard(s.card).first()!!.committedAmount)
    }

    @Test
    fun undoBlockedWhenReleasedLimitWasUsedKeepsThePayment() = runTest(dispatcher) {
        val s = scenario(limitCents = 30_000, totalCents = 30_000, count = 1, title = "TV")
        credit.payInstallment(s.installments[0], main, today)
        credit.createPurchase(UUID.randomUUID(), PurchaseDraft(s.card, market, "Outra", Money(30_000), 1, today)) // consome o limite liberado
        val vm = detailVm(s, s.installments[0])
        collect(vm.uiState)
        advanceUntilIdle()
        vm.undoPayment()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Error, undoStatus)
            assertEquals(R.string.error_insufficient_limit_to_undo, undoError)
            assertEquals(InstallmentStatus.PAID, status)
            assertNotNull(payment)
        }
        assertEquals(1, ledger.rows.value.size)
    }

    @Test
    fun undoUnexpectedFailureShowsGenericMessage() = runTest(dispatcher) {
        val s = scenario()
        credit.payInstallment(s.installments[0], main, today)
        val vm = detailVm(s, s.installments[0])
        collect(vm.uiState)
        advanceUntilIdle()
        credit.failPayments = true
        vm.undoPayment()
        advanceUntilIdle()
        assertEquals(R.string.error_undo_payment, vm.uiState.value.undoError)
        assertEquals(InstallmentStatus.PAID, vm.uiState.value.status)
        vm.dismissUndoError()
        advanceUntilIdle()
        assertEquals(CommandStatus.Idle, vm.uiState.value.undoStatus)
    }

    @Test
    fun purchaseWithPaidInstallmentDoesNotOfferCancel() = runTest(dispatcher) {
        val s = scenario()
        credit.payInstallment(s.installments[0], main, today)
        val vm = PurchaseDetailViewModel(credit, categories, s.card, s.purchase)
        collect(vm.uiState)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canCancel)
    }

    // endregion

    // region Caixa

    @Test
    fun generatedTransactionOpensReadOnlyAndCannotBeEditedOrCancelled() = runTest(dispatcher) {
        val s = scenario()
        val paid = credit.payInstallment(s.installments[0], wallet, today) as InstallmentPaymentResult.Paid
        accounts.archive(wallet) // nome ainda aparece no modo leitura
        val vm = TransactionFormViewModel(ledger, accounts, categories, paid.transaction.id, calendar = calendar)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertTrue(isManagedByInstallment)
            assertTrue(isReadOnly)
            assertEquals("Carteira", managedAccount!!.details.name)
            assertEquals("Mercado", managedCategory!!.details.name)
            assertEquals(Money(10_000), managedAmount)
        }
        vm.save()
        vm.cancelTransaction()
        advanceUntilIdle()
        assertNull(vm.uiState.value.completion)
        assertNull(ledger.rows.value.values.single().cancelledAt)
    }

    // endregion
}
