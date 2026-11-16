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
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.ui.common.MoneyInput
import com.fluxa.personal.ui.common.runCatchingNonCancellation
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// region Formulário de compra

/** Compra criada: o formulário abre o detalhe com as parcelas e vencimentos persistidos. */
internal data class PurchaseCreated(val cardId: UUID, val purchaseId: UUID)

internal data class PurchaseFormUiState(
    val cards: List<CreditCardSummary> = emptyList(),
    val categories: List<Category> = emptyList(),
    val cardsLoaded: Boolean = false,
    val categoriesLoaded: Boolean = false,
    val cardId: UUID? = null,
    val categoryId: UUID? = null,
    val amountText: String = "",
    val title: String = "",
    val merchant: String = "",
    val notes: String = "",
    val purchaseDate: LocalDate,
    val installmentsText: String = "1",
    @StringRes val cardError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val amountError: Int? = null,
    @StringRes val titleError: Int? = null,
    @StringRes val installmentsError: Int? = null,
    @StringRes val dateError: Int? = null,
    @StringRes val operationError: Int? = null,
    val isSaving: Boolean = false,
    val created: PurchaseCreated? = null,
) {
    val selectedCard: CreditCardSummary? get() = cards.firstOrNull { it.card.id == cardId }
    /** Primeiro erro na ordem do formulário: exibido também junto ao botão Salvar (o campo pode estar fora da tela). */
    @get:StringRes
    val firstError: Int? get() = listOfNotNull(operationError, cardError, amountError, installmentsError, categoryError, titleError, dateError).firstOrNull()

    /** Quantidade informada, só para o texto "N parcelas" (valores e datas vêm do backend após criar). */
    val installmentCount: Int? get() = installmentsText.toIntOrNull()?.takeIf { it >= 1 }
}

/**
 * Nova compra no cartão. Cartões ativos vêm de `observeCards()`; categorias de `observeAll(kind = EXPENSE)`.
 * Sem preview de parcelas ou vencimentos: o contrato não expõe e a UI não reimplementa `InstallmentSplitter`
 * nem o calendário de fechamento. O UUID do envio é reutilizado em novas tentativas (contrato 4-A).
 */
internal class PurchaseFormViewModel(
    private val credit: CreditSource,
    categories: CategoryRepository,
    initialCardId: UUID?,
    calendar: FinancialCalendar = FinancialCalendar(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(PurchaseFormUiState(cardId = initialCardId, purchaseDate = calendar.today()))
    val uiState: StateFlow<PurchaseFormUiState> = _uiState.asStateFlow()
    private var submissionId: UUID? = null

    init {
        credit.observeCards(includeArchived = false)
            .onEach { cards ->
                _uiState.update { state ->
                    val autoSelect = state.cardId == null && cards.size == 1
                    state.copy(cards = cards, cardsLoaded = true, cardId = if (autoSelect) cards.single().card.id else state.cardId)
                }
            }
            .catch { _uiState.update { it.copy(cardsLoaded = true, operationError = R.string.error_load) } }
            .launchIn(viewModelScope)
        categories.observeAll(includeArchived = false, kind = TransactionDirection.EXPENSE)
            .onEach { list -> _uiState.update { it.copy(categories = list, categoriesLoaded = true) } }
            .catch { _uiState.update { it.copy(categoriesLoaded = true, operationError = R.string.error_load) } }
            .launchIn(viewModelScope)
    }

    fun onCardChange(id: UUID) = _uiState.update { it.copy(cardId = id, cardError = null, amountError = null) }
    fun onCategoryChange(id: UUID) = _uiState.update { it.copy(categoryId = id, categoryError = null) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = MoneyInput.sanitizeTyping(value), amountError = null) }
    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value.take(MAX_TEXT), titleError = null) }
    fun onMerchantChange(value: String) = _uiState.update { it.copy(merchant = value.take(MAX_TEXT)) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value.take(MAX_NOTES)) }
    fun onDateChange(value: LocalDate) = _uiState.update { it.copy(purchaseDate = value, dateError = null) }
    fun onInstallmentsChange(value: String) = _uiState.update { it.copy(installmentsText = value.filter(Char::isDigit).take(3), installmentsError = null) }
    fun dismissError() = _uiState.update { it.copy(operationError = null) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.created != null) return
        val parsed = MoneyInput.parse(state.amountText)
        val amount = (parsed as? MoneyInput.Result.Valid)?.money?.takeIf { it.cents > 0 }
        val amountError = when (parsed) {
            is MoneyInput.Result.Valid -> if (parsed.money.cents > 0) null else R.string.error_amount_positive
            MoneyInput.Result.Empty -> R.string.error_amount_positive
            MoneyInput.Result.TooManyDecimals -> R.string.error_amount_decimals
            MoneyInput.Result.TooLarge -> R.string.error_amount_too_large
            else -> R.string.error_amount_format
        }
        val errors = state.copy(
            cardError = if (state.cardId == null) R.string.error_card_required else null,
            categoryError = if (state.categoryId == null) R.string.error_category_required else null,
            amountError = amountError,
            titleError = if (state.title.isBlank()) R.string.error_title_required else null,
            installmentsError = if (state.installmentCount == null) R.string.error_installments_invalid else null,
        )
        if (amount == null || listOf(errors.cardError, errors.categoryError, errors.amountError, errors.titleError, errors.installmentsError).any { it != null }) {
            _uiState.value = errors
            return
        }
        val draft = PurchaseDraft(
            cardId = state.cardId!!,
            categoryId = state.categoryId!!,
            title = state.title,
            total = amount,
            installmentCount = state.installmentCount!!,
            purchaseDate = state.purchaseDate,
            merchant = state.merchant,
            notes = state.notes,
        )
        _uiState.update { it.copy(isSaving = true, operationError = null) }
        viewModelScope.launch {
            val id = submissionId ?: UUID.randomUUID().also { submissionId = it }
            val outcome = runCatchingNonCancellation {
                when (val result = credit.createPurchase(id, draft)) {
                    is CreditResult.Rejected ->
                        if (result.error == CreditError.DUPLICATE_PURCHASE_ID && credit.observePurchases(draft.cardId).first().any { it.id == id }) {
                            // Resposta do primeiro envio perdida: a compra com este UUID já existe.
                            CreditResult.Saved(Unit)
                        } else result
                    is CreditResult.Saved -> CreditResult.Saved(Unit)
                }
            }
            _uiState.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is CreditResult.Saved -> current.copy(isSaving = false, created = PurchaseCreated(draft.cardId, id))
                            is CreditResult.Rejected -> current.copy(isSaving = false).withError(result.error)
                        }
                    },
                    onFailure = { current.copy(isSaving = false, operationError = R.string.error_save) },
                )
            }
        }
    }

    private fun PurchaseFormUiState.withError(error: CreditError): PurchaseFormUiState = when (error) {
        CreditError.INSUFFICIENT_LIMIT -> copy(amountError = R.string.error_insufficient_limit)
        CreditError.INVALID_AMOUNT -> copy(amountError = R.string.error_amount_positive)
        CreditError.INVALID_INSTALLMENT_COUNT -> copy(installmentsError = R.string.error_installments_invalid)
        CreditError.INVALID_TITLE -> copy(titleError = R.string.error_title_required)
        CreditError.CARD_ARCHIVED -> copy(cardError = R.string.error_card_archived)
        CreditError.CARD_NOT_FOUND -> copy(cardError = R.string.error_card_not_found)
        CreditError.CATEGORY_NOT_FOUND, CreditError.CATEGORY_ARCHIVED, CreditError.CATEGORY_NOT_EXPENSE ->
            copy(categoryError = R.string.error_purchase_category)
        CreditError.INVALID_SCHEDULE_DATE -> copy(dateError = R.string.error_schedule_date)
        else -> copy(operationError = R.string.error_save)
    }

    companion object {
        const val MAX_TEXT = 80
        const val MAX_NOTES = 280

        fun factory(credit: CreditSource, categories: CategoryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val cardId = createSavedStateHandle().get<String>(CreditRoutes.ARG_CARD)?.let(UUID::fromString)
                PurchaseFormViewModel(credit, categories, cardId)
            }
        }
    }
}

// endregion

// region Detalhe da compra

internal data class PurchaseDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val purchase: Purchase? = null,
    val category: Category? = null,
    val card: CreditCardSummary? = null,
    val installments: List<Installment> = emptyList(),
    val isCancelling: Boolean = false,
    @StringRes val operationError: Int? = null,
    val cancelled: Boolean = false,
) {
    val isCancelled: Boolean get() = purchase?.cancelledAt != null
    /** Só oferece o cancelamento quando o contrato o aceitaria: compra ativa e nenhuma parcela paga. */
    val canCancel: Boolean get() = purchase != null && purchase.subscriptionOccurrence == null &&
        !isCancelled && installments.none { it.status == InstallmentStatus.PAID }
}

/**
 * Detalhe da compra com o cronograma persistido (`observeInstallments`). A compra é localizada pelo UUID dentro de
 * `observePurchases(cardId)` (não há consulta por ID no contrato 4-A). Cancelar usa `cancelPurchase`.
 */
internal class PurchaseDetailViewModel(
    private val credit: CreditSource,
    categories: CategoryRepository,
    cardId: UUID,
    private val purchaseId: UUID,
) : ViewModel() {
    private val action = MutableStateFlow(Action())

    private data class Action(val cancelling: Boolean = false, @StringRes val error: Int? = null, val cancelled: Boolean = false)

    val uiState: StateFlow<PurchaseDetailUiState> = combine(
        credit.observePurchases(cardId),
        credit.observeInstallments(purchaseId),
        credit.observeCard(cardId),
        categories.observeAll(includeArchived = true),
        action,
    ) { purchases, installments, card, categoryList, act ->
        val purchase = purchases.firstOrNull { it.id == purchaseId }
        PurchaseDetailUiState(
            isLoading = false,
            notFound = purchase == null,
            purchase = purchase,
            category = purchase?.let { p -> categoryList.firstOrNull { it.id == p.details.categoryId } },
            card = card,
            installments = installments,
            isCancelling = act.cancelling,
            operationError = act.error,
            cancelled = act.cancelled,
        )
    }
        .catch { emit(PurchaseDetailUiState(isLoading = false, operationError = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchaseDetailUiState())

    fun dismissError() = action.update { it.copy(error = null) }

    fun cancelPurchase() {
        if (action.value.cancelling || action.value.cancelled || !uiState.value.canCancel) return
        action.update { it.copy(cancelling = true, error = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation { credit.cancelPurchase(purchaseId) }
            action.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is CreditResult.Saved -> current.copy(cancelling = false, cancelled = true)
                            is CreditResult.Rejected -> current.copy(
                                cancelling = false,
                                error = when (result.error) {
                                    CreditError.HAS_PAID_INSTALLMENT -> R.string.error_has_paid_installment
                                    CreditError.PURCHASE_CANCELLED -> R.string.error_purchase_already_cancelled
                                    CreditError.PURCHASE_NOT_FOUND -> R.string.error_purchase_not_found
                                    else -> R.string.error_cancel_purchase
                                },
                            )
                        }
                    },
                    onFailure = { current.copy(cancelling = false, error = R.string.error_cancel_purchase) },
                )
            }
        }
    }

    companion object {
        fun factory(credit: CreditSource, categories: CategoryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                CreditRoutes.run {
                    PurchaseDetailViewModel(
                        credit,
                        categories,
                        UUID.fromString(requireNotNull(handle.get<String>(ARG_CARD))),
                        UUID.fromString(requireNotNull(handle.get<String>(ARG_PURCHASE))),
                    )
                }
            }
        }
    }
}

// endregion
