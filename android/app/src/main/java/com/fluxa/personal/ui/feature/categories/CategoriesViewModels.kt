package com.fluxa.personal.ui.feature.categories

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.data.repository.CategoryKindInUse
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.ui.common.runCatchingNonCancellation
import com.fluxa.personal.ui.feature.accounts.FormCompletion
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// region Lista

internal data class CategoriesUiState(
    val kind: TransactionDirection = TransactionDirection.EXPENSE,
    val isLoading: Boolean = true,
    val active: List<Category> = emptyList(),
    val archived: List<Category> = emptyList(),
    val reactivating: Set<UUID> = emptySet(),
    @StringRes val errorMessage: Int? = null,
)

private data class ListLoad(val categories: List<Category>?, val failed: Boolean)

/** Categorias por tipo (receita/despesa), ativas e arquivadas, com reativação direto na lista. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CategoriesViewModel(
    private val repository: CategoryRepository,
    initialKind: TransactionDirection = TransactionDirection.EXPENSE,
) : ViewModel() {
    private val kind = MutableStateFlow(initialKind)
    private val reactivating = MutableStateFlow<Set<UUID>>(emptySet())
    private val actionError = MutableStateFlow<Int?>(null)

    private val categories = kind.flatMapLatest { selected ->
        repository.observeAll(includeArchived = true, kind = selected)
            .map { ListLoad(it, failed = false) }
            .catch { emit(ListLoad(null, failed = true)) }
    }

    val uiState: StateFlow<CategoriesUiState> = combine(kind, categories, reactivating, actionError) { selected, load, busy, error ->
        val list = load.categories.orEmpty().filter { it.details.kind == selected }
        CategoriesUiState(
            kind = selected,
            isLoading = false,
            active = list.filter { it.archivedAt == null },
            archived = list.filter { it.archivedAt != null },
            reactivating = busy,
            errorMessage = error ?: if (load.failed) R.string.error_load else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState(kind = initialKind))

    fun selectKind(value: TransactionDirection) = kind.update { value }

    fun dismissError() = actionError.update { null }

    fun reactivate(id: UUID) {
        if (id in reactivating.value) return
        reactivating.update { it + id }
        viewModelScope.launch {
            val result = runCatchingNonCancellation { repository.reactivate(id) }
            reactivating.update { it - id }
            result.onFailure { actionError.update { R.string.error_reactivate } }
        }
    }

    companion object {
        fun factory(repository: CategoryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { CategoriesViewModel(repository) }
        }
    }
}

// endregion

// region Formulário

internal data class CategoryFormUiState(
    val categoryId: UUID? = null,
    val isLoading: Boolean = false,
    val notFound: Boolean = false,
    val name: String = "",
    val kind: TransactionDirection = TransactionDirection.EXPENSE,
    val icon: String? = null,
    val color: String? = null,
    val isArchived: Boolean = false,
    @StringRes val nameError: Int? = null,
    @StringRes val operationError: Int? = null,
    val isSaving: Boolean = false,
    val isChangingState: Boolean = false,
    val completion: FormCompletion? = null,
) {
    val isEditing: Boolean get() = categoryId != null
    val isBusy: Boolean get() = isLoading || isSaving || isChangingState
}

/**
 * Cadastro e edição de categoria. Nomes repetidos são permitidos pelo contrato e não são bloqueados aqui.
 * Arquivar encerra o formulário; reativar mantém o formulário aberto com o estado atualizado.
 */
internal class CategoryFormViewModel(
    private val repository: CategoryRepository,
    categoryId: UUID?,
    initialKind: TransactionDirection = TransactionDirection.EXPENSE,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryFormUiState(categoryId = categoryId, isLoading = categoryId != null, kind = initialKind))
    val uiState: StateFlow<CategoryFormUiState> = _uiState.asStateFlow()

    init {
        if (categoryId != null) {
            viewModelScope.launch {
                val loaded = runCatchingNonCancellation { repository.observe(categoryId).first() }
                _uiState.update { state ->
                    loaded.fold(
                        onSuccess = { category ->
                            if (category == null) {
                                state.copy(isLoading = false, notFound = true)
                            } else {
                                state.copy(
                                    isLoading = false,
                                    name = category.details.name,
                                    kind = category.details.kind,
                                    icon = category.details.icon,
                                    color = category.details.color,
                                    isArchived = category.archivedAt != null,
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
    fun onKindChange(value: TransactionDirection) = _uiState.update { it.copy(kind = value) }
    fun onIconChange(value: String?) = _uiState.update { it.copy(icon = value) }
    fun onColorChange(value: String?) = _uiState.update { it.copy(color = value) }
    fun dismissError() = _uiState.update { it.copy(operationError = null) }

    fun save() {
        val state = _uiState.value
        if (state.isBusy || state.completion != null || state.notFound) return
        val details = runCatching { CategoryDetails(state.name, state.kind, state.icon, state.color) }.getOrNull()
        if (details == null) {
            _uiState.update { it.copy(nameError = R.string.error_name_required) }
            return
        }
        _uiState.update { it.copy(isSaving = true, operationError = null) }
        viewModelScope.launch {
            val result = runCatchingNonCancellation {
                val id = state.categoryId
                if (id == null) {
                    repository.create(details)
                    true
                } else {
                    repository.update(id, details)
                }
            }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { ok ->
                        if (ok) current.copy(isSaving = false, completion = FormCompletion.Saved)
                        else current.copy(isSaving = false, notFound = true)
                    },
                    onFailure = { error ->
                        // Contrato do Sprint 3-A: o tipo de uma categoria já usada em lançamentos fica bloqueado.
                        if (error is CategoryKindInUse) current.copy(isSaving = false, operationError = R.string.error_category_kind_in_use)
                        else current.copy(isSaving = false, operationError = R.string.error_save)
                    },
                )
            }
        }
    }

    fun archive() = changeState(archive = true)

    fun reactivate() = changeState(archive = false)

    private fun changeState(archive: Boolean) {
        val state = _uiState.value
        val id = state.categoryId ?: return
        if (state.isBusy || state.completion != null || state.isArchived == archive) return
        _uiState.update { it.copy(isChangingState = true, operationError = null) }
        viewModelScope.launch {
            val result = runCatchingNonCancellation { if (archive) repository.archive(id) else repository.reactivate(id) }
            _uiState.update { current ->
                result.fold(
                    onSuccess = { changed ->
                        when {
                            !changed -> current.copy(
                                isChangingState = false,
                                operationError = if (archive) R.string.error_already_archived else R.string.error_already_active,
                            )
                            archive -> current.copy(isChangingState = false, isArchived = true, completion = FormCompletion.Archived)
                            else -> current.copy(isChangingState = false, isArchived = false)
                        }
                    },
                    onFailure = {
                        current.copy(isChangingState = false, operationError = if (archive) R.string.error_archive else R.string.error_reactivate)
                    },
                )
            }
        }
    }

    companion object {
        fun factory(repository: CategoryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val handle = createSavedStateHandle()
                val id = handle.get<String>(CategoryRoutes.ARG_ID)?.let(UUID::fromString)
                val kind = handle.get<String>(CategoryRoutes.ARG_KIND)
                    ?.let { runCatching { TransactionDirection.valueOf(it) }.getOrNull() }
                    ?: TransactionDirection.EXPENSE
                CategoryFormViewModel(repository, id, kind)
            }
        }
    }
}

// endregion
