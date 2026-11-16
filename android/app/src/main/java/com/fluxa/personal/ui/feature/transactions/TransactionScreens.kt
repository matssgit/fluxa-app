package com.fluxa.personal.ui.feature.transactions

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.ui.common.ConfirmArchiveSheet
import com.fluxa.personal.ui.common.DateField
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.FieldError
import com.fluxa.personal.ui.common.FieldLabel
import com.fluxa.personal.ui.common.FluxaDatePicker
import com.fluxa.personal.ui.common.LoadingBlock
import com.fluxa.personal.ui.common.formatLocal
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaBottomSheet
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonSize
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaChip
import com.fluxa.personal.ui.designsystem.components.FluxaInput
import com.fluxa.personal.ui.feature.catalog.categoryIconFor
import com.fluxa.personal.ui.feature.catalog.visual
import java.util.UUID

internal object TransactionRoutes {
    const val ARG_ID = "id"
    const val ARG_DIRECTION = "direction"
    const val NEW = "transactions/new?$ARG_DIRECTION={$ARG_DIRECTION}"
    const val DETAIL = "transactions/{$ARG_ID}"
    fun new(direction: TransactionDirection) = "transactions/new?$ARG_DIRECTION=${direction.name}"
    fun detail(id: UUID) = "transactions/$id"
}

/** Sheet do botão central: escolhe despesa ou receita. Usa o padrão único de sheet do Design System. */
@Composable
internal fun NewEntrySheet(onChoose: (TransactionDirection) -> Unit, onDismiss: () -> Unit) {
    FluxaBottomSheet(
        title = stringResource(R.string.action_new_entry),
        onDismissRequest = onDismiss,
        footer = {
            FluxaButton(
                text = stringResource(R.string.action_close),
                onClick = onDismiss,
                variant = FluxaButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
        },
    ) {
        Text(stringResource(R.string.new_entry_sheet_body), style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary)
        EntryOption(
            title = stringResource(R.string.new_expense),
            hint = stringResource(R.string.new_expense_hint),
            sign = "−",
            income = false,
            onClick = { onChoose(TransactionDirection.EXPENSE) },
            modifier = Modifier.testTag("new-expense"),
        )
        EntryOption(
            title = stringResource(R.string.new_income),
            hint = stringResource(R.string.new_income_hint),
            sign = "+",
            income = true,
            onClick = { onChoose(TransactionDirection.INCOME) },
            modifier = Modifier.testTag("new-income"),
        )
    }
}

@Composable
private fun EntryOption(title: String, hint: String, sign: String, income: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = FluxaTheme.colors
    FluxaCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(if (income) colors.incomeContainer else colors.expenseContainer, FluxaTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text(sign, style = FluxaTheme.typography.title, color = if (income) colors.income else colors.expense)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = FluxaTheme.typography.bodyStrong, color = colors.textPrimary)
                Text(hint, style = FluxaTheme.typography.caption, color = colors.textSecondary)
            }
            Icon(FluxaIcons.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TransactionFormScreen(
    viewModel: TransactionFormViewModel,
    onDone: (TransactionCompletion?, TransactionDirection) -> Unit,
    onCreateAccount: () -> Unit,
    onCreateCategory: (TransactionDirection) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val colors = FluxaTheme.colors

    LaunchedEffect(state.completion) { state.completion?.let { onDone(it, state.direction) } }

    val title = when {
        state.isEditing -> stringResource(R.string.transaction_edit)
        state.direction == TransactionDirection.INCOME -> stringResource(R.string.new_income)
        else -> stringResource(R.string.new_expense)
    }
    val overline = if (state.isEditing) {
        stringResource(if (state.direction == TransactionDirection.INCOME) R.string.transaction_overline_income else R.string.transaction_overline_expense)
    } else stringResource(R.string.cash)

    FeatureScreen(
        title = title,
        overline = overline,
        onBack = { onDone(null, state.direction) },
        modifier = Modifier.testTag("screen-transaction-form"),
        bottomAction = if (state.isLoading || state.isReadOnly) null else {
            {
                FluxaButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::save,
                    size = FluxaButtonSize.Large,
                    loading = state.isSaving,
                    enabled = !state.isCancelling,
                    modifier = Modifier.fillMaxWidth().testTag("action-save"),
                )
            }
        },
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.notFound -> item { ErrorBanner(stringResource(R.string.error_transaction_not_found), onDismiss = null) }
            else -> {
                state.operationError?.let { error -> item { ErrorBanner(stringResource(error), onDismiss = viewModel::dismissError) } }
                state.cancelledAt?.let { cancelled ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceSunken, FluxaTheme.shapes.medium)
                                .padding(16.dp)
                                .testTag("cancelled-note"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(FluxaIcons.Close, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.transaction_cancelled_note, cancelled.formatLocal()), style = FluxaTheme.typography.label, color = colors.textSecondary)
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel(stringResource(R.string.transaction_field_direction))
                        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FluxaChip(
                                label = stringResource(R.string.categories_kind_expense_single),
                                selected = state.direction == TransactionDirection.EXPENSE,
                                onSelectedChange = { viewModel.onDirectionChange(TransactionDirection.EXPENSE) },
                                role = Role.RadioButton,
                                enabled = !state.isBusy && !state.isReadOnly,
                                modifier = Modifier.testTag("direction-EXPENSE"),
                            )
                            FluxaChip(
                                label = stringResource(R.string.categories_kind_income_single),
                                selected = state.direction == TransactionDirection.INCOME,
                                onSelectedChange = { viewModel.onDirectionChange(TransactionDirection.INCOME) },
                                role = Role.RadioButton,
                                enabled = !state.isBusy && !state.isReadOnly,
                                modifier = Modifier.testTag("direction-INCOME"),
                            )
                        }
                    }
                }
                item {
                    FluxaInput(
                        value = state.amountText,
                        onValueChange = viewModel::onAmountChange,
                        label = stringResource(R.string.transaction_field_amount),
                        placeholder = stringResource(R.string.accounts_field_amount_hint),
                        supportingText = stringResource(R.string.transaction_field_amount_support),
                        errorText = state.amountError?.let { stringResource(it) },
                        enabled = !state.isBusy && !state.isReadOnly,
                        trailing = { Text("R$", style = FluxaTheme.typography.label, color = colors.textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.testTag("field-tx-amount"),
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("group-account")) {
                        FieldLabel(stringResource(R.string.transaction_field_account))
                        if (state.accountsLoaded && state.accounts.isEmpty()) {
                            Text(stringResource(R.string.transaction_no_accounts), style = FluxaTheme.typography.body, color = colors.textSecondary)
                            FluxaButton(
                                text = stringResource(R.string.transaction_create_account),
                                onClick = onCreateAccount,
                                variant = FluxaButtonVariant.Secondary,
                                leadingIcon = FluxaIcons.Plus,
                                modifier = Modifier.testTag("action-create-account"),
                            )
                        } else {
                            FlowRow(
                                modifier = Modifier.selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                state.accounts.forEach { account ->
                                    FluxaChip(
                                        label = account.details.name,
                                        selected = state.accountId == account.id,
                                        onSelectedChange = { viewModel.onAccountChange(account.id) },
                                        leadingIcon = account.details.type.visual().icon,
                                        role = Role.RadioButton,
                                        enabled = !state.isBusy && !state.isReadOnly,
                                        modifier = Modifier.testTag("account-option-${account.details.name}"),
                                    )
                                }
                            }
                        }
                        if (state.selectedAccountInactive && !state.isReadOnly) {
                            FieldError(stringResource(R.string.transaction_selected_account_archived))
                        }
                        state.accountError?.let { FieldError(stringResource(it)) }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("group-category")) {
                        FieldLabel(stringResource(R.string.transaction_field_category))
                        if (state.categoriesLoaded && state.categories.isEmpty()) {
                            Text(
                                stringResource(
                                    if (state.direction == TransactionDirection.INCOME) R.string.transaction_no_categories_income
                                    else R.string.transaction_no_categories_expense,
                                ),
                                style = FluxaTheme.typography.body,
                                color = colors.textSecondary,
                            )
                            if (!state.isReadOnly) {
                                FluxaButton(
                                    text = stringResource(R.string.transaction_create_category),
                                    onClick = { onCreateCategory(state.direction) },
                                    variant = FluxaButtonVariant.Secondary,
                                    leadingIcon = FluxaIcons.Plus,
                                    modifier = Modifier.testTag("action-create-category"),
                                )
                            }
                        } else {
                            FlowRow(
                                modifier = Modifier.selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                state.categories.forEach { category ->
                                    FluxaChip(
                                        label = category.details.name,
                                        selected = state.categoryId == category.id,
                                        onSelectedChange = { viewModel.onCategoryChange(category.id) },
                                        leadingIcon = categoryIconFor(category.details.icon),
                                        role = Role.RadioButton,
                                        enabled = !state.isBusy && !state.isReadOnly,
                                        modifier = Modifier.testTag("category-option-${category.details.name}"),
                                    )
                                }
                            }
                        }
                        if (state.selectedCategoryInactive && !state.isReadOnly) {
                            FieldError(stringResource(R.string.transaction_selected_category_archived))
                        }
                        state.categoryError?.let { FieldError(stringResource(it)) }
                    }
                }
                item {
                    DateField(
                        label = stringResource(R.string.transaction_field_date),
                        supporting = stringResource(R.string.transaction_field_date_support),
                        date = state.occurredOn,
                        enabled = !state.isBusy && !state.isReadOnly,
                        onClick = { pickDate = true },
                        errorText = state.dateError?.let { stringResource(it) },
                        modifier = Modifier.testTag("field-tx-date"),
                    )
                }
                item {
                    FluxaInput(
                        value = state.description,
                        onValueChange = viewModel::onDescriptionChange,
                        label = stringResource(R.string.transaction_field_description),
                        placeholder = stringResource(R.string.transaction_field_description_hint),
                        enabled = !state.isBusy && !state.isReadOnly,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        modifier = Modifier.testTag("field-tx-description"),
                    )
                }
                if (state.isEditing) {
                    item {
                        val created = state.createdAt?.formatLocal().orEmpty()
                        val updated = state.updatedAt?.formatLocal().orEmpty()
                        Text(stringResource(R.string.transaction_meta, created, updated), style = FluxaTheme.typography.caption, color = colors.textMuted)
                    }
                }
                if (state.isEditing && !state.isReadOnly) {
                    // Ação destrutiva no fim do conteúdo, fora do rodapé fixo: o rodapé fica só com Salvar,
                    // para sobrar espaço útil com o teclado aberto em telas baixas.
                    item {
                        FluxaButton(
                            text = stringResource(R.string.transaction_cancel_action),
                            onClick = { confirmCancel = true },
                            variant = FluxaButtonVariant.Destructive,
                            leadingIcon = FluxaIcons.Close,
                            loading = state.isCancelling,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth().testTag("action-cancel-transaction"),
                        )
                    }
                }
            }
        }
    }

    if (confirmCancel) {
        ConfirmArchiveSheet(
            title = stringResource(R.string.transaction_cancel_title),
            body = stringResource(R.string.transaction_cancel_body),
            confirmLabel = stringResource(R.string.transaction_cancel_confirm),
            dismissLabel = stringResource(R.string.transaction_keep),
            confirmIcon = FluxaIcons.Close,
            onConfirm = {
                confirmCancel = false
                viewModel.cancelTransaction()
            },
            onDismiss = { confirmCancel = false },
        )
    }
    if (pickDate) {
        FluxaDatePicker(
            initial = state.occurredOn,
            maxDate = state.today,
            onPicked = {
                viewModel.onDateChange(it)
                pickDate = false
            },
            onDismiss = { pickDate = false },
        )
    }
}
