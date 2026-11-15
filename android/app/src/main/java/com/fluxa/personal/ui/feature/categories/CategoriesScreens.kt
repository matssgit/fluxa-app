package com.fluxa.personal.ui.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.ui.common.ConfirmArchiveSheet
import com.fluxa.personal.ui.common.EmptyStateCard
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.FieldLabel
import com.fluxa.personal.ui.common.LoadingBlock
import com.fluxa.personal.ui.common.StatePill
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonSize
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaChip
import com.fluxa.personal.ui.designsystem.components.FluxaInput
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.feature.catalog.CategoryColors
import com.fluxa.personal.ui.feature.catalog.CategoryIcons
import com.fluxa.personal.ui.feature.catalog.categoryColorFor
import com.fluxa.personal.ui.feature.catalog.categoryIconFor
import java.util.UUID

internal object CategoryRoutes {
    const val ARG_ID = "id"
    const val ARG_KIND = "kind"
    const val LIST = "categories"
    const val NEW = "categories/new?$ARG_KIND={$ARG_KIND}"
    const val EDIT = "categories/{$ARG_ID}/edit"
    fun new(kind: TransactionDirection) = "categories/new?$ARG_KIND=${kind.name}"
    fun edit(id: UUID) = "categories/$id/edit"
}

// region Lista

@Composable
internal fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onBack: () -> Unit,
    onCreate: (TransactionDirection) -> Unit,
    onEdit: (UUID) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(
        title = stringResource(R.string.categories_title),
        overline = stringResource(R.string.more),
        onBack = onBack,
        modifier = Modifier.testTag("screen-categories"),
        bottomAction = {
            FluxaButton(
                text = stringResource(R.string.categories_new),
                onClick = { onCreate(state.kind) },
                size = FluxaButtonSize.Large,
                leadingIcon = FluxaIcons.Plus,
                modifier = Modifier.fillMaxWidth().testTag("action-new-category"),
            )
        },
    ) {
        item { KindSelector(selected = state.kind, onSelect = viewModel::selectKind) }
        state.errorMessage?.let { error ->
            item { ErrorBanner(stringResource(error), onDismiss = viewModel::dismissError) }
        }
        when {
            state.isLoading -> item { LoadingBlock() }
            else -> {
                if (state.active.isEmpty()) {
                    item {
                        EmptyStateCard(
                            title = stringResource(
                                if (state.kind == TransactionDirection.INCOME) R.string.categories_empty_income
                                else R.string.categories_empty_expense,
                            ),
                            body = stringResource(R.string.categories_empty_body),
                            icon = FluxaIcons.Tag,
                        )
                    }
                }
                items(state.active, key = { it.id }) { category ->
                    CategoryRow(category, onClick = { onEdit(category.id) })
                }
                if (state.archived.isNotEmpty()) {
                    item { FluxaSectionHeader(title = stringResource(R.string.categories_archived_section)) }
                    items(state.archived, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            onClick = { onEdit(category.id) },
                            trailing = {
                                FluxaButton(
                                    text = stringResource(R.string.action_reactivate),
                                    onClick = { viewModel.reactivate(category.id) },
                                    variant = FluxaButtonVariant.Secondary,
                                    leadingIcon = FluxaIcons.Restore,
                                    loading = category.id in state.reactivating,
                                    modifier = Modifier.testTag("reactivate-${category.details.name}"),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KindSelector(selected: TransactionDirection, onSelect: (TransactionDirection) -> Unit) {
    Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FluxaChip(
            label = stringResource(R.string.categories_kind_expense),
            selected = selected == TransactionDirection.EXPENSE,
            onSelectedChange = { onSelect(TransactionDirection.EXPENSE) },
            role = Role.RadioButton,
            modifier = Modifier.testTag("kind-EXPENSE"),
        )
        FluxaChip(
            label = stringResource(R.string.categories_kind_income),
            selected = selected == TransactionDirection.INCOME,
            onSelectedChange = { onSelect(TransactionDirection.INCOME) },
            role = Role.RadioButton,
            modifier = Modifier.testTag("kind-INCOME"),
        )
    }
}

@Composable
internal fun CategoryBadge(icon: String?, color: String?, size: Dp = 44.dp) {
    val colors = FluxaTheme.colors
    val tone = categoryColorFor(color)
    Box(
        modifier = Modifier
            .size(size)
            .background(tone.copy(alpha = 0.22f).compositeOver(colors.surface), FluxaTheme.shapes.pill)
            .border(2.dp, tone, FluxaTheme.shapes.pill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(categoryIconFor(icon), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(size / 2))
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = FluxaTheme.colors
    val archived = category.archivedAt != null
    Column {
        FluxaCard(
            modifier = Modifier.fillMaxWidth().testTag("category-${category.details.name}"),
            onClick = onClick,
            onClickLabel = stringResource(R.string.categories_edit),
            contentPadding = PaddingValues(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CategoryBadge(category.details.icon, category.details.color)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(category.details.name, style = FluxaTheme.typography.bodyStrong, color = colors.textPrimary, maxLines = 2)
                    if (archived) StatePill(stringResource(R.string.state_archived), FluxaIcons.Archive)
                }
            }
        }
        if (trailing != null) {
            Box(modifier = Modifier.padding(top = 8.dp)) { trailing() }
        }
    }
}

// endregion

// region Formulário

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryFormScreen(viewModel: CategoryFormViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.completion) { if (state.completion != null) onDone() }

    FeatureScreen(
        title = stringResource(if (state.isEditing) R.string.categories_edit else R.string.categories_new),
        overline = stringResource(R.string.categories_title),
        onBack = onDone,
        modifier = Modifier.testTag("screen-category-form"),
        bottomAction = if (state.isLoading || state.notFound) null else {
            {
                FluxaButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::save,
                    size = FluxaButtonSize.Large,
                    loading = state.isSaving,
                    enabled = !state.isChangingState,
                    modifier = Modifier.fillMaxWidth().testTag("action-save"),
                )
                if (state.isEditing) {
                    if (state.isArchived) {
                        FluxaButton(
                            text = stringResource(R.string.action_reactivate),
                            onClick = viewModel::reactivate,
                            variant = FluxaButtonVariant.Secondary,
                            leadingIcon = FluxaIcons.Restore,
                            loading = state.isChangingState,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth().testTag("action-reactivate"),
                        )
                    } else {
                        FluxaButton(
                            text = stringResource(R.string.action_archive),
                            onClick = { confirmArchive = true },
                            variant = FluxaButtonVariant.Destructive,
                            leadingIcon = FluxaIcons.Archive,
                            loading = state.isChangingState,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth().testTag("action-archive"),
                        )
                    }
                }
            }
        },
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.notFound -> item { ErrorBanner(stringResource(R.string.error_not_found), onDismiss = null) }
            else -> {
                state.operationError?.let { error -> item { ErrorBanner(stringResource(error), onDismiss = viewModel::dismissError) } }
                if (state.isArchived) item { StatePill(stringResource(R.string.categories_archived_note), FluxaIcons.Archive) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CategoryBadge(state.icon, state.color, size = 56.dp)
                        FluxaInput(
                            value = state.name,
                            onValueChange = viewModel::onNameChange,
                            label = stringResource(R.string.label_name),
                            errorText = state.nameError?.let { stringResource(it) },
                            enabled = !state.isBusy,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f).testTag("field-category-name"),
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel(stringResource(R.string.categories_field_kind))
                        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                TransactionDirection.EXPENSE to R.string.categories_kind_expense_single,
                                TransactionDirection.INCOME to R.string.categories_kind_income_single,
                            ).forEach { (kind, label) ->
                                FluxaChip(
                                    label = stringResource(label),
                                    selected = state.kind == kind,
                                    onSelectedChange = { viewModel.onKindChange(kind) },
                                    role = Role.RadioButton,
                                    enabled = !state.isBusy,
                                    modifier = Modifier.testTag("form-kind-${kind.name}"),
                                )
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel(stringResource(R.string.categories_field_icon))
                        FlowRow(
                            modifier = Modifier.selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChoiceTile(
                                selected = state.icon == null,
                                description = stringResource(R.string.categories_none),
                                onClick = { viewModel.onIconChange(null) },
                                enabled = !state.isBusy,
                            ) { Icon(FluxaIcons.Close, null, tint = FluxaTheme.colors.textMuted, modifier = Modifier.size(20.dp)) }
                            CategoryIcons.forEach { option ->
                                ChoiceTile(
                                    selected = state.icon == option.key,
                                    description = stringResource(option.label),
                                    onClick = { viewModel.onIconChange(option.key) },
                                    enabled = !state.isBusy,
                                    modifier = Modifier.testTag("icon-${option.key}"),
                                ) { Icon(option.icon, null, tint = FluxaTheme.colors.textPrimary, modifier = Modifier.size(22.dp)) }
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel(stringResource(R.string.categories_field_color))
                        FlowRow(
                            modifier = Modifier.selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ChoiceTile(
                                selected = state.color == null,
                                description = stringResource(R.string.categories_none),
                                onClick = { viewModel.onColorChange(null) },
                                enabled = !state.isBusy,
                            ) { Icon(FluxaIcons.Close, null, tint = FluxaTheme.colors.textMuted, modifier = Modifier.size(20.dp)) }
                            CategoryColors.forEach { option ->
                                ChoiceTile(
                                    selected = state.color == option.hex,
                                    description = stringResource(option.label),
                                    onClick = { viewModel.onColorChange(option.hex) },
                                    enabled = !state.isBusy,
                                    modifier = Modifier.testTag("color-${option.hex}"),
                                ) { ColorDot(option.color) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmArchive) {
        ConfirmArchiveSheet(
            title = stringResource(R.string.categories_archive_title),
            body = stringResource(R.string.categories_archive_body),
            confirmLabel = stringResource(R.string.action_archive),
            onConfirm = {
                confirmArchive = false
                viewModel.archive()
            },
            onDismiss = { confirmArchive = false },
        )
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(Modifier.size(24.dp).background(color, FluxaTheme.shapes.pill))
}

/**
 * Opção de escolha única (ícone ou cor) com alvo de 48dp. Selecionado = borda grossa da marca + check,
 * portanto não depende só da cor.
 */
@Composable
private fun ChoiceTile(
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = FluxaTheme.colors
    val shape = FluxaTheme.shapes.medium
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(shape)
            .background(if (selected) colors.brandTonal else colors.surfaceSunken, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.brand else colors.border, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(16.dp)
                    .background(colors.brand, FluxaTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FluxaIcons.Check, null, tint = colors.onBrand, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// endregion
