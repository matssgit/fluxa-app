package com.fluxa.personal.ui.feature.credit

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentPaymentError
import com.fluxa.personal.domain.model.InstallmentPaymentResult
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.rules.BalanceSnapshot
import com.fluxa.personal.ui.common.runCatchingNonCancellation
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado explícito de um comando (pagar ou desfazer). A integridade continua no backend; isto só evita toque duplo. */
internal enum class CommandStatus { Idle, Submitting, Success, Error }

// region Detalhe da parcela (leitura + desfazer)

internal data class InstallmentDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val purchase: Purchase? = null,
    val installment: Installment? = null,
    val installmentCount: Int = 0,
    val card: CreditCardSummary? = null,
    /** Baixa vinda de `observeInstallmentPayment`; `null` enquanto a parcela não está paga. */
    val payment: FinancialTransaction? = null,
    /** Conta da baixa (inclusive arquivada depois do pagamento). */
    val paymentAccount: FinancialAccount? = null,
    val undoStatus: CommandStatus = CommandStatus.Idle,
    @StringRes val undoError: Int? = null,
    /** Incrementa a cada estorno concluído, para a tela mostrar o feedback uma vez. */
    val undoneCount: Int = 0,
    @StringRes val loadError: Int? = null,
) {
    val status: InstallmentStatus? get() = installment?.status
    private val purchaseActive: Boolean get() = purchase != null && purchase.cancelledAt == null
    /** Pagar só é oferecido para parcela pendente de compra ativa (o backend continua decidindo). */
    val canPay: Boolean get() = purchaseActive && status == InstallmentStatus.PENDING
    /** Desfazer exige a baixa lida do contrato: é o UUID dela que vai em `expectedTransactionId`. */
    val canUndo: Boolean get() = purchaseActive && status == InstallmentStatus.PAID && payment != null
}

/**
 * Detalhe de uma parcela. A compra é localizada em `observePurchases(cardId)` e a parcela em
 * `observeInstallments(purchaseId)` (o contrato não tem consulta por ID). A baixa vem diretamente de
 * `observeInstallmentPayment` — nada de varrer o Caixa. Desfazer usa só `undoInstallmentPayment`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class InstallmentDetailViewModel(
    private val credit: CreditSource,
    accounts: AccountRepository,
    cardId: UUID,
    purchaseId: UUID,
    private val installmentId: UUID,
) : ViewModel() {
    private data class Undo(
        val status: CommandStatus = CommandStatus.Idle,
        @StringRes val error: Int? = null,
        val undoneCount: Int = 0,
    )

    private val undo = MutableStateFlow(Undo())

    private val paymentWithAccount: Flow<Pair<FinancialTransaction?, FinancialAccount?>> =
        credit.observeInstallmentPayment(installmentId).flatMapLatest { tx ->
            if (tx == null) flowOf(null to null) else accounts.observe(tx.details.accountId).map { tx to it }
        }

    val uiState: StateFlow<InstallmentDetailUiState> = combine(
        credit.observePurchases(cardId),
        credit.observeInstallments(purchaseId),
        paymentWithAccount,
        credit.observeCard(cardId),
        undo,
    ) { purchases, installments, (payment, account), card, u ->
        val purchase = purchases.firstOrNull { it.id == purchaseId }
        val installment = installments.firstOrNull { it.id == installmentId }
        InstallmentDetailUiState(
            isLoading = false,
            notFound = purchase == null || installment == null,
            purchase = purchase,
            installment = installment,
            installmentCount = purchase?.details?.installmentCount ?: installments.size,
            card = card,
            payment = payment,
            paymentAccount = account,
            undoStatus = u.status,
            undoError = u.error,
            undoneCount = u.undoneCount,
        )
    }
        .catch { emit(InstallmentDetailUiState(isLoading = false, loadError = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallmentDetailUiState())

    fun dismissUndoError() = undo.update { it.copy(status = CommandStatus.Idle, error = null) }

    fun undoPayment() {
        val state = uiState.value
        val payment = state.payment ?: return
        if (undo.value.status == CommandStatus.Submitting || !state.canUndo) return
        undo.update { it.copy(status = CommandStatus.Submitting, error = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation { credit.undoInstallmentPayment(installmentId, payment.id) }
            undo.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is InstallmentPaymentResult.Undone ->
                                current.copy(status = CommandStatus.Success, error = null, undoneCount = current.undoneCount + 1)
                            is InstallmentPaymentResult.Rejected ->
                                current.copy(status = CommandStatus.Error, error = undoErrorMessage(result.error))
                            // Não são respostas de estorno; trata como mudança concorrente e deixa os Flows mostrarem o estado real.
                            is InstallmentPaymentResult.Paid, is InstallmentPaymentResult.AlreadyPaid ->
                                current.copy(status = CommandStatus.Error, error = R.string.error_payment_changed)
                        }
                    },
                    onFailure = { current.copy(status = CommandStatus.Error, error = R.string.error_undo_payment) },
                )
            }
        }
    }

    companion object {
        @StringRes
        fun undoErrorMessage(error: InstallmentPaymentError): Int = when (error) {
            InstallmentPaymentError.INSUFFICIENT_LIMIT_TO_UNDO -> R.string.error_insufficient_limit_to_undo
            InstallmentPaymentError.INSTALLMENT_NOT_PAID -> R.string.error_installment_not_paid
            InstallmentPaymentError.PAYMENT_CHANGED -> R.string.error_payment_changed
            InstallmentPaymentError.PURCHASE_CANCELLED -> R.string.error_payment_purchase_cancelled
            InstallmentPaymentError.INSTALLMENT_CANCELLED -> R.string.error_installment_cancelled
            InstallmentPaymentError.INSTALLMENT_NOT_FOUND -> R.string.error_installment_not_found
            else -> R.string.error_undo_payment
        }

        fun factory(credit: CreditSource, accounts: AccountRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                CreditRoutes.run {
                    InstallmentDetailViewModel(
                        credit,
                        accounts,
                        UUID.fromString(requireNotNull(handle.get<String>(ARG_CARD))),
                        UUID.fromString(requireNotNull(handle.get<String>(ARG_PURCHASE))),
                        UUID.fromString(requireNotNull(handle.get<String>(ARG_INSTALLMENT))),
                    )
                }
            }
        }
    }
}

// endregion

// region Pagamento da parcela

internal enum class PaymentStep { Form, Confirm }

internal enum class PaymentOutcome { Paid, AlreadyPaid }

internal data class PaymentAccountOption(val account: FinancialAccount, val balance: CalculatedBalance?)

internal data class InstallmentPaymentUiState(
    val today: LocalDate,
    val paidOn: LocalDate,
    val accounts: List<PaymentAccountOption> = emptyList(),
    val accountsLoaded: Boolean = false,
    val accountId: UUID? = null,
    val step: PaymentStep = PaymentStep.Form,
    val status: CommandStatus = CommandStatus.Idle,
    @StringRes val accountError: Int? = null,
    @StringRes val dateError: Int? = null,
    @StringRes val operationError: Int? = null,
    val outcome: PaymentOutcome? = null,
    /** Incrementa a cada baixa confirmada (nova ou idempotente) para o feedback aparecer uma vez. */
    val completedCount: Int = 0,
) {
    val selected: PaymentAccountOption? get() = accounts.firstOrNull { it.account.id == accountId }
    val isSubmitting: Boolean get() = status == CommandStatus.Submitting
}

/**
 * Formulário de baixa: conta (ativas, com saldo atual de `observeBalances`) e data (hoje por padrão; datas
 * futuras bloqueadas só na UX). Valor e categoria vêm do backend. O resultado `AlreadyPaid` é sucesso idempotente.
 */
internal class InstallmentPaymentViewModel(
    private val credit: CreditSource,
    accounts: AccountRepository,
    balances: Flow<BalanceSnapshot>,
    private val installmentId: UUID,
    calendar: FinancialCalendar = FinancialCalendar(),
) : ViewModel() {
    private val today = calendar.today()
    private val _uiState = MutableStateFlow(InstallmentPaymentUiState(today = today, paidOn = today))
    val uiState: StateFlow<InstallmentPaymentUiState> = _uiState.asStateFlow()

    init {
        combine(accounts.observeAll(includeArchived = false), balances) { list, snapshot ->
            list.map { PaymentAccountOption(it, snapshot.accounts[it.id]) }
        }
            .onEach { options ->
                _uiState.update { state ->
                    val stillThere = options.any { it.account.id == state.accountId }
                    val lost = state.accountId != null && !stillThere && !state.isSubmitting
                    val accountId = when {
                        stillThere -> state.accountId
                        lost -> null
                        // Conta única é escolhida automaticamente, salvo se o usuário acabou de perder a escolha.
                        options.size == 1 && state.accountError == null -> options.single().account.id
                        else -> null
                    }
                    state.copy(
                        accounts = options,
                        accountsLoaded = true,
                        accountId = accountId,
                        // A conta escolhida foi arquivada/removida: volta para a escolha em vez de confirmar às cegas.
                        step = if (lost) PaymentStep.Form else state.step,
                        accountError = if (lost) R.string.error_payment_account_invalid else state.accountError,
                    )
                }
            }
            .catch { _uiState.update { it.copy(accountsLoaded = true, operationError = R.string.error_load) } }
            .launchIn(viewModelScope)
    }

    /** Reabre o formulário limpo (data volta para hoje), preservando a conta escolhida se ainda existir. */
    fun start() = _uiState.update {
        if (it.isSubmitting) it
        else it.copy(
            paidOn = today, step = PaymentStep.Form, status = CommandStatus.Idle,
            accountError = null, dateError = null, operationError = null, outcome = null,
        )
    }

    fun onAccountChange(id: UUID) = _uiState.update { if (it.isSubmitting) it else it.copy(accountId = id, accountError = null) }

    fun onDateChange(value: LocalDate) = _uiState.update { if (it.isSubmitting) it else it.copy(paidOn = value, dateError = null) }

    /** Vai para o resumo. Só verifica o que a UX consegue saber (conta escolhida, data não futura). */
    fun review() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val accountError = if (state.selected == null) R.string.error_payment_account_required else null
        val dateError = if (state.paidOn > state.today) R.string.error_payment_future_date else null
        _uiState.update {
            if (accountError != null || dateError != null) it.copy(accountError = accountError, dateError = dateError)
            else it.copy(step = PaymentStep.Confirm, operationError = null)
        }
    }

    fun backToForm() = _uiState.update { if (it.isSubmitting) it else it.copy(step = PaymentStep.Form) }

    fun confirm() {
        val state = _uiState.value
        val accountId = state.accountId ?: return
        if (state.isSubmitting || state.step != PaymentStep.Confirm) return
        _uiState.update { it.copy(status = CommandStatus.Submitting, operationError = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation { credit.payInstallment(installmentId, accountId, state.paidOn) }
            _uiState.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is InstallmentPaymentResult.Paid -> current.succeeded(PaymentOutcome.Paid)
                            // Baixa já existente (reenvio ou concorrência): sucesso idempotente; os Flows mostram a baixa real.
                            is InstallmentPaymentResult.AlreadyPaid -> current.succeeded(PaymentOutcome.AlreadyPaid)
                            is InstallmentPaymentResult.Rejected -> current.rejected(result.error)
                            is InstallmentPaymentResult.Undone -> current.copy(status = CommandStatus.Error, operationError = R.string.error_payment_changed)
                        }
                    },
                    onFailure = { current.copy(status = CommandStatus.Error, operationError = R.string.error_pay_installment) },
                )
            }
        }
    }

    private fun InstallmentPaymentUiState.succeeded(result: PaymentOutcome) =
        copy(status = CommandStatus.Success, outcome = result, completedCount = completedCount + 1)

    private fun InstallmentPaymentUiState.rejected(error: InstallmentPaymentError): InstallmentPaymentUiState = when (error) {
        InstallmentPaymentError.ACCOUNT_NOT_FOUND, InstallmentPaymentError.ACCOUNT_ARCHIVED ->
            copy(status = CommandStatus.Error, step = PaymentStep.Form, accountError = R.string.error_payment_account_invalid)
        InstallmentPaymentError.PAYMENT_DATE_IN_FUTURE ->
            copy(status = CommandStatus.Error, step = PaymentStep.Form, dateError = R.string.error_payment_future_date)
        InstallmentPaymentError.PAYMENT_BEFORE_ACCOUNT_OPENING ->
            copy(status = CommandStatus.Error, step = PaymentStep.Form, dateError = R.string.error_payment_before_opening)
        else -> copy(status = CommandStatus.Error, operationError = paymentErrorMessage(error))
    }

    companion object {
        @StringRes
        fun paymentErrorMessage(error: InstallmentPaymentError): Int = when (error) {
            InstallmentPaymentError.INSTALLMENT_NOT_FOUND -> R.string.error_installment_not_found
            InstallmentPaymentError.INSTALLMENT_CANCELLED -> R.string.error_installment_cancelled
            InstallmentPaymentError.PURCHASE_CANCELLED -> R.string.error_payment_purchase_cancelled
            InstallmentPaymentError.PAYMENT_CHANGED, InstallmentPaymentError.INVALID_PAYMENT_LINK -> R.string.error_payment_changed
            else -> R.string.error_pay_installment
        }

        fun factory(credit: CreditSource, accounts: AccountRepository, balances: Flow<BalanceSnapshot>): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val handle = createSavedStateHandle()
                    InstallmentPaymentViewModel(
                        credit,
                        accounts,
                        balances,
                        UUID.fromString(requireNotNull(handle.get<String>(CreditRoutes.ARG_INSTALLMENT))),
                    )
                }
            }
    }
}

// endregion
