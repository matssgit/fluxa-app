package com.fluxa.personal.ui.feature.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountReserveSummary
import com.fluxa.personal.domain.model.GoalMovementType
import com.fluxa.personal.domain.model.GoalStatus
import com.fluxa.personal.domain.model.GoalSummary
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.common.BalanceAmount
import com.fluxa.personal.ui.common.NegativeAvailableAlert
import com.fluxa.personal.ui.common.ReserveLine
import com.fluxa.personal.ui.common.isNarrowForText
import com.fluxa.personal.ui.common.isNegative
import com.fluxa.personal.ui.common.ConfirmArchiveSheet
import com.fluxa.personal.ui.common.DateField
import com.fluxa.personal.ui.common.EmptyStateCard
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.FieldError
import com.fluxa.personal.ui.common.FieldLabel
import com.fluxa.personal.ui.common.FluxaDatePicker
import com.fluxa.personal.ui.common.LoadingBlock
import com.fluxa.personal.ui.common.StatePill
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.LocalAmountsHidden
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.designsystem.components.FluxaBottomSheet
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonSize
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaChip
import com.fluxa.personal.ui.designsystem.components.FluxaInput
import com.fluxa.personal.ui.designsystem.components.FluxaMoneyFormat
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.designsystem.components.FluxaSurfaceTone
import com.fluxa.personal.ui.feature.catalog.visual
import com.fluxa.personal.ui.feature.credit.CommandStatus
import com.fluxa.personal.ui.feature.subscriptions.InfoLine
import com.fluxa.personal.ui.feature.subscriptions.RowDivider
import com.fluxa.personal.ui.feature.subscriptions.short
import com.fluxa.personal.ui.feature.subscriptions.spoken
import java.util.UUID

// region Componentes

/**
 * Barra de progresso da meta. A fração vem dos pontos-base do contrato (só desenho; nenhum valor monetário passa por
 * ponto flutuante). Anunciada em percentual.
 */
@Composable
internal fun GoalProgressBar(basisPoints: Int, modifier: Modifier = Modifier, onInverse: Boolean = false) {
    val colors = FluxaTheme.colors
    val track = if (onInverse) colors.onSurfaceInverse.copy(alpha = 0.18f) else colors.surfaceSunken
    val fill = if (onInverse) colors.onSurfaceInverse else colors.brand
    val percent = basisPoints / 100
    val described = stringResource(R.string.goal_progress_described, percent)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(FluxaTheme.shapes.pill)
            .background(track)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(basisPoints.coerceIn(0, 10_000) / 10_000f, 0f..1f)
                stateDescription = described
            },
    ) {
        Box(Modifier.fillMaxWidth(basisPoints.coerceIn(0, 10_000) / 10_000f).height(8.dp).clip(FluxaTheme.shapes.pill).background(fill))
    }
}

@Composable
internal fun GoalStatusPill(status: GoalStatus, modifier: Modifier = Modifier) {
    val (label, icon) = when (status) {
        GoalStatus.ACTIVE -> stringResource(R.string.goal_status_active) to FluxaIcons.Target
        GoalStatus.COMPLETED -> stringResource(R.string.goal_status_completed) to FluxaIcons.Check
        GoalStatus.ARCHIVED -> stringResource(R.string.goal_status_archived) to FluxaIcons.Archive
    }
    val described = stringResource(R.string.goal_status_described, label)
    StatePill(label, icon, modifier = modifier, spoken = described)
}

// endregion

// region Lista

@Composable
internal fun GoalsScreen(viewModel: GoalsViewModel, onBack: () -> Unit, onNew: () -> Unit, onOpen: (UUID) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(
        title = stringResource(R.string.goals_title),
        overline = stringResource(R.string.goals_overline),
        onBack = onBack,
        modifier = Modifier.testTag("screen-goals"),
        bottomAction = {
            FluxaButton(
                text = stringResource(R.string.goals_new), onClick = onNew, leadingIcon = FluxaIcons.Plus,
                size = FluxaButtonSize.Large, modifier = Modifier.fillMaxWidth().testTag("action-new-goal"),
            )
        },
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.errorMessage != null -> item { ErrorBanner(stringResource(state.errorMessage!!), onDismiss = null) }
            state.isEmpty -> item { EmptyStateCard(stringResource(R.string.goals_empty_title), stringResource(R.string.goals_empty_body), FluxaIcons.Target) }
            else -> {
                section(R.string.goals_section_active, state.active, onOpen)
                section(R.string.goals_section_completed, state.completed, onOpen)
                if (state.archived.isNotEmpty()) {
                    item {
                        FluxaButton(
                            text = stringResource(if (state.showArchived) R.string.goals_hide_archived else R.string.goals_show_archived),
                            onClick = { viewModel.setShowArchived(!state.showArchived) }, variant = FluxaButtonVariant.Secondary,
                            leadingIcon = FluxaIcons.Archive, modifier = Modifier.fillMaxWidth().testTag("toggle-archived-goals"),
                        )
                    }
                    if (state.showArchived) section(R.string.goals_section_archived, state.archived, onOpen)
                }
            }
        }
    }
}

private fun LazyListScope.section(title: Int, list: List<GoalSummary>, onOpen: (UUID) -> Unit) {
    if (list.isEmpty()) return
    item { FluxaSectionHeader(title = androidx.compose.ui.res.stringResource(title)) }
    items(list, key = { it.goal.id }) { GoalCard(it, onClick = { onOpen(it.goal.id) }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoalCard(summary: GoalSummary, onClick: () -> Unit) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val g = summary.goal
    val archived = g.status == GoalStatus.ARCHIVED
    FluxaCard(
        modifier = Modifier.fillMaxWidth().testTag("goal-${g.details.name}"),
        onClick = onClick,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(g.details.name, style = type.titleSmall, color = if (archived) colors.textSecondary else colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.goal_progress_percent, summary.percent), style = type.titleSmall, color = colors.brand)
        }
        Spacer(Modifier.height(8.dp))
        // "R$ 2.000 reservados de R$ 5.000" — quebra em telas estreitas sem cortar valores.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FluxaAmount(amount = summary.reservedAmount, style = type.amount, color = colors.textPrimary)
            Text(stringResource(R.string.goal_reserved_of), style = type.label, color = colors.textSecondary, modifier = Modifier.align(Alignment.CenterVertically))
            FluxaAmount(amount = summary.targetAmount, style = type.label, color = colors.textSecondary, modifier = Modifier.align(Alignment.CenterVertically))
        }
        Spacer(Modifier.height(10.dp))
        GoalProgressBar(summary.progressBasisPoints)
        Spacer(Modifier.height(10.dp))
        RemainingLine(summary)
        Text(stringResource(R.string.goal_account, summary.account.details.name), style = type.caption, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        g.details.targetDate?.let {
            val targetSpoken = stringResource(R.string.goal_target_date, it.spoken())
            Text(stringResource(R.string.goal_target_date, it.short()), style = type.caption, color = colors.textSecondary, modifier = Modifier.semantics { contentDescription = targetSpoken })
        }
        if (g.status != GoalStatus.ACTIVE) GoalStatusPill(g.status, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun RemainingLine(summary: GoalSummary) {
    val type = FluxaTheme.typography
    val colors = FluxaTheme.colors
    if (summary.remainingAmount.cents == 0L) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(FluxaIcons.Check, null, tint = colors.brand, modifier = Modifier.size(16.dp))
            Text(stringResource(R.string.goal_reached), style = type.label, color = colors.brand)
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Text(stringResource(R.string.goal_remaining), style = type.label, color = colors.textSecondary)
            FluxaAmount(amount = summary.remainingAmount, style = type.label, color = colors.textPrimary)
        }
    }
}

// endregion

// region Formulário

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GoalFormScreen(viewModel: GoalFormViewModel, onDone: (GoalCompletion?) -> Unit, onCreateAccount: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    LaunchedEffect(state.completion) { state.completion?.let(onDone) }
    val enabled = !state.isSaving && !state.isReadOnly
    FeatureScreen(
        title = stringResource(if (state.isEditing) R.string.goals_edit else R.string.goals_new),
        overline = stringResource(R.string.goals_title),
        onBack = { onDone(null) },
        modifier = Modifier.testTag("screen-goal-form"),
        bottomAction = if (state.isLoading || state.isReadOnly) null else {
            {
                state.firstError?.let {
                    FieldError(stringResource(it), modifier = Modifier.padding(bottom = 8.dp).semantics { liveRegion = LiveRegionMode.Assertive }.testTag("goal-form-error"))
                }
                FluxaButton(
                    text = stringResource(R.string.action_save), onClick = viewModel::save, size = FluxaButtonSize.Large,
                    loading = state.isSaving, modifier = Modifier.fillMaxWidth().testTag("action-save"),
                )
            }
        },
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.notFound -> item { ErrorBanner(stringResource(R.string.error_goal_not_found), onDismiss = null) }
            else -> {
                state.operationError?.let { item { ErrorBanner(stringResource(it), onDismiss = viewModel::dismissError) } }
                item {
                    FluxaInput(
                        value = state.name, onValueChange = viewModel::onNameChange,
                        label = stringResource(R.string.goal_field_name), placeholder = stringResource(R.string.goal_field_name_hint),
                        errorText = state.nameError?.let { stringResource(it) }, enabled = enabled,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        modifier = Modifier.testTag("field-goal-name"),
                    )
                }
                item {
                    FluxaInput(
                        value = state.targetText, onValueChange = viewModel::onTargetChange,
                        label = stringResource(R.string.goal_field_target), placeholder = stringResource(R.string.accounts_field_amount_hint),
                        errorText = state.targetError?.let { stringResource(it) }, enabled = enabled,
                        trailing = { Text("R$", style = type.label, color = colors.textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.testTag("field-goal-target"),
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("group-goal-account")) {
                        FieldLabel(stringResource(R.string.goal_field_account))
                        if (state.accountsLoaded && state.accounts.isEmpty()) {
                            Text(stringResource(R.string.goal_no_accounts), style = type.body, color = colors.textSecondary)
                            FluxaButton(text = stringResource(R.string.transaction_create_account), onClick = onCreateAccount, variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Plus)
                        } else {
                            FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.accounts.forEach { a ->
                                    FluxaChip(
                                        label = a.details.name, selected = state.accountId == a.id, onSelectedChange = { viewModel.onAccountChange(a.id) },
                                        leadingIcon = a.details.type.visual().icon, role = Role.RadioButton, enabled = enabled,
                                        modifier = Modifier.testTag("goal-account-${a.details.name}"),
                                    )
                                }
                            }
                        }
                        // Meta é reserva dentro da conta, não uma conta separada.
                        Row(
                            modifier = Modifier.fillMaxWidth().background(colors.surfaceSunken, FluxaTheme.shapes.medium).padding(12.dp).testTag("goal-account-explainer"),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(FluxaIcons.Wallet, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.goal_account_explainer), style = type.caption, color = colors.textSecondary)
                        }
                        state.accountError?.let { FieldError(stringResource(it)) }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val date = state.targetDate
                        if (date != null) {
                            DateField(
                                label = stringResource(R.string.goal_field_target_date), supporting = null, date = date,
                                enabled = enabled, onClick = { pickDate = true }, modifier = Modifier.testTag("field-goal-date"),
                            )
                            FluxaButton(
                                text = stringResource(R.string.goal_target_date_clear), onClick = { viewModel.onTargetDateChange(null) },
                                variant = FluxaButtonVariant.Secondary, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            FieldLabel(stringResource(R.string.goal_field_target_date))
                            FluxaButton(
                                text = stringResource(R.string.goal_target_date_set), onClick = { pickDate = true },
                                variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Calendar, enabled = enabled,
                                modifier = Modifier.fillMaxWidth().testTag("action-goal-date"),
                            )
                        }
                    }
                }
            }
        }
    }
    if (pickDate) {
        FluxaDatePicker(
            initial = state.targetDate ?: java.time.LocalDate.now().plusMonths(6),
            onPicked = { viewModel.onTargetDateChange(it); pickDate = false },
            onDismiss = { pickDate = false },
        )
    }
}

// endregion

// region Detalhe

@Composable
internal fun GoalDetailScreen(
    viewModel: GoalDetailViewModel,
    allocation: GoalAllocationViewModel,
    onBack: () -> Unit,
    onEdit: (UUID) -> Unit,
    onFeedback: (Int) -> Unit,
    onFeedbackText: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val move by allocation.uiState.collectAsStateWithLifecycle()
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var confirm by rememberSaveable { mutableStateOf<GoalAction?>(null) }
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val hidden = LocalAmountsHidden.current
    val allocatedHidden = stringResource(R.string.feedback_goal_allocated_hidden)
    val releasedHidden = stringResource(R.string.feedback_goal_released_hidden)
    val allocatedTemplate = stringResource(R.string.feedback_goal_allocated, "%s")
    val releasedTemplate = stringResource(R.string.feedback_goal_released, "%s")

    var lastAction by rememberSaveable { mutableIntStateOf(state.actionCount) }
    LaunchedEffect(state.actionCount) {
        if (state.actionCount > lastAction) {
            lastAction = state.actionCount
            onFeedback(if (state.lastAction == GoalAction.Complete) R.string.feedback_goal_completed else R.string.feedback_goal_archived)
        }
    }
    var lastMove by rememberSaveable { mutableIntStateOf(move.completedCount) }
    LaunchedEffect(move.completedCount) {
        if (move.completedCount > lastMove) {
            lastMove = move.completedCount
            showSheet = false
            // "R$ X foram reservados…" com o valor gravado pelo contrato; com valores ocultos, uma frase sem valor.
            val release = move.lastMode == MovementMode.Release
            val amount = move.lastAmount?.takeIf { !hidden }?.let { FluxaMoneyFormat.format(it) }
            onFeedbackText(
                if (amount == null) (if (release) releasedHidden else allocatedHidden)
                else (if (release) releasedTemplate else allocatedTemplate).format(amount),
            )
        }
    }

    val s = state.summary
    FeatureScreen(
        title = s?.goal?.details?.name ?: stringResource(R.string.goals_title),
        overline = stringResource(R.string.goals_title),
        onBack = onBack,
        modifier = Modifier.testTag("screen-goal-detail"),
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.loadError != null -> item { ErrorBanner(stringResource(state.loadError!!), onDismiss = null) }
            state.notFound || s == null -> item { ErrorBanner(stringResource(R.string.error_goal_not_found), onDismiss = null) }
            else -> {
                item { GoalHeroCard(s) }
                when (s.goal.status) {
                    GoalStatus.COMPLETED -> item { Note(stringResource(R.string.goal_completed_note), FluxaIcons.Check, "goal-completed-note") }
                    GoalStatus.ARCHIVED -> item { Note(stringResource(R.string.goal_archived_note), FluxaIcons.Archive, "goal-archived-note") }
                    GoalStatus.ACTIVE -> Unit
                }
                item {
                    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("goal-info")) {
                        InfoLine(stringResource(R.string.goal_label_account), s.account.details.name)
                        s.goal.details.targetDate?.let { InfoLine(stringResource(R.string.goal_label_target_date), it.short(), spoken = it.spoken()) }
                    }
                }
                state.reserves?.let { r -> item { AccountReserveCard(r) } }
                if (state.canAllocate) item {
                    FluxaButton(
                        text = stringResource(R.string.goal_action_allocate), onClick = { allocation.start(MovementMode.Allocate); showSheet = true },
                        size = FluxaButtonSize.Large, leadingIcon = FluxaIcons.Plus, modifier = Modifier.fillMaxWidth().testTag("action-allocate"),
                    )
                }
                if (state.canRelease) item {
                    FluxaButton(
                        text = stringResource(R.string.goal_action_release), onClick = { allocation.start(MovementMode.Release); showSheet = true },
                        variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Minus, modifier = Modifier.fillMaxWidth().testTag("action-release"),
                    )
                }
                item { FluxaSectionHeader(title = stringResource(R.string.goal_history)) }
                if (state.movements.isEmpty()) item {
                    Text(stringResource(R.string.goal_history_empty), style = type.body, color = colors.textSecondary)
                } else {
                    item { Text(stringResource(R.string.goal_history_hint), style = type.caption, color = colors.textMuted) }
                    item {
                        FluxaCard(modifier = Modifier.fillMaxWidth().testTag("goal-history"), contentPadding = PaddingValues(vertical = 4.dp)) {
                            state.movements.forEachIndexed { i, m ->
                                if (i > 0) RowDivider()
                                MovementRow(m.type, m.amount, m.occurredOn.short(), m.occurredOn.spoken())
                            }
                        }
                    }
                }
                // Erro de concluir/arquivar fica junto dos botões que o causaram (no topo, sairia da tela).
                state.actionError?.let { item { ErrorBanner(stringResource(it), onDismiss = viewModel::dismissError, modifier = Modifier.testTag("goal-action-error")) } }
                if (state.canEdit) item {
                    FluxaButton(
                        text = stringResource(R.string.goals_edit), onClick = { onEdit(s.goal.id) }, variant = FluxaButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth().testTag("action-edit-goal"),
                    )
                }
                val busy = state.actionStatus == CommandStatus.Submitting
                if (state.canComplete) {
                    item {
                        FluxaButton(
                            text = stringResource(R.string.goal_action_complete), onClick = { confirm = GoalAction.Complete },
                            variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Check, loading = busy && state.lastAction == GoalAction.Complete,
                            enabled = s.remainingAmount.cents <= 0,
                            modifier = Modifier.fillMaxWidth().testTag("action-complete-goal"),
                        )
                    }
                    if (s.remainingAmount.cents > 0) item { Text(stringResource(R.string.goal_complete_hint), style = type.caption, color = colors.textMuted) }
                }
                if (state.canArchive) item {
                    FluxaButton(
                        text = stringResource(R.string.goal_action_archive), onClick = { confirm = GoalAction.Archive },
                        variant = FluxaButtonVariant.Destructive, leadingIcon = FluxaIcons.Archive, loading = busy && state.lastAction == GoalAction.Archive,
                        modifier = Modifier.fillMaxWidth().testTag("action-archive-goal"),
                    )
                }
            }
        }
    }

    if (showSheet && s != null) {
        MovementSheet(s, state.reserves, move, allocation, onDismiss = { if (!move.isSubmitting) showSheet = false })
    }
    confirm?.let { action ->
        ConfirmArchiveSheet(
            title = stringResource(if (action == GoalAction.Complete) R.string.goal_complete_title else R.string.goal_archive_title),
            body = stringResource(if (action == GoalAction.Complete) R.string.goal_complete_body else R.string.goal_archive_body),
            confirmLabel = stringResource(if (action == GoalAction.Complete) R.string.goal_action_complete else R.string.goal_action_archive),
            dismissLabel = stringResource(R.string.goal_keep),
            destructive = action == GoalAction.Archive,
            confirmIcon = if (action == GoalAction.Complete) FluxaIcons.Check else FluxaIcons.Archive,
            onConfirm = { confirm = null; if (action == GoalAction.Complete) viewModel.complete() else viewModel.archive() },
            onDismiss = { confirm = null },
        )
    }
}

/** Cartão principal: reservado em destaque, objetivo, progresso e restante — todos do resumo do contrato. */
@Composable
private fun GoalHeroCard(s: GoalSummary) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    FluxaCard(
        modifier = Modifier.fillMaxWidth().testTag("goal-summary"),
        tone = FluxaSurfaceTone.Inverse, featured = true, contentPadding = PaddingValues(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.goal_label_reserved).uppercase(), style = type.overline, color = colors.onSurfaceInverseMuted, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.goal_progress_percent, s.percent), style = type.titleSmall, color = colors.onSurfaceInverse,
                modifier = Modifier.testTag("goal-percent"),
            )
        }
        FluxaAmount(amount = s.reservedAmount, style = type.amountHero, color = colors.onSurfaceInverse, modifier = Modifier.testTag("goal-reserved"))
        Spacer(Modifier.height(12.dp))
        GoalProgressBar(s.progressBasisPoints, onInverse = true)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}.testTag("goal-target")) {
                Text(stringResource(R.string.goal_label_target), style = type.caption, color = colors.onSurfaceInverseMuted)
                FluxaAmount(amount = s.targetAmount, style = type.bodyStrong, color = colors.onSurfaceInverse)
            }
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}.testTag("goal-remaining")) {
                Text(stringResource(R.string.goal_label_remaining), style = type.caption, color = colors.onSurfaceInverseMuted)
                FluxaAmount(amount = s.remainingAmount, style = type.bodyStrong, color = colors.onSurfaceInverse)
            }
        }
        GoalStatusPill(s.goal.status, modifier = Modifier.padding(top = 12.dp).testTag("goal-status"))
    }
}

/** Saldo real, reservado (todas as metas da conta) e disponível exatamente como o contrato entrega. */
@Composable
internal fun AccountReserveCard(r: AccountReserveSummary, modifier: Modifier = Modifier) {
    FluxaCard(modifier = modifier.fillMaxWidth().testTag("goal-account-reserves")) {
        Text(stringResource(R.string.goal_account_section).uppercase(), style = FluxaTheme.typography.overline, color = FluxaTheme.colors.textMuted)
        Spacer(Modifier.height(4.dp))
        ReserveLine(stringResource(R.string.goal_account_real), r.accountBalance, modifier = Modifier.testTag("reserve-real"))
        ReserveLine(stringResource(R.string.goal_account_reserved), r.reservedAmount, modifier = Modifier.testTag("reserve-reserved"))
        ReserveLine(
            stringResource(R.string.goal_account_available), r.availableAmount, modifier = Modifier.testTag("reserve-available"),
            color = if (r.availableAmount.isNegative) FluxaTheme.colors.expense else FluxaTheme.colors.textPrimary,
        )
        if (r.availableAmount.isNegative) NegativeAvailableAlert(stringResource(R.string.reserve_negative_alert), modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun MovementRow(type: GoalMovementType, amount: Money, date: String, spokenDate: String) {
    val colors = FluxaTheme.colors
    val allocate = type == GoalMovementType.ALLOCATE
    val label = stringResource(if (allocate) R.string.goal_movement_allocate else R.string.goal_movement_release)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("goal-movement-$label-$date"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(36.dp).background(if (allocate) colors.brandTonal else colors.surfaceSunken, FluxaTheme.shapes.pill), contentAlignment = Alignment.Center) {
            Icon(if (allocate) FluxaIcons.Plus else FluxaIcons.Minus, null, tint = if (allocate) colors.onBrandTonal else colors.textSecondary, modifier = Modifier.size(18.dp))
        }
        // Sinal = movimento da reserva (não receita/despesa).
        val value: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (allocate) "+" else "−", style = FluxaTheme.typography.amount, color = colors.textPrimary, modifier = Modifier.clearAndSetSemantics {})
                FluxaAmount(amount = amount, style = FluxaTheme.typography.amount, color = colors.textPrimary)
            }
        }
        val narrow = isNarrowForText()
        Column(Modifier.weight(1f)) {
            Text(label, style = FluxaTheme.typography.bodyStrong, color = colors.textPrimary)
            Text(date, style = FluxaTheme.typography.caption, color = colors.textSecondary, modifier = Modifier.semantics { contentDescription = spokenDate })
            if (narrow) value()
        }
        if (!narrow) value()
    }
}

@Composable
private fun Note(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FluxaTheme.colors.surfaceSunken, FluxaTheme.shapes.medium).padding(16.dp).testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = FluxaTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
        Text(text, style = FluxaTheme.typography.label, color = FluxaTheme.colors.textSecondary)
    }
}

/** Reservar ou liberar: mostra conta, saldo real, reservado, disponível e restante (todos do contrato). */
@Composable
private fun MovementSheet(
    s: GoalSummary,
    reserves: AccountReserveSummary?,
    state: GoalAllocationUiState,
    viewModel: GoalAllocationViewModel,
    onDismiss: () -> Unit,
) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val allocate = state.mode == MovementMode.Allocate
    FluxaBottomSheet(
        title = stringResource(if (allocate) R.string.goal_action_allocate else R.string.goal_action_release),
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("sheet-goal-movement"),
    ) {
        // Campo no topo; com teclado aberto a área rolável encolhe para o campo seguir visível (320×640 com fonte 1,3).
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val maxScroll = (LocalConfiguration.current.screenHeightDp.dp - 330.dp - imeBottom).coerceAtLeast(140.dp)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = maxScroll).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            FluxaInput(
                value = state.amountText, onValueChange = viewModel::onAmountChange,
                label = stringResource(if (allocate) R.string.goal_allocate_amount else R.string.goal_release_amount),
                placeholder = stringResource(R.string.accounts_field_amount_hint),
                errorText = state.amountError?.let { stringResource(it) },
                enabled = !state.isSubmitting,
                trailing = { Text("R$", style = type.label, color = colors.textMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.testTag("field-goal-amount"),
            )
            Text(stringResource(if (allocate) R.string.goal_allocate_note else R.string.goal_release_note), style = type.caption, color = colors.textSecondary)
            state.error?.let { ErrorBanner(stringResource(it), onDismiss = null) }
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.surfaceSunken, FluxaTheme.shapes.medium).padding(16.dp).testTag("movement-summary"),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(s.account.details.name, style = type.bodyStrong, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                reserves?.let {
                    ReserveLine(stringResource(R.string.goal_account_real), it.accountBalance)
                    ReserveLine(stringResource(R.string.goal_account_reserved), it.reservedAmount)
                    ReserveLine(stringResource(R.string.goal_account_available), it.availableAmount, color = if (it.availableAmount.isNegative) colors.expense else colors.textPrimary)
                }
                if (allocate) ReserveLine(stringResource(R.string.goal_label_remaining), CalculatedBalance.Exact(s.remainingAmount))
                else ReserveLine(stringResource(R.string.goal_label_reserved), CalculatedBalance.Exact(s.reservedAmount))
            }
          }
            FluxaButton(
                text = stringResource(if (allocate) R.string.goal_allocate_confirm else R.string.goal_release_confirm),
                onClick = viewModel::submit, size = FluxaButtonSize.Large, loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag("action-confirm-movement"),
            )
            FluxaButton(
                text = stringResource(R.string.action_back), onClick = onDismiss, variant = FluxaButtonVariant.Secondary,
                enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// endregion
