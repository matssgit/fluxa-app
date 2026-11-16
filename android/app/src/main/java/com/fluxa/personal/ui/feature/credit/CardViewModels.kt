package com.fluxa.personal.ui.feature.credit

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.ui.common.MoneyInput
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class CreditCompletion { Saved, Archived, Cancelled }

// region Lista de cartões

internal data class CardsUiState(
    val isLoading: Boolean = true,
    val active: List<CreditCardSummary> = emptyList(),
    val archived: List<CreditCardSummary> = emptyList(),
    val showArchived: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    val isEmpty: Boolean get() = !isLoading && active.isEmpty() && archived.isEmpty()
}

/** Cartões com limite total, comprometido e disponível vindos de `observeCards` (nada calculado na UI). */
internal class CardsViewModel(credit: CreditSource) : ViewModel() {
    private val showArchived = MutableStateFlow(false)

    val uiState: StateFlow<CardsUiState> = combine(
        credit.observeCards(includeArchived = true)
            .map<List<CreditCardSummary>, List<CreditCardSummary>?> { it }
            .catch { emit(null) },
        showArchived,
    ) { cards, show ->
        if (cards == null) {
            CardsUiState(isLoading = false, showArchived = show, errorMessage = R.string.error_load)
        } else {
            CardsUiState(
                isLoading = false,
                active = cards.filter { it.card.archivedAt == null },
                archived = cards.filter { it.card.archivedAt != null },
                showArchived = show,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardsUiState())

    fun setShowArchived(value: Boolean) = showArchived.update { value }

    companion object {
        fun factory(credit: CreditSource): ViewModelProvider.Factory = viewModelFactory { initializer { CardsViewModel(credit) } }
    }
}

// endregion

// region Formulário de cartão

internal data class CardFormUiState(
    val cardId: UUID? = null,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val name: String = "",
    val limitText: String = "",
    val closingDayText: String = "",
    val dueDayText: String = "",
    val color: String? = null,
    val isArchived: Boolean = false,
    @StringRes val nameError: Int? = null,
    @StringRes val limitError: Int? = null,
    @StringRes val closingDayError: Int? = null,
    @StringRes val dueDayError: Int? = null,
    @StringRes val operationError: Int? = null,
    val isSaving: Boolean = false,
    val isArchiving: Boolean = false,
    val completion: CreditCompletion? = null,
) {
    val isEditing: Boolean get() = cardId != null
    val isBusy: Boolean get() = isLoading || isSaving || isArchiving
}

/**
 * Cadastro/edição de cartão. A UI só coleta e valida o formato (nome preenchido, valor BRL, dia 1–31);
 * regras como "limite abaixo do comprometido" são decididas pelo repositório e exibidas como erro, sem correção
 * silenciosa. Nenhum vencimento é calculado aqui.
 */
internal class CardFormViewModel(private val credit: CreditSource, cardId: UUID?) : ViewModel() {
    private val _uiState = MutableStateFlow(CardFormUiState(cardId = cardId, isLoading = cardId != null))
    val uiState: StateFlow<CardFormUiState> = _uiState.asStateFlow()

    init {
        if (cardId != null) {
            viewModelScope.launch {
                val loaded = runCatchingNonCancellation { credit.observeCard(cardId).first() }
                _uiState.update { state ->
                    loaded.fold(
                        onSuccess = { summary ->
                            if (summary == null) state.copy(isLoading = false, notFound = true)
                            else {
                                val d = summary.card.details
                                state.copy(
                                    isLoading = false,
                                    name = d.name,
                                    limitText = MoneyInput.formatForEditing(d.totalLimit),
                                    closingDayText = d.closingDay.day.toString(),
                                    dueDayText = d.dueDay.day.toString(),
                                    color = d.color,
                                    isArchived = summary.card.archivedAt != null,
                                )
                            }
                        },
                        onFailure = { state.copy(isLoading = false, operationError = R.string.error_load) },
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onLimitChange(value: String) = _uiState.update { it.copy(limitText = MoneyInput.sanitizeTyping(value), limitError = null) }
    fun onClosingDayChange(value: String) = _uiState.update { it.copy(closingDayText = value.filter(Char::isDigit).take(2), closingDayError = null) }
    fun onDueDayChange(value: String) = _uiState.update { it.copy(dueDayText = value.filter(Char::isDigit).take(2), dueDayError = null) }
    fun onColorChange(value: String?) = _uiState.update { it.copy(color = value) }
    fun dismissError() = _uiState.update { it.copy(operationError = null) }

    fun save() {
        val state = _uiState.value
        if (state.isBusy || state.completion != null || state.notFound) return
        val limit = (MoneyInput.parse(state.limitText) as? MoneyInput.Result.Valid)?.money
        val limitError = when (MoneyInput.parse(state.limitText)) {
            is MoneyInput.Result.Valid -> null
            MoneyInput.Result.Empty -> R.string.error_card_limit_required
            MoneyInput.Result.TooManyDecimals -> R.string.error_amount_decimals
            MoneyInput.Result.TooLarge -> R.string.error_amount_too_large
            else -> R.string.error_amount_format
        }
        val closing = parseDay(state.closingDayText)
        val due = parseDay(state.dueDayText)
        val nameError = if (state.name.isBlank()) R.string.error_name_required else null
        if (nameError != null || limitError != null || closing == null || due == null || limit == null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    limitError = limitError,
                    closingDayError = if (closing == null) R.string.error_billing_day else null,
                    dueDayError = if (due == null) R.string.error_billing_day else null,
                )
            }
            return
        }
        val details = CreditCardDetails(state.name, limit, closing, due, state.color)
        _uiState.update { it.copy(isSaving = true, operationError = null) }
        viewModelScope.launch {
            val id = state.cardId
            val outcome = runCatchingNonCancellation { if (id == null) credit.createCard(details) else credit.updateCard(id, details) }
            _uiState.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is CreditResult.Saved -> current.copy(isSaving = false, completion = CreditCompletion.Saved)
                            is CreditResult.Rejected -> current.copy(isSaving = false).withError(result.error)
                        }
                    },
                    onFailure = { current.copy(isSaving = false, operationError = R.string.error_save) },
                )
            }
        }
    }

    /** Arquiva: o cartão sai das novas compras e o histórico é preservado (contrato). */
    fun archive() {
        val state = _uiState.value
        val id = state.cardId ?: return
        if (state.isBusy || state.isArchived || state.completion != null) return
        _uiState.update { it.copy(isArchiving = true, operationError = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation { credit.archiveCard(id) }
            _uiState.update { current ->
                outcome.fold(
                    onSuccess = { changed ->
                        if (changed) current.copy(isArchiving = false, isArchived = true, completion = CreditCompletion.Archived)
                        else current.copy(isArchiving = false, operationError = R.string.error_already_archived)
                    },
                    onFailure = { current.copy(isArchiving = false, operationError = R.string.error_archive) },
                )
            }
        }
    }

    private fun CardFormUiState.withError(error: CreditError): CardFormUiState = when (error) {
        CreditError.INVALID_NAME -> copy(nameError = R.string.error_name_required)
        CreditError.INVALID_LIMIT -> copy(limitError = R.string.error_amount_format)
        CreditError.LIMIT_BELOW_COMMITTED -> copy(limitError = R.string.error_limit_below_committed)
        CreditError.CARD_NOT_FOUND -> copy(notFound = true, operationError = R.string.error_card_not_found)
        CreditError.INVALID_SCHEDULE_DATE -> copy(dueDayError = R.string.error_billing_day)
        else -> copy(operationError = R.string.error_save)
    }

    companion object {
        fun parseDay(text: String): BillingDay? = text.toIntOrNull()?.takeIf { it in 1..31 }?.let(::BillingDay)

        fun factory(credit: CreditSource): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val id = createSavedStateHandle().get<String>(CreditRoutes.ARG_CARD)?.let(UUID::fromString)
                CardFormViewModel(credit, id)
            }
        }
    }
}

// endregion

// region Detalhe do cartão

internal data class PurchaseItem(
    val purchase: Purchase,
    val category: Category?,
    val installments: List<Installment>,
)

/** Parcela pendente com a compra de origem, para a lista "Próximas parcelas". */
internal data class UpcomingInstallment(val installment: Installment, val purchase: Purchase, val count: Int)

internal data class CardDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val summary: CreditCardSummary? = null,
    val purchases: List<PurchaseItem> = emptyList(),
    val upcoming: List<UpcomingInstallment> = emptyList(),
    @StringRes val errorMessage: Int? = null,
)

private data class PurchasesWithInstallments(val purchases: List<Purchase>, val installments: Map<UUID, List<Installment>>)

/**
 * Detalhe do cartão: resumo de limite (`observeCard`), compras (`observePurchases`, ordem do repositório) e as
 * parcelas de cada compra (`observeInstallments`). "Próximas parcelas" apenas reúne e ordena por vencimento as
 * parcelas pendentes já persistidas; nenhum valor ou data é calculado aqui.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CardDetailViewModel(
    credit: CreditSource,
    categories: CategoryRepository,
    cardId: UUID,
) : ViewModel() {

    private val purchases: Flow<PurchasesWithInstallments> = credit.observePurchases(cardId).flatMapLatest { list ->
        if (list.isEmpty()) flowOf(PurchasesWithInstallments(emptyList(), emptyMap()))
        else combine(list.map { p -> credit.observeInstallments(p.id).map { p.id to it } }) { pairs ->
            PurchasesWithInstallments(list, pairs.toMap())
        }
    }

    val uiState: StateFlow<CardDetailUiState> = combine(
        credit.observeCard(cardId),
        purchases,
        categories.observeAll(includeArchived = true),
    ) { summary, data, categoryList ->
        val byCategory = categoryList.associateBy { it.id }
        val items = data.purchases.map { PurchaseItem(it, byCategory[it.details.categoryId], data.installments[it.id].orEmpty()) }
        CardDetailUiState(
            isLoading = false,
            notFound = summary == null,
            summary = summary,
            purchases = items,
            upcoming = items
                .flatMap { item ->
                    item.installments.filter { it.status == InstallmentStatus.PENDING }
                        .map { UpcomingInstallment(it, item.purchase, item.installments.size) }
                }
                .sortedWith(compareBy<UpcomingInstallment> { it.installment.dueDate }.thenBy { it.installment.number }),
        )
    }
        .catch { emit(CardDetailUiState(isLoading = false, errorMessage = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardDetailUiState())

    companion object {
        fun factory(credit: CreditSource, categories: CategoryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val id = UUID.fromString(requireNotNull(createSavedStateHandle().get<String>(CreditRoutes.ARG_CARD)))
                CardDetailViewModel(credit, categories, id)
            }
        }
    }
}

// endregion

/** Hoje no fuso financeiro do aparelho (mesma abstração do backend). */
internal fun creditToday(calendar: FinancialCalendar = FinancialCalendar()): LocalDate = calendar.today()
