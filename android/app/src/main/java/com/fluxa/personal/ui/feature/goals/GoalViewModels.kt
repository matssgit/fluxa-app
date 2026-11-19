package com.fluxa.personal.ui.feature.goals

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.domain.model.AccountReserveSummary
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalError
import com.fluxa.personal.domain.model.GoalMovement
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.GoalStatus
import com.fluxa.personal.domain.model.GoalSummary
import com.fluxa.personal.ui.common.MoneyInput
import com.fluxa.personal.ui.common.runCatchingNonCancellation
import com.fluxa.personal.ui.feature.credit.CommandStatus
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

// region Rotas e utilidades

internal object GoalRoutes {
    const val ARG_ID = "goal"
    const val LIST = "goals"
    const val NEW = "goals/new"
    const val DETAIL = "goals/{$ARG_ID}"
    const val EDIT = "goals/{$ARG_ID}/edit"
    fun detail(id: UUID) = "goals/$id"
    fun edit(id: UUID) = "goals/$id/edit"
}

/** Percentual para exibição a partir dos pontos-base do contrato (0..10.000, já arredondados para baixo). */
internal val GoalSummary.percent: Int get() = progressBasisPoints / 100


@StringRes
internal fun goalErrorMessage(error: GoalError): Int = when (error) {
    GoalError.NOT_FOUND -> R.string.error_goal_not_found
    GoalError.ARCHIVED -> R.string.error_goal_archived
    GoalError.COMPLETED -> R.string.error_goal_completed
    GoalError.INVALID_NAME -> R.string.error_name_required
    GoalError.INVALID_TARGET -> R.string.error_goal_target
    GoalError.INVALID_AMOUNT -> R.string.error_amount_positive
    GoalError.ACCOUNT_NOT_FOUND, GoalError.ACCOUNT_ARCHIVED -> R.string.error_goal_account
    GoalError.ACCOUNT_CHANGE_NOT_ALLOWED -> R.string.error_goal_account_change
    GoalError.TARGET_BELOW_RESERVED -> R.string.error_goal_target_below_reserved
    GoalError.ALLOCATION_EXCEEDS_TARGET -> R.string.error_goal_exceeds_target
    GoalError.INSUFFICIENT_AVAILABLE_FUNDS -> R.string.error_goal_insufficient_available
    GoalError.RELEASE_EXCEEDS_RESERVED -> R.string.error_goal_release_exceeds
    GoalError.TARGET_NOT_REACHED -> R.string.error_goal_target_not_reached
    GoalError.HAS_RESERVED_FUNDS -> R.string.error_goal_has_reserved
}

// endregion

// region Lista

internal data class GoalsUiState(
    val isLoading: Boolean = true,
    val active: List<GoalSummary> = emptyList(),
    val completed: List<GoalSummary> = emptyList(),
    val archived: List<GoalSummary> = emptyList(),
    val showArchived: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    val isEmpty: Boolean get() = active.isEmpty() && completed.isEmpty() && archived.isEmpty()
}

/** Lista de metas com o resumo do contrato (`observeGoals`): reserva, restante e progresso vêm prontos. */
internal class GoalsViewModel(source: GoalDataSource) : ViewModel() {
    private val showArchived = MutableStateFlow(false)

    val uiState: StateFlow<GoalsUiState> = combine(source.observeGoals(includeArchived = true), showArchived) { list, show ->
        val sorted = list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.goal.details.name })
        GoalsUiState(
            isLoading = false,
            active = sorted.filter { it.goal.status == GoalStatus.ACTIVE },
            completed = sorted.filter { it.goal.status == GoalStatus.COMPLETED },
            archived = sorted.filter { it.goal.status == GoalStatus.ARCHIVED },
            showArchived = show,
        )
    }
        .catch { emit(GoalsUiState(isLoading = false, errorMessage = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    fun setShowArchived(value: Boolean) = showArchived.update { value }

    companion object {
        fun factory(source: GoalDataSource): ViewModelProvider.Factory = viewModelFactory { initializer { GoalsViewModel(source) } }
    }
}

// endregion

// region Formulário

internal sealed interface GoalCompletion {
    data class Created(val id: UUID) : GoalCompletion
    data object Updated : GoalCompletion
}

internal data class GoalFormUiState(
    val goalId: UUID? = null,
    val name: String = "",
    val targetText: String = "",
    val accountId: UUID? = null,
    val targetDate: LocalDate? = null,
    val accounts: List<FinancialAccount> = emptyList(),
    val accountsLoaded: Boolean = false,
    /** Conta original (edição): aparece mesmo se arquivada depois, para não sumir da escolha. */
    val originalAccountId: UUID? = null,
    val hasReserve: Boolean = false,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val status: GoalStatus? = null,
    @StringRes val nameError: Int? = null,
    @StringRes val targetError: Int? = null,
    @StringRes val accountError: Int? = null,
    @StringRes val operationError: Int? = null,
    val isSaving: Boolean = false,
    val completion: GoalCompletion? = null,
) {
    val isEditing: Boolean get() = goalId != null
    val isReadOnly: Boolean get() = status == GoalStatus.ARCHIVED || notFound
    val firstError: Int? get() = operationError ?: nameError ?: targetError ?: accountError
}

/**
 * Cadastro/edição de meta. Só coleta dados; validação de negócio (target abaixo do reservado, troca de conta com
 * histórico) é do contrato e volta como erro tipado.
 */
internal class GoalFormViewModel(
    private val source: GoalDataSource,
    accounts: AccountRepository,
    goalId: UUID?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GoalFormUiState(goalId = goalId, isLoading = goalId != null))
    val uiState: StateFlow<GoalFormUiState> = _uiState.asStateFlow()

    init {
        accounts.observeAll(includeArchived = false)
            .onEach { list ->
                _uiState.update { s ->
                    s.copy(
                        accounts = list, accountsLoaded = true,
                        accountId = s.accountId ?: list.singleOrNull()?.id.takeIf { !s.isEditing },
                    )
                }
            }
            .catch { _uiState.update { it.copy(accountsLoaded = true, operationError = R.string.error_load) } }
            .launchIn(viewModelScope)

        if (goalId != null) viewModelScope.launch {
            val loaded = runCatchingNonCancellation { source.observeGoal(goalId).first() }
            _uiState.update { s ->
                val g = loaded.getOrNull()
                when {
                    loaded.isFailure -> s.copy(isLoading = false, operationError = R.string.error_load)
                    g == null -> s.copy(isLoading = false, notFound = true)
                    else -> s.copy(
                        isLoading = false,
                        name = g.goal.details.name,
                        targetText = MoneyInput.formatForEditing(g.targetAmount),
                        accountId = g.goal.details.accountId,
                        originalAccountId = g.goal.details.accountId,
                        targetDate = g.goal.details.targetDate,
                        hasReserve = g.reservedAmount.cents > 0,
                        status = g.goal.status,
                    )
                }
            }
        }
    }

    private fun edit(block: (GoalFormUiState) -> GoalFormUiState) = _uiState.update { if (it.isSaving || it.isReadOnly) it else block(it) }

    fun onNameChange(v: String) = edit { it.copy(name = v.take(MAX_NAME), nameError = null) }
    fun onTargetChange(v: String) = edit { it.copy(targetText = MoneyInput.sanitizeTyping(v), targetError = null) }
    fun onAccountChange(id: UUID) = edit { it.copy(accountId = id, accountError = null) }
    fun onTargetDateChange(d: LocalDate?) = edit { it.copy(targetDate = d) }
    fun dismissError() = _uiState.update { it.copy(operationError = null) }

    fun save() {
        val s = _uiState.value
        if (s.isSaving || s.isReadOnly || s.isLoading || s.completion != null) return
        val parsed = MoneyInput.parse(s.targetText)
        val target = (parsed as? MoneyInput.Result.Valid)?.money?.takeIf { it.cents > 0 }
        val accountOk = s.accountId != null && (s.accounts.any { it.id == s.accountId } || s.accountId == s.originalAccountId)
        val errors = s.copy(
            nameError = if (s.name.isBlank()) R.string.error_name_required else null,
            targetError = if (target == null) R.string.error_goal_target else null,
            accountError = if (!accountOk) R.string.error_goal_account_required else null,
            operationError = null,
        )
        if (errors.firstError != null) {
            _uiState.value = errors
            return
        }
        val details = GoalDetails(s.name.trim(), target!!, s.accountId!!, s.targetDate)
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation {
                val id = s.goalId
                if (id == null) source.createGoal(details) else source.updateGoal(id, details)
            }
            _uiState.update { current ->
                outcome.fold(
                    onSuccess = { r ->
                        when (r) {
                            is GoalResult.Saved -> current.copy(
                                isSaving = false,
                                completion = if (s.goalId == null) GoalCompletion.Created(r.value.id) else GoalCompletion.Updated,
                            )
                            is GoalResult.Rejected -> current.copy(isSaving = false).withError(r.error)
                        }
                    },
                    onFailure = { current.copy(isSaving = false, operationError = R.string.error_save) },
                )
            }
        }
    }

    private fun GoalFormUiState.withError(error: GoalError) = when (error) {
        GoalError.INVALID_NAME -> copy(nameError = R.string.error_name_required)
        GoalError.INVALID_TARGET, GoalError.TARGET_BELOW_RESERVED -> copy(targetError = goalErrorMessage(error))
        GoalError.ACCOUNT_NOT_FOUND, GoalError.ACCOUNT_ARCHIVED, GoalError.ACCOUNT_CHANGE_NOT_ALLOWED -> copy(accountError = goalErrorMessage(error))
        GoalError.NOT_FOUND -> copy(notFound = true, operationError = goalErrorMessage(error))
        else -> copy(operationError = goalErrorMessage(error))
    }

    companion object {
        const val MAX_NAME = 60

        fun factory(source: GoalDataSource, accounts: AccountRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val id = createSavedStateHandle().get<String>(GoalRoutes.ARG_ID)?.let(UUID::fromString)
                GoalFormViewModel(source, accounts, id)
            }
        }
    }
}

// endregion

// region Detalhe (concluir, arquivar)

internal enum class GoalAction { Complete, Archive }

internal data class GoalDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val summary: GoalSummary? = null,
    val reserves: AccountReserveSummary? = null,
    val movements: List<GoalMovement> = emptyList(),
    val actionStatus: CommandStatus = CommandStatus.Idle,
    val lastAction: GoalAction? = null,
    @StringRes val actionError: Int? = null,
    val actionCount: Int = 0,
    @StringRes val loadError: Int? = null,
) {
    val status: GoalStatus? get() = summary?.goal?.status
    val canAllocate: Boolean get() = status == GoalStatus.ACTIVE
    val canRelease: Boolean get() = status != null && status != GoalStatus.ARCHIVED && (summary?.reservedAmount?.cents ?: 0) > 0
    /** A UI só oferece as ações; o contrato decide (`TARGET_NOT_REACHED`, `HAS_RESERVED_FUNDS`). */
    val canComplete: Boolean get() = status == GoalStatus.ACTIVE
    val canArchive: Boolean get() = status != null && status != GoalStatus.ARCHIVED
    val canEdit: Boolean get() = status != null && status != GoalStatus.ARCHIVED
}

/**
 * Detalhe: resumo (`observeGoal`), saldo/reserva/disponível da conta (`observeAccountReserves`, sem subtrair nada aqui)
 * e histórico (`observeMovements`). Concluir e arquivar usam os comandos do contrato.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class GoalDetailViewModel(private val source: GoalDataSource, private val id: UUID) : ViewModel() {
    private data class Action(val status: CommandStatus = CommandStatus.Idle, val last: GoalAction? = null, @StringRes val error: Int? = null, val count: Int = 0)

    private val action = MutableStateFlow(Action())

    val uiState: StateFlow<GoalDetailUiState> = combine(
        source.observeGoal(id).flatMapLatest { g ->
            if (g == null) flowOf(null to null) else source.observeAccountReserves(g.goal.details.accountId).map { g to it }
        },
        source.observeMovements(id),
        action,
    ) { (summary, reserves), movements, act ->
        GoalDetailUiState(
            isLoading = false,
            notFound = summary == null,
            summary = summary,
            reserves = reserves,
            movements = movements,
            actionStatus = act.status,
            lastAction = act.last,
            actionError = act.error,
            actionCount = act.count,
        )
    }
        .catch { emit(GoalDetailUiState(isLoading = false, loadError = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState())

    fun dismissError() = action.update { it.copy(status = CommandStatus.Idle, error = null) }
    fun complete() = run(GoalAction.Complete) { source.completeGoal(id) }
    fun archive() = run(GoalAction.Archive) { source.archiveGoal(id) }

    private fun run(kind: GoalAction, command: suspend () -> GoalResult<*>) {
        if (action.value.status == CommandStatus.Submitting) return
        action.update { it.copy(status = CommandStatus.Submitting, last = kind, error = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation { command() }
            action.update { current ->
                outcome.fold(
                    onSuccess = { r ->
                        when (r) {
                            is GoalResult.Saved -> current.copy(status = CommandStatus.Success, count = current.count + 1)
                            is GoalResult.Rejected -> current.copy(status = CommandStatus.Error, error = goalErrorMessage(r.error))
                        }
                    },
                    onFailure = { current.copy(status = CommandStatus.Error, error = R.string.error_goal_action) },
                )
            }
        }
    }

    companion object {
        fun factory(source: GoalDataSource): ViewModelProvider.Factory = viewModelFactory {
            initializer { GoalDetailViewModel(source, UUID.fromString(requireNotNull(createSavedStateHandle().get<String>(GoalRoutes.ARG_ID)))) }
        }
    }
}

// endregion

// region Reservar / liberar

internal enum class MovementMode { Allocate, Release }

internal data class GoalAllocationUiState(
    val mode: MovementMode = MovementMode.Allocate,
    val amountText: String = "",
    val status: CommandStatus = CommandStatus.Idle,
    @StringRes val amountError: Int? = null,
    @StringRes val error: Int? = null,
    /** Valor gravado pelo contrato no último sucesso, para o feedback ("R$ X foram reservados"). */
    val lastAmount: Money? = null,
    val lastMode: MovementMode? = null,
    val completedCount: Int = 0,
) {
    val isSubmitting: Boolean get() = status == CommandStatus.Submitting
}

/**
 * Reservar (`allocateToGoal`) ou liberar (`releaseFromGoal`) valor. A UI só lê o valor digitado; aceitar ou recusar
 * (restante da meta, disponível da conta, reservado atual) é decisão do contrato.
 */
internal class GoalAllocationViewModel(private val source: GoalDataSource, private val id: UUID) : ViewModel() {
    private val _uiState = MutableStateFlow(GoalAllocationUiState())
    val uiState: StateFlow<GoalAllocationUiState> = _uiState.asStateFlow()

    fun start(mode: MovementMode) = _uiState.update {
        if (it.isSubmitting) it else it.copy(mode = mode, amountText = "", status = CommandStatus.Idle, amountError = null, error = null)
    }

    fun onAmountChange(v: String) = _uiState.update { if (it.isSubmitting) it else it.copy(amountText = MoneyInput.sanitizeTyping(v), amountError = null, error = null) }

    fun submit() {
        val s = _uiState.value
        if (s.isSubmitting) return
        val parsed = MoneyInput.parse(s.amountText)
        val amount = (parsed as? MoneyInput.Result.Valid)?.money?.takeIf { it.cents > 0 }
        if (amount == null) {
            _uiState.update { it.copy(amountError = R.string.error_amount_positive) }
            return
        }
        _uiState.update { it.copy(status = CommandStatus.Submitting, error = null) }
        viewModelScope.launch {
            val outcome = runCatchingNonCancellation {
                if (s.mode == MovementMode.Allocate) source.allocateToGoal(id, amount) else source.releaseFromGoal(id, amount)
            }
            _uiState.update { current ->
                outcome.fold(
                    onSuccess = { r ->
                        when (r) {
                            is GoalResult.Saved -> current.copy(
                                status = CommandStatus.Success, lastAmount = r.value.amount, lastMode = s.mode,
                                completedCount = current.completedCount + 1, amountText = "",
                            )
                            is GoalResult.Rejected -> current.copy(status = CommandStatus.Error, amountError = goalErrorMessage(r.error))
                        }
                    },
                    onFailure = { current.copy(status = CommandStatus.Error, error = R.string.error_goal_action) },
                )
            }
        }
    }

    companion object {
        fun factory(source: GoalDataSource): ViewModelProvider.Factory = viewModelFactory {
            initializer { GoalAllocationViewModel(source, UUID.fromString(requireNotNull(createSavedStateHandle().get<String>(GoalRoutes.ARG_ID)))) }
        }
    }
}

// endregion
