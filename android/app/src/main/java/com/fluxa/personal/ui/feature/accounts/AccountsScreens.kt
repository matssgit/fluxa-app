package com.fluxa.personal.ui.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.ui.common.BalanceAmount
import com.fluxa.personal.ui.common.BrazilDate
import com.fluxa.personal.ui.common.ConfirmArchiveSheet
import com.fluxa.personal.ui.common.DateField
import com.fluxa.personal.ui.common.FluxaDatePicker
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.common.EmptyStateCard
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.FieldLabel
import com.fluxa.personal.ui.common.LoadingBlock
import com.fluxa.personal.ui.common.StatePill
import com.fluxa.personal.ui.common.SwitchRow
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonSize
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaChip
import com.fluxa.personal.ui.designsystem.components.FluxaInput
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.feature.catalog.visual
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

internal object AccountRoutes {
    const val ARG_ID = "id"
    const val LIST = "accounts"
    const val NEW = "accounts/new"
    const val EDIT = "accounts/{$ARG_ID}/edit"
    fun edit(id: UUID) = "accounts/$id/edit"
}

// region Lista

@Composable
internal fun AccountsScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (UUID) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(
        title = stringResource(R.string.accounts_title),
        overline = stringResource(R.string.accounts_overline),
        onBack = onBack,
        modifier = Modifier.testTag("screen-accounts"),
        bottomAction = {
            FluxaButton(
                text = stringResource(R.string.accounts_new),
                onClick = onCreate,
                size = FluxaButtonSize.Large,
                leadingIcon = FluxaIcons.Plus,
                modifier = Modifier.fillMaxWidth().testTag("action-new-account"),
            )
        },
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.errorMessage != null -> item { ErrorBanner(stringResource(state.errorMessage!!), onDismiss = null) }
            state.isEmpty -> item {
                EmptyStateCard(
                    title = stringResource(R.string.accounts_empty_title),
                    body = stringResource(R.string.accounts_empty_body),
                    icon = FluxaIcons.Wallet,
                )
            }
            else -> {
                item {
                    Text(
                        stringResource(R.string.accounts_list_note),
                        style = FluxaTheme.typography.caption,
                        color = FluxaTheme.colors.textSecondary,
                    )
                }
                items(state.active, key = { it.id }) { account -> AccountRow(account, state.balances[account.id], onClick = { onEdit(account.id) }) }
                if (state.archived.isNotEmpty()) {
                    item {
                        FluxaButton(
                            text = if (state.showArchived) stringResource(R.string.hide_archived)
                            else stringResource(R.string.show_archived, state.archived.size),
                            onClick = { viewModel.setShowArchived(!state.showArchived) },
                            variant = FluxaButtonVariant.Secondary,
                            leadingIcon = FluxaIcons.Archive,
                            modifier = Modifier.testTag("toggle-archived-accounts"),
                        )
                    }
                    if (state.showArchived) {
                        item { FluxaSectionHeader(title = stringResource(R.string.accounts_archived_section)) }
                        items(state.archived, key = { it.id }) { account -> AccountRow(account, state.balances[account.id], onClick = { onEdit(account.id) }) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountRow(account: FinancialAccount, balance: CalculatedBalance?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val details = account.details
    val visual = details.type.visual()
    val archived = account.archivedAt != null
    FluxaCard(
        modifier = modifier.fillMaxWidth().testTag("account-${details.name}"),
        onClick = onClick,
        onClickLabel = stringResource(R.string.accounts_edit),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(colors.brandTonal, FluxaTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(visual.icon, contentDescription = null, tint = colors.onBrandTonal, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(details.name, style = type.bodyStrong, color = colors.textPrimary, maxLines = 2)
                Text(stringResource(visual.label), style = type.caption, color = colors.textSecondary)
                Text(
                    stringResource(R.string.cash_current_balance),
                    style = type.caption,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 8.dp),
                )
                BalanceAmount(
                    balance = balance,
                    color = if ((balance as? CalculatedBalance.Exact)?.amount?.cents?.let { it < 0 } == true) colors.expense else colors.textPrimary,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.accounts_opening_balance), style = type.caption, color = colors.textMuted)
                    FluxaAmount(amount = details.openingBalance, style = type.caption, color = colors.textMuted)
                    Text(
                        stringResource(R.string.accounts_opening_balance_date, details.openingDate.format(BrazilDate)),
                        style = type.caption,
                        color = colors.textMuted,
                    )
                }
                if (archived || !details.includeInAvailable) {
                    FlowRowPills(archived = archived, outOfAvailable = !details.includeInAvailable)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowPills(archived: Boolean, outOfAvailable: Boolean) {
    FlowRow(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (archived) StatePill(stringResource(R.string.state_archived), FluxaIcons.Archive)
        if (outOfAvailable) StatePill(stringResource(R.string.accounts_not_in_available), null)
    }
}

// endregion

// region Formulário

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AccountFormScreen(viewModel: AccountFormViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmArchive by rememberSaveable { mutableStateOf(false) }
    var pickDate by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.completion) { if (state.completion != null) onDone() }

    FeatureScreen(
        title = stringResource(if (state.isEditing) R.string.accounts_edit else R.string.accounts_new),
        overline = stringResource(R.string.accounts_title),
        onBack = onDone,
        modifier = Modifier.testTag("screen-account-form"),
        bottomAction = if (state.isLoading || state.notFound) null else {
            {
                FluxaButton(
                    text = stringResource(R.string.action_save),
                    onClick = viewModel::save,
                    size = FluxaButtonSize.Large,
                    loading = state.isSaving,
                    enabled = !state.isArchiving,
                    modifier = Modifier.fillMaxWidth().testTag("action-save"),
                )
            }
        },
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.notFound -> item { ErrorBanner(stringResource(R.string.error_not_found), onDismiss = null) }
            else -> {
                state.operationError?.let { error ->
                    item { ErrorBanner(stringResource(error), onDismiss = viewModel::dismissError) }
                }
                if (state.isArchived) {
                    item { StatePill(stringResource(R.string.accounts_archived_note), FluxaIcons.Archive) }
                }
                item {
                    FluxaInput(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = stringResource(R.string.label_name),
                        errorText = state.nameError?.let { stringResource(it) },
                        enabled = !state.isBusy,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        modifier = Modifier.testTag("field-account-name"),
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel(stringResource(R.string.accounts_field_type))
                        FlowRow(
                            modifier = Modifier.selectableGroup(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AccountType.entries.forEach { type ->
                                val visual = type.visual()
                                FluxaChip(
                                    label = stringResource(visual.label),
                                    selected = state.type == type,
                                    onSelectedChange = { viewModel.onTypeChange(type) },
                                    leadingIcon = visual.icon,
                                    role = Role.RadioButton,
                                    enabled = !state.isBusy,
                                    modifier = Modifier.testTag("type-${type.name}"),
                                )
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FluxaInput(
                            value = state.amountText,
                            onValueChange = viewModel::onAmountChange,
                            label = stringResource(R.string.accounts_field_amount),
                            placeholder = stringResource(R.string.accounts_field_amount_hint),
                            supportingText = stringResource(R.string.accounts_field_amount_support),
                            errorText = state.amountError?.let { stringResource(it) },
                            enabled = !state.isBusy,
                            trailing = {
                                Text("R$", style = FluxaTheme.typography.label, color = FluxaTheme.colors.textMuted)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                            modifier = Modifier.testTag("field-account-amount"),
                        )
                        FluxaChip(
                            label = stringResource(R.string.accounts_field_negative),
                            selected = state.negative,
                            onSelectedChange = viewModel::onNegativeChange,
                            enabled = !state.isBusy,
                            modifier = Modifier.testTag("toggle-negative"),
                        )
                    }
                }
                item {
                    DateField(
                        label = stringResource(R.string.accounts_field_date),
                        supporting = stringResource(R.string.accounts_field_date_support),
                        date = state.openingDate,
                        enabled = !state.isBusy,
                        onClick = { pickDate = true },
                        modifier = Modifier.testTag("field-account-date"),
                    )
                }
                item {
                    SwitchRow(
                        label = stringResource(R.string.accounts_field_include),
                        supporting = stringResource(R.string.accounts_field_include_support),
                        checked = state.includeInAvailable,
                        onCheckedChange = viewModel::onIncludeInAvailableChange,
                        enabled = !state.isBusy,
                    )
                }
                if (state.isEditing && !state.isArchived) {
                    // Ação destrutiva no fim do conteúdo; o rodapé fixo fica só com Salvar (cabe com teclado em 320x640dp).
                    item {
                        FluxaButton(
                            text = stringResource(R.string.action_archive),
                            onClick = { confirmArchive = true },
                            variant = FluxaButtonVariant.Destructive,
                            leadingIcon = FluxaIcons.Archive,
                            loading = state.isArchiving,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth().testTag("action-archive"),
                        )
                    }
                }
            }
        }
    }

    if (confirmArchive) {
        ConfirmArchiveSheet(
            title = stringResource(R.string.accounts_archive_title),
            body = stringResource(R.string.accounts_archive_body),
            confirmLabel = stringResource(R.string.action_archive),
            onConfirm = {
                confirmArchive = false
                viewModel.archive()
            },
            onDismiss = { confirmArchive = false },
        )
    }
    if (pickDate) {
        FluxaDatePicker(
            initial = state.openingDate,
            onPicked = {
                viewModel.onOpeningDateChange(it)
                pickDate = false
            },
            onDismiss = { pickDate = false },
        )
    }
}


// endregion
