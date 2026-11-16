package com.fluxa.personal.ui.feature.subscriptions

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
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.Subscription
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionError
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionOccurrence
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionResult
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionStatus
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.ui.common.MoneyInput
import com.fluxa.personal.ui.common.financialDays
import com.fluxa.personal.ui.common.runCatchingNonCancellation
import com.fluxa.personal.ui.feature.credit.CommandStatus
import com.fluxa.personal.ui.feature.credit.CreditSource
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// region Rotas e utilidades comuns

internal object SubscriptionRoutes {
    const val ARG_ID = "subscription"
    const val ARG_DATE = "date"
    const val LIST = "subscriptions"
    const val NEW = "subscriptions/new"
    const val AGENDA = "subscriptions/agenda"
    const val DETAIL = "subscriptions/{$ARG_ID}"
    const val EDIT = "subscriptions/{$ARG_ID}/edit"
    const val OCCURRENCE = "subscriptions/{$ARG_ID}/occurrences/{$ARG_DATE}"
    fun detail(id: UUID) = "subscriptions/$id"
    fun edit(id: UUID) = "subscriptions/$id/edit"
    fun occurrence(id: UUID, date: LocalDate) = "subscriptions/$id/occurrences/$date"
}

/** Fonte de pagamento para exibição. O nome vem das listas de contas/cartões (inclui arquivados, para o histórico). */
internal data class SourceLabel(val isCard: Boolean, val name: String?)

internal data class SourceNames(
    val accounts: Map<UUID, FinancialAccount> = emptyMap(),
    val cards: Map<UUID, CreditCardSummary> = emptyMap(),
) {
    fun label(source: SubscriptionSource): SourceLabel = when (source) {
        is SubscriptionSource.Account -> SourceLabel(false, accounts[source.id]?.details?.name)
        is SubscriptionSource.Card -> SourceLabel(true, cards[source.id]?.card?.details?.name)
    }
}

internal fun sourceNames(accounts: AccountRepository, credit: CreditSource): Flow<SourceNames> =
    combine(accounts.observeAll(includeArchived = true), credit.observeCards(includeArchived = true)) { a, c ->
        SourceNames(a.associateBy { it.id }, c.associateBy { it.card.id })
    }

internal val SubscriptionOccurrenceStatus.isOpen: Boolean
    get() = this == SubscriptionOccurrenceStatus.PENDING || this == SubscriptionOccurrenceStatus.OVERDUE

internal val SubscriptionOccurrenceStatus.isResolved: Boolean get() = !isOpen

/** Mensagens para erros tipados de assinatura; nunca mostra o código. */
@StringRes
internal fun subscriptionErrorMessage(error: SubscriptionError): Int = when (error) {
    SubscriptionError.NOT_FOUND -> R.string.error_subscription_not_found
    SubscriptionError.PAUSED -> R.string.error_subscription_paused
    SubscriptionError.CANCELLED -> R.string.error_subscription_cancelled
    SubscriptionError.INVALID_DETAILS -> R.string.error_subscription_details
    SubscriptionError.INVALID_ANCHOR -> R.string.error_subscription_anchor
    SubscriptionError.INVALID_CATEGORY -> R.string.error_subscription_category
    SubscriptionError.INVALID_SOURCE -> R.string.error_subscription_source
    SubscriptionError.INVALID_OCCURRENCE -> R.string.error_subscription_occurrence
    SubscriptionError.FUTURE_DATE -> R.string.error_subscription_future_date
    SubscriptionError.BEFORE_ACCOUNT_OPENING -> R.string.error_subscription_before_opening
    SubscriptionError.ALREADY_RESOLVED, SubscriptionError.SKIPPED -> R.string.feedback_occurrence_already_resolved
    SubscriptionError.INSUFFICIENT_CARD_LIMIT -> R.string.error_subscription_card_limit
    SubscriptionError.INVALID_CARD_SCHEDULE -> R.string.error_subscription_card_schedule
}

// endregion

// region Lista

internal data class SubscriptionListItem(val subscription: Subscription, val next: SubscriptionOccurrence?, val source: SourceLabel)

internal data class SubscriptionsUiState(
    val isLoading: Boolean = true,
    val active: List<SubscriptionListItem> = emptyList(),
    val paused: List<SubscriptionListItem> = emptyList(),
    val cancelled: List<SubscriptionListItem> = emptyList(),
    val overdueCount: Int = 0,
    @StringRes val errorMessage: Int? = null,
) {
    val isEmpty: Boolean get() = active.isEmpty() && paused.isEmpty() && cancelled.isEmpty()
}

/**
 * Lista de assinaturas com a próxima cobrança de cada uma (`observeNextOccurrence`, nada calculado aqui).
 * A observação é recriada quando o dia financeiro muda ([days]).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionsViewModel(
    source: SubscriptionDataSource,
    accounts: AccountRepository,
    credit: CreditSource,
    days: Flow<LocalDate> = financialDays(),
) : ViewModel() {
    val uiState: StateFlow<SubscriptionsUiState> = days.flatMapLatest {
        val withNext = source.observeSubscriptions().flatMapLatest { subs ->
            if (subs.isEmpty()) flowOf(emptyList())
            else combine(subs.map { s -> source.observeNextOccurrence(s.id).map { s to it } }) { it.toList() }
        }
        combine(withNext, source.observeOverdueOccurrences(), sourceNames(accounts, credit)) { list, overdue, names ->
            val items = list.map { (s, next) -> SubscriptionListItem(s, next, names.label(s.details.source)) }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.subscription.details.name })
            SubscriptionsUiState(
                isLoading = false,
                active = items.filter { it.subscription.status == SubscriptionStatus.ACTIVE },
                paused = items.filter { it.subscription.status == SubscriptionStatus.PAUSED },
                cancelled = items.filter { it.subscription.status == SubscriptionStatus.CANCELLED },
                overdueCount = overdue.size,
            )
        }
    }
        .catch { emit(SubscriptionsUiState(isLoading = false, errorMessage = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionsUiState())

    companion object {
        fun factory(source: SubscriptionDataSource, accounts: AccountRepository, credit: CreditSource): ViewModelProvider.Factory =
            viewModelFactory { initializer { SubscriptionsViewModel(source, accounts, credit) } }
    }
}

// endregion

// region Formulário

internal enum class SourceKind { ACCOUNT, CARD }

internal sealed interface SubscriptionCompletion {
    data class Created(val id: UUID) : SubscriptionCompletion
    data object Updated : SubscriptionCompletion
}

internal data class SubscriptionFormUiState(
    val subscriptionId: UUID? = null,
    val today: LocalDate,
    val name: String = "",
    val amountText: String = "",
    val categoryId: UUID? = null,
    val frequency: SubscriptionFrequency = SubscriptionFrequency.MONTHLY,
    val anchorDate: LocalDate,
    val billingDayText: String,
    val billingDayEdited: Boolean = false,
    val sourceKind: SourceKind = SourceKind.ACCOUNT,
    val accountId: UUID? = null,
    val cardId: UUID? = null,
    val categories: List<Category> = emptyList(),
    val accounts: List<FinancialAccount> = emptyList(),
    val cards: List<CreditCardSummary> = emptyList(),
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val status: SubscriptionStatus? = null,
    @StringRes val nameError: Int? = null,
    @StringRes val amountError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val billingDayError: Int? = null,
    @StringRes val sourceError: Int? = null,
    @StringRes val operationError: Int? = null,
    val isSaving: Boolean = false,
    val completion: SubscriptionCompletion? = null,
) {
    val isEditing: Boolean get() = subscriptionId != null
    val isReadOnly: Boolean get() = status == SubscriptionStatus.CANCELLED || notFound
    /** Frequência, âncora e dia ficam fixos depois de criados (contrato 5-A). */
    val scheduleLocked: Boolean get() = isEditing
    val firstError: Int? get() = operationError ?: nameError ?: amountError ?: categoryError ?: billingDayError ?: sourceError
}

/**
 * Cadastro/edição. A UI só coleta os dados: frequência Mensal/Anual, âncora e dia de cobrança vão para o contrato,
 * que gera o calendário. Fonte é exclusiva: a escolha de [SourceKind] decide se vai conta OU cartão.
 * Na edição, só nome, valor, categoria e fonte (o resto é fixo); valor e fonte valem para cobranças futuras.
 */
internal class SubscriptionFormViewModel(
    private val source: SubscriptionDataSource,
    categories: CategoryRepository,
    accounts: AccountRepository,
    credit: CreditSource,
    subscriptionId: UUID?,
    calendar: FinancialCalendar = FinancialCalendar(),
) : ViewModel() {
    private val today = calendar.today()
    private val _uiState = MutableStateFlow(
        SubscriptionFormUiState(
            subscriptionId = subscriptionId,
            today = today,
            anchorDate = today,
            billingDayText = today.dayOfMonth.toString(),
            isLoading = subscriptionId != null,
        ),
    )
    val uiState: StateFlow<SubscriptionFormUiState> = _uiState.asStateFlow()

    init {
        combine(
            categories.observeAll(includeArchived = false, kind = TransactionDirection.EXPENSE),
            accounts.observeAll(includeArchived = false),
            credit.observeCards(includeArchived = false),
        ) { c, a, k -> Triple(c, a, k) }
            .onEach { (c, a, k) ->
                _uiState.update { s ->
                    s.copy(
                        categories = c, accounts = a, cards = k, loaded = true,
                        accountId = s.accountId ?: a.singleOrNull()?.id.takeIf { !s.isEditing },
                        cardId = s.cardId ?: k.singleOrNull()?.card?.id.takeIf { !s.isEditing },
                    )
                }
            }
            .catch { _uiState.update { it.copy(loaded = true, operationError = R.string.error_load) } }
            .launchIn(viewModelScope)

        if (subscriptionId != null) {
            viewModelScope.launch {
                val loaded = runCatchingNonCancellation { source.observeSubscription(subscriptionId).first() }
                _uiState.update { s ->
                    val sub = loaded.getOrNull()
                    when {
                        loaded.isFailure -> s.copy(isLoading = false, operationError = R.string.error_load)
                        sub == null -> s.copy(isLoading = false, notFound = true)
                        else -> {
                            val d = sub.details
                            s.copy(
                                isLoading = false,
                                name = d.name,
                                amountText = MoneyInput.formatForEditing(d.amount),
                                categoryId = d.categoryId,
                                frequency = sub.frequency,
                                anchorDate = sub.anchorDate,
                                billingDayText = sub.billingDay.day.toString(),
                                billingDayEdited = true,
                                sourceKind = if (d.source is SubscriptionSource.Card) SourceKind.CARD else SourceKind.ACCOUNT,
                                accountId = (d.source as? SubscriptionSource.Account)?.id,
                                cardId = (d.source as? SubscriptionSource.Card)?.id,
                                status = sub.status,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun edit(block: (SubscriptionFormUiState) -> SubscriptionFormUiState) =
        _uiState.update { if (it.isSaving || it.isReadOnly) it else block(it) }

    fun onNameChange(v: String) = edit { it.copy(name = v.take(MAX_NAME), nameError = null) }
    fun onAmountChange(v: String) = edit { it.copy(amountText = MoneyInput.sanitizeTyping(v), amountError = null) }
    fun onCategoryChange(id: UUID) = edit { it.copy(categoryId = id, categoryError = null) }
    fun onFrequencyChange(f: SubscriptionFrequency) = edit { if (it.scheduleLocked) it else it.copy(frequency = f, billingDayError = null) }
    fun onAnchorChange(d: LocalDate) = edit {
        if (it.scheduleLocked) it
        else it.copy(anchorDate = d, billingDayText = if (it.billingDayEdited) it.billingDayText else d.dayOfMonth.toString(), billingDayError = null)
    }
    fun onBillingDayChange(v: String) = edit {
        if (it.scheduleLocked) it else it.copy(billingDayText = v.filter(Char::isDigit).take(2), billingDayEdited = true, billingDayError = null)
    }
    fun onSourceKindChange(k: SourceKind) = edit { it.copy(sourceKind = k, sourceError = null) }
    fun onAccountChange(id: UUID) = edit { it.copy(accountId = id, sourceError = null) }
    fun onCardChange(id: UUID) = edit { it.copy(cardId = id, sourceError = null) }
    fun dismissError() = _uiState.update { it.copy(operationError = null) }

    fun save() {
        val s = _uiState.value
        if (s.isSaving || s.isReadOnly || s.isLoading || s.completion != null) return
        val parsed = MoneyInput.parse(s.amountText)
        val amount = (parsed as? MoneyInput.Result.Valid)?.money?.takeIf { it.cents > 0 }
        // Anual: o dia é o da âncora (o contrato rejeita outro). Mensal: dia informado (1..31).
        val billingDay = if (s.frequency == SubscriptionFrequency.YEARLY) s.anchorDate.dayOfMonth
        else s.billingDayText.toIntOrNull()?.takeIf { it in 1..31 }
        val sourceValue: SubscriptionSource? = when (s.sourceKind) {
            SourceKind.ACCOUNT -> s.accountId?.takeIf { id -> s.accounts.any { it.id == id } }?.let { SubscriptionSource.Account(it) }
            SourceKind.CARD -> s.cardId?.takeIf { id -> s.cards.any { it.card.id == id } }?.let { SubscriptionSource.Card(it) }
        }
        val errors = s.copy(
            nameError = if (s.name.isBlank()) R.string.error_name_required else null,
            amountError = if (amount == null) amountError(parsed) else null,
            categoryError = if (s.categoryId == null || s.categories.none { it.id == s.categoryId }) R.string.error_subscription_category_required else null,
            billingDayError = if (billingDay == null) R.string.error_billing_day else null,
            sourceError = if (sourceValue == null) {
                if (s.sourceKind == SourceKind.ACCOUNT) R.string.error_subscription_account_required else R.string.error_subscription_card_required
            } else null,
            operationError = null,
        )
        if (errors.firstError != null) {
            _uiState.value = errors
            return
        }
        val details = SubscriptionDetails(s.name.trim(), amount!!, s.categoryId!!, sourceValue!!)
        _uiState.update { it.copy(isSaving = true, operationError = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation {
                val id = s.subscriptionId
                if (id == null) source.create(details, s.frequency, s.anchorDate, BillingDay(billingDay!!)) else source.update(id, details)
            }
            _uiState.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is SubscriptionResult.Saved -> current.copy(
                                isSaving = false,
                                completion = if (s.subscriptionId == null) SubscriptionCompletion.Created(result.value.id) else SubscriptionCompletion.Updated,
                            )
                            is SubscriptionResult.Rejected -> current.copy(isSaving = false).withError(result.error)
                        }
                    },
                    onFailure = { current.copy(isSaving = false, operationError = R.string.error_save) },
                )
            }
        }
    }

    private fun SubscriptionFormUiState.withError(error: SubscriptionError) = when (error) {
        SubscriptionError.INVALID_CATEGORY -> copy(categoryError = R.string.error_subscription_category)
        SubscriptionError.INVALID_SOURCE -> copy(sourceError = R.string.error_subscription_source)
        SubscriptionError.INVALID_ANCHOR -> copy(billingDayError = R.string.error_subscription_anchor)
        SubscriptionError.CANCELLED -> copy(status = SubscriptionStatus.CANCELLED, operationError = R.string.error_subscription_cancelled)
        SubscriptionError.NOT_FOUND -> copy(notFound = true, operationError = R.string.error_subscription_not_found)
        else -> copy(operationError = subscriptionErrorMessage(error))
    }

    private fun amountError(result: MoneyInput.Result): Int = when (result) {
        MoneyInput.Result.InvalidFormat -> R.string.error_amount_format
        MoneyInput.Result.TooManyDecimals -> R.string.error_amount_decimals
        MoneyInput.Result.TooLarge -> R.string.error_amount_too_large
        else -> R.string.error_amount_positive
    }

    companion object {
        const val MAX_NAME = 80

        fun factory(
            source: SubscriptionDataSource,
            categories: CategoryRepository,
            accounts: AccountRepository,
            credit: CreditSource,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val id = createSavedStateHandle().get<String>(SubscriptionRoutes.ARG_ID)?.let(UUID::fromString)
                SubscriptionFormViewModel(source, categories, accounts, credit, id)
            }
        }
    }
}

// endregion

// region Detalhe

internal enum class StatusAction { Pause, Resume, Cancel }

internal data class SubscriptionDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val subscription: Subscription? = null,
    val next: SubscriptionOccurrence? = null,
    val open: List<SubscriptionOccurrence> = emptyList(),
    val history: List<SubscriptionOccurrence> = emptyList(),
    val source: SourceLabel? = null,
    val category: Category? = null,
    val names: SourceNames = SourceNames(),
    val actionStatus: CommandStatus = CommandStatus.Idle,
    val lastAction: StatusAction? = null,
    @StringRes val actionError: Int? = null,
    val actionCount: Int = 0,
    @StringRes val loadError: Int? = null,
) {
    val status: SubscriptionStatus? get() = subscription?.status
    val canPause: Boolean get() = status == SubscriptionStatus.ACTIVE
    val canResume: Boolean get() = status == SubscriptionStatus.PAUSED
    val canCancel: Boolean get() = status != null && status != SubscriptionStatus.CANCELLED
    val canEdit: Boolean get() = canCancel
}

/**
 * Detalhe: cadastro atual, próxima cobrança (contrato), ocorrências em aberto e histórico de desfechos com os
 * termos gravados (snapshot) — lidos de `observeOccurrences` na janela [âncora, hoje + 1 ano] e filtrados por ID.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionDetailViewModel(
    private val source: SubscriptionDataSource,
    accounts: AccountRepository,
    credit: CreditSource,
    categories: CategoryRepository,
    private val id: UUID,
    days: Flow<LocalDate> = financialDays(),
) : ViewModel() {
    private data class Action(
        val status: CommandStatus = CommandStatus.Idle,
        val last: StatusAction? = null,
        @StringRes val error: Int? = null,
        val count: Int = 0,
    )

    private val action = MutableStateFlow(Action())

    val uiState: StateFlow<SubscriptionDetailUiState> = days.flatMapLatest { today ->
        val data = source.observeSubscription(id).flatMapLatest { sub ->
            if (sub == null) flowOf(Triple<Subscription?, SubscriptionOccurrence?, List<SubscriptionOccurrence>>(null, null, emptyList()))
            else combine(
                source.observeNextOccurrence(id),
                source.observeOccurrences(minOf(sub.anchorDate, today), today.plusYears(1).plusDays(1)),
            ) { next, all -> Triple(sub, next, all.filter { it.key.subscriptionId == id }) }
        }
        combine(data, sourceNames(accounts, credit), categories.observeAll(includeArchived = true), action) { (sub, next, mine), names, cats, act ->
            SubscriptionDetailUiState(
                isLoading = false,
                notFound = sub == null,
                subscription = sub,
                next = next,
                open = mine.filter { it.status == SubscriptionOccurrenceStatus.OVERDUE },
                history = mine.filter { it.status.isResolved }.sortedByDescending { it.key.occurrenceDate },
                source = sub?.let { names.label(it.details.source) },
                category = sub?.let { s -> cats.firstOrNull { it.id == s.details.categoryId } },
                names = names,
                actionStatus = act.status,
                lastAction = act.last,
                actionError = act.error,
                actionCount = act.count,
            )
        }
    }
        .catch { emit(SubscriptionDetailUiState(isLoading = false, loadError = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionDetailUiState())

    fun dismissError() = action.update { it.copy(status = CommandStatus.Idle, error = null) }

    fun pause() = run(StatusAction.Pause) { source.pause(id) }
    fun resume() = run(StatusAction.Resume) { source.resume(id) }
    fun cancel() = run(StatusAction.Cancel) { source.cancel(id) }

    private fun run(kind: StatusAction, command: suspend () -> SubscriptionResult<Subscription>) {
        val state = uiState.value
        val allowed = when (kind) {
            StatusAction.Pause -> state.canPause
            StatusAction.Resume -> state.canResume
            StatusAction.Cancel -> state.canCancel
        }
        if (action.value.status == CommandStatus.Submitting || !allowed) return
        action.update { it.copy(status = CommandStatus.Submitting, last = kind, error = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation { command() }
            action.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is SubscriptionResult.Saved -> current.copy(status = CommandStatus.Success, count = current.count + 1)
                            is SubscriptionResult.Rejected -> current.copy(status = CommandStatus.Error, error = subscriptionErrorMessage(result.error))
                        }
                    },
                    onFailure = { current.copy(status = CommandStatus.Error, error = R.string.error_subscription_action) },
                )
            }
        }
    }

    companion object {
        fun factory(source: SubscriptionDataSource, accounts: AccountRepository, credit: CreditSource, categories: CategoryRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val id = UUID.fromString(requireNotNull(createSavedStateHandle().get<String>(SubscriptionRoutes.ARG_ID)))
                    SubscriptionDetailViewModel(source, accounts, credit, categories, id)
                }
            }
    }
}

// endregion

// region Agenda

internal data class AgendaDay(val date: LocalDate, val items: List<SubscriptionOccurrence>)

internal data class SubscriptionAgendaUiState(
    val isLoading: Boolean = true,
    val today: LocalDate? = null,
    val overdue: List<AgendaDay> = emptyList(),
    val upcoming: List<AgendaDay> = emptyList(),
    val resolved: List<AgendaDay> = emptyList(),
    val names: SourceNames = SourceNames(),
    @StringRes val errorMessage: Int? = null,
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && upcoming.isEmpty() && resolved.isEmpty()
}

/**
 * Agenda: vencidas (`observeOverdueOccurrences`), próximos 30 dias (`observeUpcomingOccurrences(30)`) e resolvidas
 * nos últimos/próximos 30 dias (`observeOccurrences`, só desfechos). Agrupada por data; nada é gerado aqui.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionAgendaViewModel(
    source: SubscriptionDataSource,
    accounts: AccountRepository,
    credit: CreditSource,
    days: Flow<LocalDate> = financialDays(),
) : ViewModel() {
    val uiState: StateFlow<SubscriptionAgendaUiState> = days.flatMapLatest { today ->
        combine(
            source.observeOverdueOccurrences(),
            source.observeUpcomingOccurrences(UPCOMING_DAYS),
            source.observeOccurrences(today.minusDays(RESOLVED_DAYS), today.plusDays(RESOLVED_DAYS + 1)),
            sourceNames(accounts, credit),
        ) { overdue, upcoming, window, names ->
            SubscriptionAgendaUiState(
                isLoading = false,
                today = today,
                overdue = overdue.byDay(),
                upcoming = upcoming.byDay(),
                resolved = window.filter { it.status.isResolved }.byDay().reversed(),
                names = names,
            )
        }
    }
        .catch { emit(SubscriptionAgendaUiState(isLoading = false, errorMessage = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubscriptionAgendaUiState())

    private fun List<SubscriptionOccurrence>.byDay() =
        groupBy { it.key.occurrenceDate }.toSortedMap().map { (date, items) -> AgendaDay(date, items) }

    companion object {
        const val UPCOMING_DAYS = 30L
        const val RESOLVED_DAYS = 30L

        fun factory(source: SubscriptionDataSource, accounts: AccountRepository, credit: CreditSource): ViewModelProvider.Factory =
            viewModelFactory { initializer { SubscriptionAgendaViewModel(source, accounts, credit) } }
    }
}

// endregion

// region Ocorrência (pagar, lançar no cartão, pular)

internal enum class OccurrenceOutcome { Paid, Charged, Skipped, AlreadyResolved }

internal data class OccurrenceUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val today: LocalDate,
    val occurrence: SubscriptionOccurrence? = null,
    val subscription: Subscription? = null,
    val source: SourceLabel? = null,
    val paidOn: LocalDate,
    val status: CommandStatus = CommandStatus.Idle,
    @StringRes val error: Int? = null,
    @StringRes val dateError: Int? = null,
    val outcome: OccurrenceOutcome? = null,
    val completedCount: Int = 0,
) {
    private val open: Boolean get() = occurrence?.status?.isOpen == true
    val isCard: Boolean get() = occurrence?.details?.source is SubscriptionSource.Card
    val isFuture: Boolean get() = occurrence != null && occurrence.key.occurrenceDate > today
    /** Pagar/lançar: ocorrência em aberto, não futura e assinatura ativa (o backend continua decidindo). */
    val canSettle: Boolean get() = open && !isFuture && subscription?.status == SubscriptionStatus.ACTIVE
    val canSkip: Boolean get() = open
    val isSubmitting: Boolean get() = status == CommandStatus.Submitting
}

/**
 * Uma ocorrência (`observeOccurrence(id, data)`): os termos exibidos e usados são os da ocorrência. Pagar envia só
 * a identidade e a data da baixa; valor, categoria e fonte são decididos pelo backend. `ALREADY_RESOLVED`/`SKIPPED`
 * são tratados como estado atualizado (idempotente), não como erro.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionOccurrenceViewModel(
    private val source: SubscriptionDataSource,
    accounts: AccountRepository,
    credit: CreditSource,
    private val subscriptionId: UUID,
    private val date: LocalDate,
    calendar: FinancialCalendar = FinancialCalendar(),
    days: Flow<LocalDate> = financialDays(calendar),
) : ViewModel() {
    private val form = MutableStateFlow(
        calendar.today().let { OccurrenceUiState(today = it, paidOn = it) }, // baixa hoje por padrão; pode ser anterior
    )

    val uiState: StateFlow<OccurrenceUiState> = combine(
        days.flatMapLatest { today ->
            combine(source.observeOccurrence(subscriptionId, date), source.observeSubscription(subscriptionId)) { occ, sub -> Triple(today, occ, sub) }
        },
        sourceNames(accounts, credit),
        form,
    ) { (today, occ, sub), names, f ->
        f.copy(
            isLoading = false,
            notFound = occ == null,
            today = today,
            occurrence = occ,
            subscription = sub,
            source = occ?.let { names.label(it.details.source) },
        )
    }
        .catch { emit(form.value.copy(isLoading = false, error = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    fun onPaidOnChange(value: LocalDate) = form.update { if (it.isSubmitting) it else it.copy(paidOn = value, dateError = null) }
    fun dismissError() = form.update { it.copy(status = CommandStatus.Idle, error = null) }

    /** Pagar em conta ou lançar no cartão, conforme a fonte gravada na ocorrência. */
    fun settle() {
        val state = uiState.value
        // `form` é atualizado na hora; o `uiState` combinado só reflete o envio depois (toque duplo).
        if (form.value.isSubmitting || !state.canSettle) return
        val paidOn = form.value.paidOn // escolha mais recente, mesmo antes de o estado combinado emitir
        if (paidOn > state.today) {
            form.update { it.copy(dateError = R.string.error_subscription_future_date) }
            return
        }
        command(if (state.isCard) OccurrenceOutcome.Charged else OccurrenceOutcome.Paid) { source.pay(subscriptionId, date, paidOn) }
    }

    fun skip() {
        val state = uiState.value
        if (form.value.isSubmitting || !state.canSkip) return
        command(OccurrenceOutcome.Skipped) { source.skip(subscriptionId, date) }
    }

    private fun command(success: OccurrenceOutcome, block: suspend () -> SubscriptionResult<SubscriptionOccurrence>) {
        form.update { it.copy(status = CommandStatus.Submitting, error = null, dateError = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation { block() }
            form.update { current ->
                outcome.fold(
                    onSuccess = { result ->
                        when (result) {
                            is SubscriptionResult.Saved -> current.copy(
                                status = CommandStatus.Success, outcome = success, completedCount = current.completedCount + 1,
                            )
                            is SubscriptionResult.Rejected -> when (result.error) {
                                // Outra tentativa já resolveu: o Flow mostra o desfecho real.
                                SubscriptionError.ALREADY_RESOLVED, SubscriptionError.SKIPPED -> current.copy(
                                    status = CommandStatus.Success, outcome = OccurrenceOutcome.AlreadyResolved,
                                    completedCount = current.completedCount + 1,
                                )
                                SubscriptionError.FUTURE_DATE, SubscriptionError.BEFORE_ACCOUNT_OPENING ->
                                    current.copy(status = CommandStatus.Error, dateError = subscriptionErrorMessage(result.error))
                                else -> current.copy(status = CommandStatus.Error, error = subscriptionErrorMessage(result.error))
                            }
                        }
                    },
                    onFailure = { current.copy(status = CommandStatus.Error, error = R.string.error_subscription_action) },
                )
            }
        }
    }

    companion object {
        fun factory(source: SubscriptionDataSource, accounts: AccountRepository, credit: CreditSource): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val handle = createSavedStateHandle()
                    SubscriptionOccurrenceViewModel(
                        source, accounts, credit,
                        UUID.fromString(requireNotNull(handle.get<String>(SubscriptionRoutes.ARG_ID))),
                        LocalDate.parse(requireNotNull(handle.get<String>(SubscriptionRoutes.ARG_DATE))),
                    )
                }
            }
    }
}

// endregion
