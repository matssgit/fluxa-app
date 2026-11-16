package com.fluxa.personal.ui.feature.transactions

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
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionError
import com.fluxa.personal.domain.model.TransactionResult
import com.fluxa.personal.ui.common.MoneyInput
import com.fluxa.personal.ui.common.runCatchingNonCancellation
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class TransactionCompletion { Created, Updated, Cancelled }

internal data class TransactionFormUiState(
    val transactionId: UUID? = null,
    val direction: TransactionDirection = TransactionDirection.EXPENSE,
    val amountText: String = "",
    val accountId: UUID? = null,
    val categoryId: UUID? = null,
    val occurredOn: LocalDate,
    val today: LocalDate,
    val description: String = "",
    val accounts: List<FinancialAccount> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accountsLoaded: Boolean = false,
    val categoriesLoaded: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    /** Lançamento cancelado: detalhes visíveis, edição bloqueada pelo contrato. */
    val cancelledAt: Instant? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    /** Saída gerada pelo pagamento de uma parcela: o contrato devolve `MANAGED_BY_INSTALLMENT` para editar/cancelar. */
    val installmentId: UUID? = null,
    /** Saída gerada pelo pagamento de uma assinatura: `MANAGED_BY_SUBSCRIPTION` para editar/cancelar. */
    val subscriptionId: UUID? = null,
    /** Origem da parcela paga (cartão/compra/parcela), lida de `observeInstallmentOrigin` para o atalho "Ver parcela". */
    val installmentOrigin: com.fluxa.personal.domain.model.InstallmentOrigin? = null,
    /** Conta e categoria do lançamento gerenciado (podem ter sido arquivadas depois). */
    val managedAccount: FinancialAccount? = null,
    val managedCategory: Category? = null,
    /** Valor persistido da baixa (exibido sem passar pelo campo de edição). */
    val managedAmount: com.fluxa.personal.core.money.Money? = null,
    @StringRes val amountError: Int? = null,
    @StringRes val accountError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val dateError: Int? = null,
    @StringRes val operationError: Int? = null,
    val isSaving: Boolean = false,
    val isCancelling: Boolean = false,
    val completion: TransactionCompletion? = null,
) {
    val isEditing: Boolean get() = transactionId != null
    val isCancelled: Boolean get() = cancelledAt != null
    val isBusy: Boolean get() = isLoading || isSaving || isCancelling
    val isManagedByInstallment: Boolean get() = installmentId != null
    val isManagedBySubscription: Boolean get() = subscriptionId != null
    val isManaged: Boolean get() = isManagedByInstallment || isManagedBySubscription
    val isReadOnly: Boolean get() = isCancelled || notFound || isManaged
    /** A conta escolhida (edição) pode ter sido arquivada depois do lançamento. */
    val selectedAccountInactive: Boolean get() = accountsLoaded && accountId != null && accounts.none { it.id == accountId }
    val selectedCategoryInactive: Boolean get() = categoriesLoaded && categoryId != null && categories.none { it.id == categoryId }
}

/**
 * Formulário de receita/despesa sobre o [TransactionSource] (repositório do Codex).
 *
 * - O valor vem de [MoneyInput] e precisa ser positivo; a direção dá o sinal (contrato).
 * - Contas e categorias listadas são só as ativas, vindas dos Flows dos repositórios; categorias filtradas
 *   pela direção.
 * - O UUID de criação é gerado no primeiro envio e reutilizado nos reenvios, como pede o contrato; um
 *   `DUPLICATE_ID` consulta o mesmo ID para recuperar o resultado do primeiro envio.
 * - Nenhum saldo é calculado aqui.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TransactionFormViewModel(
    private val transactions: TransactionSource,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    transactionId: UUID?,
    initialDirection: TransactionDirection = TransactionDirection.EXPENSE,
    calendar: FinancialCalendar = FinancialCalendar(),
    private val installmentOrigins: ((UUID) -> kotlinx.coroutines.flow.Flow<com.fluxa.personal.domain.model.InstallmentOrigin?>)? = null,
) : ViewModel() {

    private val today = calendar.today()
    private val _uiState = MutableStateFlow(
        TransactionFormUiState(
            transactionId = transactionId,
            direction = initialDirection,
            occurredOn = today,
            today = today,
            isLoading = transactionId != null,
        ),
    )
    val uiState: StateFlow<TransactionFormUiState> = _uiState.asStateFlow()
    private val direction = MutableStateFlow(initialDirection)
    private var submissionId: UUID? = null

    init {
        accounts.observeAll(includeArchived = false)
            .onEach { list ->
                _uiState.update { state ->
                    val autoSelect = state.accountId == null && !state.isEditing && list.size == 1
                    state.copy(
                        accounts = list,
                        accountsLoaded = true,
                        accountId = if (autoSelect) list.single().id else state.accountId,
                    )
                }
            }
            .catch { _uiState.update { it.copy(accountsLoaded = true, operationError = R.string.error_load) } }
            .launchIn(viewModelScope)

        direction
            .flatMapLatest { kind -> categories.observeAll(includeArchived = false, kind = kind).map { kind to it } }
            .onEach { (kind, list) ->
                _uiState.update { state ->
                    if (state.direction != kind) state
                    else state.copy(categories = list, categoriesLoaded = true)
                }
            }
            .catch { _uiState.update { it.copy(categoriesLoaded = true, operationError = R.string.error_load) } }
            .launchIn(viewModelScope)

        if (transactionId != null) {
            viewModelScope.launch {
                val loaded = runCatchingNonCancellation { transactions.observe(transactionId).first() }
                loaded.fold(
                    onSuccess = { tx ->
                        if (tx == null) {
                            _uiState.update { it.copy(isLoading = false, notFound = true) }
                        } else {
                            val d = tx.details
                            _uiState.update {
                                val sameDirection = it.direction == d.direction
                                it.copy(
                                    isLoading = false,
                                    direction = d.direction,
                                    amountText = MoneyInput.formatForEditing(d.amount),
                                    accountId = d.accountId,
                                    categoryId = d.categoryId,
                                    occurredOn = d.occurredOn,
                                    description = d.description.orEmpty(),
                                    cancelledAt = tx.cancelledAt,
                                    createdAt = tx.createdAt,
                                    updatedAt = tx.updatedAt,
                                    installmentId = tx.installmentId,
                                    subscriptionId = tx.subscriptionOccurrence?.subscriptionId,
                                    managedAmount = d.amount.takeIf { tx.installmentId != null || tx.subscriptionOccurrence != null },
                                    categories = if (sameDirection) it.categories else emptyList(),
                                    categoriesLoaded = sameDirection && it.categoriesLoaded,
                                )
                            }
                            direction.value = d.direction
                            if (tx.installmentId != null || tx.subscriptionOccurrence != null) loadManagedNames(d.accountId, d.categoryId)
                            tx.installmentId?.let(::loadInstallmentOrigin)
                            if (tx.subscriptionOccurrence != null) {
                                transactions.observe(transactionId)
                                    .onEach { current ->
                                        if (current == null) _uiState.update { it.copy(notFound = true) }
                                    }
                                    .catch { _uiState.update { it.copy(operationError = R.string.error_load) } }
                                    .launchIn(viewModelScope)
                            }
                        }
                    },
                    onFailure = { _uiState.update { it.copy(isLoading = false, operationError = R.string.error_load) } },
                )
            }
        }
    }

    /** Origem da parcela pelo contrato `observeInstallmentOrigin` (um join por PK; nada de varrer listas). */
    private fun loadInstallmentOrigin(installmentId: UUID) {
        val source = installmentOrigins ?: return
        viewModelScope.launch {
            val origin = runCatchingNonCancellation { source(installmentId).first() }.getOrNull()
            _uiState.update { it.copy(installmentOrigin = origin) }
        }
    }

    /** Nomes para o modo leitura do lançamento gerenciado; inclui registros arquivados. */
    private fun loadManagedNames(accountId: UUID, categoryId: UUID) {
        viewModelScope.launch {
            val account = runCatchingNonCancellation { accounts.observe(accountId).first() }.getOrNull()
            val category = runCatchingNonCancellation { categories.observe(categoryId).first() }.getOrNull()
            _uiState.update { it.copy(managedAccount = account, managedCategory = category) }
        }
    }

    fun onDirectionChange(value: TransactionDirection) {
        if (_uiState.value.isReadOnly || value == _uiState.value.direction) return
        direction.value = value
        _uiState.update {
            it.copy(direction = value, categoryId = null, categories = emptyList(), categoriesLoaded = false, categoryError = null)
        }
    }

    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = MoneyInput.sanitizeTyping(value), amountError = null) }
    fun onAccountChange(id: UUID) = _uiState.update { it.copy(accountId = id, accountError = null) }
    fun onCategoryChange(id: UUID) = _uiState.update { it.copy(categoryId = id, categoryError = null) }
    fun onDateChange(value: LocalDate) = _uiState.update { it.copy(occurredOn = value, dateError = null) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value.take(MAX_DESCRIPTION)) }
    fun dismissError() = _uiState.update { it.copy(operationError = null) }

    fun save() {
        val state = _uiState.value
        if (state.isBusy || state.isReadOnly || state.completion != null) return

        val amount = when (val parsed = MoneyInput.parse(state.amountText)) {
            is MoneyInput.Result.Valid -> parsed.money.takeIf { it.cents > 0 }
            else -> null
        }
        val amountError = amountErrorFor(MoneyInput.parse(state.amountText))
        val accountError = when {
            state.accountId == null -> R.string.error_account_required
            state.selectedAccountInactive -> R.string.error_account_archived
            else -> null
        }
        val categoryError = when {
            state.categoryId == null -> R.string.error_category_required
            state.selectedCategoryInactive -> R.string.error_category_archived
            else -> null
        }
        val dateError = if (state.occurredOn > state.today) R.string.error_future_date else null
        if (amount == null || amountError != null || accountError != null || categoryError != null || dateError != null) {
            _uiState.update {
                it.copy(amountError = amountError, accountError = accountError, categoryError = categoryError, dateError = dateError)
            }
            return
        }

        val draft = TransactionDraft(
            direction = state.direction,
            accountId = state.accountId!!,
            categoryId = state.categoryId!!,
            amount = amount,
            occurredOn = state.occurredOn,
            description = state.description,
        )
        _uiState.update { it.copy(isSaving = true, operationError = null) }
        viewModelScope.launch {
            val editingId = state.transactionId
            val outcome = runCatchingNonCancellation {
                if (editingId == null) {
                    val id = submissionId ?: UUID.randomUUID().also { submissionId = it }
                    when (val result = transactions.create(id, draft)) {
                        is TransactionResult.Rejected ->
                            if (result.error == TransactionError.DUPLICATE_ID && transactions.observe(id).first() != null) {
                                TransactionResult.Saved(transactions.observe(id).first()!!)
                            } else result
                        else -> result
                    }
                } else {
                    transactions.update(editingId, draft)
                }
            }
            _uiState.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is TransactionResult.Saved -> current.copy(
                                isSaving = false,
                                completion = if (editingId == null) TransactionCompletion.Created else TransactionCompletion.Updated,
                            )
                            is TransactionResult.Rejected -> current.copy(isSaving = false).withError(result.error)
                        }
                    },
                    onFailure = { current.copy(isSaving = false, operationError = R.string.error_save) },
                )
            }
            refreshIfCancelledElsewhere(editingId, outcome.getOrNull())
        }
    }

    /** Se outro fluxo cancelou o lançamento, relê o registro para exibir o estado real (somente leitura). */
    private suspend fun refreshIfCancelledElsewhere(id: UUID?, result: TransactionResult?) {
        if (id == null || result !is TransactionResult.Rejected || result.error != TransactionError.CANCELLED) return
        val current = runCatchingNonCancellation { transactions.observe(id).first() }.getOrNull() ?: return
        _uiState.update { it.copy(cancelledAt = current.cancelledAt, updatedAt = current.updatedAt) }
    }

    /** Cancelamento preserva o lançamento no histórico (contrato); não é exclusão. */
    fun cancelTransaction() {
        val state = _uiState.value
        val id = state.transactionId ?: return
        if (state.isBusy || state.isReadOnly || state.completion != null) return
        _uiState.update { it.copy(isCancelling = true, operationError = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation { transactions.cancel(id) }
            _uiState.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is TransactionResult.Saved -> current.copy(
                                isCancelling = false,
                                cancelledAt = result.transaction.cancelledAt,
                                completion = TransactionCompletion.Cancelled,
                            )
                            is TransactionResult.Rejected -> current.copy(isCancelling = false).withError(result.error)
                        }
                    },
                    onFailure = { current.copy(isCancelling = false, operationError = R.string.error_cancel_transaction) },
                )
            }
            refreshIfCancelledElsewhere(id, outcome.getOrNull())
        }
    }

    private fun TransactionFormUiState.withError(error: TransactionError): TransactionFormUiState = when (error) {
        TransactionError.INVALID_AMOUNT -> copy(amountError = R.string.error_amount_positive)
        TransactionError.FUTURE_DATE -> copy(dateError = R.string.error_future_date)
        TransactionError.ACCOUNT_NOT_FOUND, TransactionError.ACCOUNT_ARCHIVED -> copy(accountError = R.string.error_account_archived)
        TransactionError.CATEGORY_NOT_FOUND, TransactionError.CATEGORY_ARCHIVED -> copy(categoryError = R.string.error_category_archived)
        TransactionError.CATEGORY_DIRECTION_MISMATCH -> copy(categoryError = R.string.error_category_direction)
        TransactionError.NOT_FOUND -> copy(notFound = true, operationError = R.string.error_transaction_not_found)
        TransactionError.CANCELLED -> copy(operationError = R.string.error_transaction_cancelled)
        TransactionError.DUPLICATE_ID -> copy(operationError = R.string.error_save)
        TransactionError.MANAGED_BY_SUBSCRIPTION -> copy(operationError = R.string.managed_subscription_note)
        TransactionError.MANAGED_BY_INSTALLMENT -> copy(operationError = R.string.managed_payment_note)
    }

    companion object {
        const val MAX_DESCRIPTION = 140

        @StringRes
        fun amountErrorFor(result: MoneyInput.Result): Int? = when (result) {
            is MoneyInput.Result.Valid -> if (result.money.cents > 0) null else R.string.error_amount_positive
            MoneyInput.Result.Empty -> R.string.error_amount_positive
            MoneyInput.Result.InvalidFormat -> R.string.error_amount_format
            MoneyInput.Result.TooManyDecimals -> R.string.error_amount_decimals
            MoneyInput.Result.SignNotAllowed -> R.string.error_amount_transaction_sign
            MoneyInput.Result.TooLarge -> R.string.error_amount_too_large
        }

        fun factory(
            transactions: TransactionSource,
            accounts: AccountRepository,
            categories: CategoryRepository,
            installmentOrigins: ((UUID) -> kotlinx.coroutines.flow.Flow<com.fluxa.personal.domain.model.InstallmentOrigin?>)? = null,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                val id = handle.get<String>(TransactionRoutes.ARG_ID)?.let(UUID::fromString)
                val direction = handle.get<String>(TransactionRoutes.ARG_DIRECTION)
                    ?.let { runCatching { TransactionDirection.valueOf(it) }.getOrNull() }
                    ?: TransactionDirection.EXPENSE
                TransactionFormViewModel(transactions, accounts, categories, id, direction, installmentOrigins = installmentOrigins)
            }
        }
    }
}
