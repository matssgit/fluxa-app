package com.fluxa.personal.ui.feature.accounts

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.ui.common.MoneyInput
import com.fluxa.personal.ui.common.runCatchingNonCancellation
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// region Lista

internal data class AccountsUiState(
    val isLoading: Boolean = true,
    val active: List<FinancialAccount> = emptyList(),
    val archived: List<FinancialAccount> = emptyList(),
    val showArchived: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    val isEmpty: Boolean get() = !isLoading && active.isEmpty() && archived.isEmpty()
}

private sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data object Failed : Load<Nothing>
}

/** Lista de contas: ativas e arquivadas, refletindo os Flows do repositório. */
internal class AccountsViewModel(repository: AccountRepository) : ViewModel() {
    private val showArchived = MutableStateFlow(false)

    val uiState: StateFlow<AccountsUiState> = combine(
        repository.observeAll(includeArchived = true)
            .map<List<FinancialAccount>, Load<List<FinancialAccount>>> { Load.Ready(it) }
            .catch { emit(Load.Failed) },
        showArchived,
    ) { load, archivedVisible ->
        when (load) {
            Load.Loading -> AccountsUiState(showArchived = archivedVisible)
            Load.Failed -> AccountsUiState(isLoading = false, showArchived = archivedVisible, errorMessage = R.string.error_load)
            is Load.Ready -> AccountsUiState(
                isLoading = false,
                active = load.value.filter { it.archivedAt == null },
                archived = load.value.filter { it.archivedAt != null },
                showArchived = archivedVisible,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun setShowArchived(show: Boolean) = showArchived.update { show }

    companion object {
        fun factory(repository: AccountRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { AccountsViewModel(repository) }
        }
    }
}

// endregion

// region Formulário

internal enum class FormCompletion { Saved, Archived }

internal data class AccountFormUiState(
    val accountId: UUID? = null,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val name: String = "",
    val type: AccountType = AccountType.CHECKING,
    val amountText: String = "0,00",
    val negative: Boolean = false,
    val openingDate: LocalDate,
    val includeInAvailable: Boolean = true,
    val isArchived: Boolean = false,
    @StringRes val nameError: Int? = null,
    @StringRes val amountError: Int? = null,
    @StringRes val operationError: Int? = null,
    val isSaving: Boolean = false,
    val isArchiving: Boolean = false,
    val completion: FormCompletion? = null,
) {
    val isEditing: Boolean get() = accountId != null
    val isBusy: Boolean get() = isLoading || isSaving || isArchiving
}

/**
 * Cadastro e edição de conta. A validação do nome vem do próprio [AccountDetails] (domínio);
 * a do valor vem de [MoneyInput]. Toques repetidos são ignorados enquanto há operação em curso.
 */
internal class AccountFormViewModel(
    private val repository: AccountRepository,
    accountId: UUID?,
    clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountFormUiState(accountId = accountId, isLoading = accountId != null, openingDate = LocalDate.now(clock)))
    val uiState: StateFlow<AccountFormUiState> = _uiState.asStateFlow()

    init {
        if (accountId != null) {
            viewModelScope.launch {
                val loaded = runCatchingNonCancellation { repository.observe(accountId).first() }
                _uiState.update { state ->
                    loaded.fold(
                        onSuccess = { account ->
                            if (account == null) {
                                state.copy(isLoading = false, notFound = true)
                            } else {
                                val details = account.details
                                state.copy(
                                    isLoading = false,
                                    name = details.name,
                                    type = details.type,
                                    amountText = MoneyInput.formatForEditing(details.openingBalance),
                                    negative = details.openingBalance.cents < 0,
                                    openingDate = details.openingDate,
                                    includeInAvailable = details.includeInAvailable,
                                    isArchived = account.archivedAt != null,
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
    fun onTypeChange(value: AccountType) = _uiState.update { it.copy(type = value) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountText = MoneyInput.sanitizeTyping(value), amountError = null) }
    fun onNegativeChange(value: Boolean) = _uiState.update { it.copy(negative = value, amountError = null) }
    fun onOpeningDateChange(value: LocalDate) = _uiState.update { it.copy(openingDate = value) }
    fun onIncludeInAvailableChange(value: Boolean) = _uiState.update { it.copy(includeInAvailable = value) }
    fun dismissError() = _uiState.update { it.copy(operationError = null) }

    fun save() {
        val state = _uiState.value
        if (state.isBusy || state.completion != null || state.notFound) return

        val amount = when (val parsed = MoneyInput.parse(state.amountText, state.negative)) {
            is MoneyInput.Result.Valid -> parsed.money
            else -> null
        }
        val details = runCatching {
            AccountDetails(
                name = state.name,
                type = state.type,
                openingBalance = amount ?: Money(0),
                openingDate = state.openingDate,
                includeInAvailable = state.includeInAvailable,
            )
        }.getOrNull()

        val amountError = amountErrorFor(MoneyInput.parse(state.amountText, state.negative))
        val nameError = if (details == null) R.string.error_name_required else null
        if (nameError != null || amountError != null || details == null) {
            _uiState.update { it.copy(nameError = nameError, amountError = amountError) }
            return
        }

        _uiState.update { it.copy(isSaving = true, operationError = null) }
        viewModelScope.launch {
            val result = runCatchingNonCancellation {
                val id = state.accountId
                if (id == null) {
                    repository.create(details)
                    true
                } else {
                    repository.update(id, details)
                }
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { updated ->
                        if (updated) current.copy(isSaving = false, completion = FormCompletion.Saved)
                        else current.copy(isSaving = false, notFound = true)
                    },
                    onFailure = { current.copy(isSaving = false, operationError = R.string.error_save) },
                )
            }
        }
    }

    /** O contrato atual só oferece arquivar conta; não há reativação de conta. */
    fun archive() {
        val state = _uiState.value
        val id = state.accountId ?: return
        if (state.isBusy || state.isArchived || state.completion != null) return
        _uiState.update { it.copy(isArchiving = true, operationError = null) }
        viewModelScope.launch {
            val result = runCatchingNonCancellation { repository.archive(id) }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { archived ->
                        if (archived) current.copy(isArchiving = false, isArchived = true, completion = FormCompletion.Archived)
                        else current.copy(isArchiving = false, operationError = R.string.error_already_archived)
                    },
                    onFailure = { current.copy(isArchiving = false, operationError = R.string.error_archive) },
                )
            }
        }
    }

    companion object {
        @StringRes
        fun amountErrorFor(result: MoneyInput.Result): Int? = when (result) {
            is MoneyInput.Result.Valid -> null
            MoneyInput.Result.Empty -> R.string.error_amount_required
            MoneyInput.Result.InvalidFormat -> R.string.error_amount_format
            MoneyInput.Result.TooManyDecimals -> R.string.error_amount_decimals
            MoneyInput.Result.SignNotAllowed -> R.string.error_amount_sign
            MoneyInput.Result.TooLarge -> R.string.error_amount_too_large
        }

        fun factory(repository: AccountRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle: SavedStateHandle = createSavedStateHandle()
                val id = handle.get<String>(AccountRoutes.ARG_ID)?.let(UUID::fromString)
                AccountFormViewModel(repository, id)
            }
        }
    }
}

// endregion
