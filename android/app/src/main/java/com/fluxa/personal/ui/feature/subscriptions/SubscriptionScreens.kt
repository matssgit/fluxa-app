package com.fluxa.personal.ui.feature.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.domain.model.Subscription
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionOccurrence
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionStatus
import com.fluxa.personal.ui.common.BrazilLocale
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
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonSize
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaChip
import com.fluxa.personal.ui.designsystem.components.FluxaInput
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.feature.catalog.categoryIconFor
import com.fluxa.personal.ui.feature.credit.CommandStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/** "17 dez. 2026" — data curta e legível; o TalkBack lê o mês por extenso. */
internal val ShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", BrazilLocale)
private val SpokenDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", BrazilLocale)

internal fun LocalDate.short(): String = format(ShortDate)
internal fun LocalDate.spoken(): String = format(SpokenDate)

// region Componentes compartilhados

@Composable
internal fun frequencyLabel(frequency: SubscriptionFrequency, long: Boolean = false): String = stringResource(
    when (frequency) {
        SubscriptionFrequency.MONTHLY -> if (long) R.string.subscription_frequency_monthly_long else R.string.subscription_frequency_monthly
        SubscriptionFrequency.YEARLY -> if (long) R.string.subscription_frequency_yearly_long else R.string.subscription_frequency_yearly
    },
)

@Composable
internal fun sourceText(source: SourceLabel?): String {
    val name = source?.name ?: stringResource(R.string.subscription_source_unknown)
    return stringResource(if (source?.isCard == true) R.string.subscription_source_card else R.string.subscription_source_account, name)
}

@Composable
internal fun SubscriptionStatusPill(status: SubscriptionStatus, modifier: Modifier = Modifier) {
    val (label, icon) = when (status) {
        SubscriptionStatus.ACTIVE -> stringResource(R.string.subscription_status_active) to FluxaIcons.Check
        SubscriptionStatus.PAUSED -> stringResource(R.string.subscription_status_paused) to FluxaIcons.Calendar
        SubscriptionStatus.CANCELLED -> stringResource(R.string.subscription_status_cancelled) to FluxaIcons.Close
    }
    val described = stringResource(R.string.subscription_status_described, label)
    StatePill(label, icon, modifier = modifier.semantics { stateDescription = described })
}

internal data class OccurrenceStatusVisual(val label: Int, val icon: ImageVector)

internal fun SubscriptionOccurrenceStatus.visual() = when (this) {
    SubscriptionOccurrenceStatus.PENDING -> OccurrenceStatusVisual(R.string.occurrence_status_pending, FluxaIcons.Calendar)
    SubscriptionOccurrenceStatus.OVERDUE -> OccurrenceStatusVisual(R.string.occurrence_status_overdue, FluxaIcons.Alert)
    SubscriptionOccurrenceStatus.PAID -> OccurrenceStatusVisual(R.string.occurrence_status_paid, FluxaIcons.Check)
    SubscriptionOccurrenceStatus.CHARGED -> OccurrenceStatusVisual(R.string.occurrence_status_charged, FluxaIcons.Card)
    SubscriptionOccurrenceStatus.SKIPPED -> OccurrenceStatusVisual(R.string.occurrence_status_skipped, FluxaIcons.ChevronRight)
}

@Composable
internal fun OccurrenceStatusPill(status: SubscriptionOccurrenceStatus, modifier: Modifier = Modifier) {
    val v = status.visual()
    val label = stringResource(v.label)
    val described = stringResource(R.string.subscription_status_described, label)
    StatePill(label, v.icon, modifier = modifier.semantics { stateDescription = described })
}

/** Linha de ocorrência: assinatura, valor, data, fonte e estado (texto + ícone). */
@Composable
internal fun OccurrenceRow(occurrence: SubscriptionOccurrence, source: SourceLabel, onClick: () -> Unit, showDate: Boolean = true) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val d = occurrence.details
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("occurrence-${d.name}-${occurrence.key.occurrenceDate}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(d.name, style = type.bodyStrong, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (showDate) Text(occurrence.key.occurrenceDate.short(), style = type.caption, color = colors.textSecondary)
            Text(sourceText(source), style = type.caption, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            OccurrenceStatusPill(occurrence.status, modifier = Modifier.padding(top = 4.dp))
        }
        FluxaAmount(amount = d.amount, style = type.amount)
        Icon(FluxaIcons.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun RowDivider() {
    Spacer(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(FluxaTheme.colors.border))
}

/** Par rótulo/valor lido como uma unidade pelo TalkBack. */
@Composable
internal fun InfoLine(label: String, value: String, modifier: Modifier = Modifier, spoken: String? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp).semantics(mergeDescendants = true) {
            if (spoken != null) stateDescription = spoken
        },
    ) {
        Text(label, style = FluxaTheme.typography.label, color = FluxaTheme.colors.textSecondary)
        Text(value, style = FluxaTheme.typography.bodyStrong, color = FluxaTheme.colors.textPrimary)
    }
}

// endregion

// region Lista

@Composable
internal fun SubscriptionsScreen(
    viewModel: SubscriptionsViewModel,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onOpen: (UUID) -> Unit,
    onAgenda: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(
        title = stringResource(R.string.subscriptions_title),
        overline = stringResource(R.string.subscriptions_overline),
        onBack = onBack,
        modifier = Modifier.testTag("screen-subscriptions"),
    ) {
        // Empilhados: lado a lado, "Nova assinatura" cortava em 360dp.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FluxaButton(
                    text = stringResource(R.string.subscriptions_new), onClick = onNew, leadingIcon = FluxaIcons.Plus,
                    modifier = Modifier.fillMaxWidth().testTag("action-new-subscription"),
                )
                FluxaButton(
                    text = stringResource(R.string.subscriptions_agenda), onClick = onAgenda, variant = FluxaButtonVariant.Secondary,
                    leadingIcon = FluxaIcons.Calendar, modifier = Modifier.fillMaxWidth().testTag("action-agenda"),
                )
            }
        }
        when {
            state.isLoading -> item { LoadingBlock() }
            state.errorMessage != null -> item { ErrorBanner(stringResource(state.errorMessage!!), onDismiss = null) }
            state.isEmpty -> item {
                EmptyStateCard(stringResource(R.string.subscriptions_empty_title), stringResource(R.string.subscriptions_empty_body), FluxaIcons.Repeat)
            }
            else -> {
                if (state.overdueCount > 0) item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(FluxaTheme.colors.expenseContainer, FluxaTheme.shapes.medium)
                            .clickable(role = Role.Button, onClick = onAgenda)
                            .heightIn(min = 48.dp)
                            .padding(16.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("overdue-banner"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(FluxaIcons.Alert, null, tint = FluxaTheme.colors.expense, modifier = Modifier.size(20.dp))
                        Text(
                            pluralStringResource(R.plurals.subscriptions_overdue_banner, state.overdueCount, state.overdueCount),
                            style = FluxaTheme.typography.label, color = FluxaTheme.colors.expense, modifier = Modifier.weight(1f),
                        )
                    }
                }
                section(R.string.subscriptions_section_active, state.active, onOpen)
                section(R.string.subscriptions_section_paused, state.paused, onOpen)
                section(R.string.subscriptions_section_cancelled, state.cancelled, onOpen)
            }
        }
    }
}

private fun LazyListScope.section(title: Int, list: List<SubscriptionListItem>, onOpen: (UUID) -> Unit) {
    if (list.isEmpty()) return
    item { FluxaSectionHeader(title = androidx.compose.ui.res.stringResource(title)) }
    items(list, key = { it.subscription.id }) { SubscriptionCard(it, onClick = { onOpen(it.subscription.id) }) }
}

@Composable
internal fun SubscriptionCard(item: SubscriptionListItem, onClick: () -> Unit) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val s = item.subscription
    val cancelled = s.status == SubscriptionStatus.CANCELLED
    FluxaCard(
        modifier = Modifier.fillMaxWidth().testTag("subscription-${s.details.name}"),
        onClick = onClick,
        contentPadding = PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(s.details.name, style = type.titleSmall, color = if (cancelled) colors.textSecondary else colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(frequencyLabel(s.frequency, long = true), style = type.caption, color = colors.textSecondary)
            }
            FluxaAmount(amount = s.details.amount, style = type.amount, color = if (cancelled) colors.textMuted else colors.textPrimary)
        }
        Spacer(Modifier.height(10.dp))
        NextLine(s, item.next)
        Text(sourceText(item.source), style = type.caption, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        SubscriptionStatusPill(s.status, modifier = Modifier.padding(top = 8.dp))
    }
}

/** "Próxima cobrança · 17 dez. 2026" ou o motivo de não haver próxima (pausa/cancelamento). Vem do contrato. */
@Composable
internal fun NextLine(subscription: Subscription, next: SubscriptionOccurrence?) {
    val type = FluxaTheme.typography
    val colors = FluxaTheme.colors
    val label = stringResource(R.string.subscription_next_label)
    val value = when {
        subscription.status == SubscriptionStatus.CANCELLED -> stringResource(R.string.subscription_next_none_cancelled)
        subscription.status == SubscriptionStatus.PAUSED -> stringResource(R.string.subscription_next_none_paused)
        next == null -> stringResource(R.string.subscription_next_none)
        else -> null
    }
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("next-${subscription.details.name}")) {
        Text(label, style = type.overline, color = colors.textMuted)
        if (value != null) Text(value, style = type.label, color = colors.textSecondary)
        else Text(
            next!!.key.occurrenceDate.short(), style = type.bodyStrong, color = colors.textPrimary,
            modifier = Modifier.semantics { stateDescription = next.key.occurrenceDate.spoken() },
        )
    }
}

// endregion

// region Formulário

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SubscriptionFormScreen(
    viewModel: SubscriptionFormViewModel,
    onDone: (SubscriptionCompletion?) -> Unit,
    onCreateCategory: () -> Unit,
    onCreateAccount: () -> Unit,
    onCreateCard: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pickAnchor by rememberSaveable { mutableStateOf(false) }
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    LaunchedEffect(state.completion) { state.completion?.let(onDone) }
    val enabled = !state.isSaving && !state.isReadOnly
    FeatureScreen(
        title = stringResource(if (state.isEditing) R.string.subscriptions_edit else R.string.subscriptions_new),
        overline = stringResource(R.string.subscriptions_title),
        onBack = { onDone(null) },
        modifier = Modifier.testTag("screen-subscription-form"),
        bottomAction = if (state.isLoading || state.isReadOnly) null else {
            {
                state.firstError?.let {
                    FieldError(stringResource(it), modifier = Modifier.padding(bottom = 8.dp).semantics { liveRegion = LiveRegionMode.Assertive }.testTag("subscription-form-error"))
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
            state.notFound -> item { ErrorBanner(stringResource(R.string.error_subscription_not_found), onDismiss = null) }
            else -> {
                if (state.isReadOnly) item { ErrorBanner(stringResource(R.string.subscription_cancelled_note), onDismiss = null) }
                state.operationError?.let { item { ErrorBanner(stringResource(it), onDismiss = viewModel::dismissError) } }
                if (state.isEditing) item {
                    Text(stringResource(R.string.subscription_edit_future_note), style = type.caption, color = colors.textSecondary, modifier = Modifier.testTag("edit-future-note"))
                }
                item {
                    FluxaInput(
                        value = state.name, onValueChange = viewModel::onNameChange,
                        label = stringResource(R.string.subscription_field_name), placeholder = stringResource(R.string.subscription_field_name_hint),
                        errorText = state.nameError?.let { stringResource(it) }, enabled = enabled,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        modifier = Modifier.testTag("field-subscription-name"),
                    )
                }
                item {
                    FluxaInput(
                        value = state.amountText, onValueChange = viewModel::onAmountChange,
                        label = stringResource(R.string.subscription_field_amount), placeholder = stringResource(R.string.accounts_field_amount_hint),
                        errorText = state.amountError?.let { stringResource(it) }, enabled = enabled,
                        trailing = { Text("R$", style = type.label, color = colors.textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        modifier = Modifier.testTag("field-subscription-amount"),
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FieldLabel(stringResource(R.string.subscription_field_frequency))
                        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubscriptionFrequency.entries.forEach { f ->
                                FluxaChip(
                                    label = frequencyLabel(f), selected = state.frequency == f,
                                    onSelectedChange = { viewModel.onFrequencyChange(f) }, role = Role.RadioButton,
                                    enabled = enabled && !state.scheduleLocked, modifier = Modifier.testTag("frequency-${f.name}"),
                                )
                            }
                        }
                        if (state.frequency == SubscriptionFrequency.YEARLY) {
                            Text(stringResource(R.string.subscription_yearly_note), style = type.caption, color = colors.textSecondary, modifier = Modifier.testTag("yearly-note"))
                        }
                        if (state.scheduleLocked) Text(stringResource(R.string.subscription_schedule_locked), style = type.caption, color = colors.textMuted)
                    }
                }
                item {
                    DateField(
                        label = stringResource(R.string.subscription_field_anchor),
                        supporting = stringResource(R.string.subscription_field_anchor_support),
                        date = state.anchorDate, enabled = enabled && !state.scheduleLocked,
                        onClick = { pickAnchor = true }, modifier = Modifier.testTag("field-subscription-anchor"),
                    )
                }
                if (state.frequency == SubscriptionFrequency.MONTHLY) item {
                    FluxaInput(
                        value = state.billingDayText, onValueChange = viewModel::onBillingDayChange,
                        label = stringResource(R.string.subscription_field_billing_day),
                        supportingText = stringResource(R.string.subscription_billing_day_support),
                        errorText = state.billingDayError?.let { stringResource(it) }, enabled = enabled && !state.scheduleLocked,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        modifier = Modifier.testTag("field-subscription-day"),
                    )
                } else state.billingDayError?.let { item { FieldError(stringResource(it)) } }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("group-subscription-category")) {
                        FieldLabel(stringResource(R.string.subscription_field_category))
                        if (state.loaded && state.categories.isEmpty()) {
                            Text(stringResource(R.string.transaction_no_categories_expense), style = type.body, color = colors.textSecondary)
                            FluxaButton(
                                text = stringResource(R.string.transaction_create_category), onClick = onCreateCategory,
                                variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Plus,
                            )
                        } else {
                            FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.categories.forEach { c ->
                                    FluxaChip(
                                        label = c.details.name, selected = state.categoryId == c.id,
                                        onSelectedChange = { viewModel.onCategoryChange(c.id) }, leadingIcon = categoryIconFor(c.details.icon),
                                        role = Role.RadioButton, enabled = enabled, modifier = Modifier.testTag("subscription-category-${c.details.name}"),
                                    )
                                }
                            }
                        }
                        state.categoryError?.let { FieldError(stringResource(it)) }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("group-subscription-source")) {
                        FieldLabel(stringResource(R.string.subscription_field_source))
                        Row(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FluxaChip(
                                label = stringResource(R.string.subscription_source_kind_account), selected = state.sourceKind == SourceKind.ACCOUNT,
                                onSelectedChange = { viewModel.onSourceKindChange(SourceKind.ACCOUNT) }, leadingIcon = FluxaIcons.Bank,
                                role = Role.RadioButton, enabled = enabled, modifier = Modifier.testTag("source-kind-account"),
                            )
                            FluxaChip(
                                label = stringResource(R.string.subscription_source_kind_card), selected = state.sourceKind == SourceKind.CARD,
                                onSelectedChange = { viewModel.onSourceKindChange(SourceKind.CARD) }, leadingIcon = FluxaIcons.Card,
                                role = Role.RadioButton, enabled = enabled, modifier = Modifier.testTag("source-kind-card"),
                            )
                        }
                        // Só a lista do tipo escolhido aparece: conta OU cartão, nunca os dois.
                        FlowRow(modifier = Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.sourceKind == SourceKind.ACCOUNT) {
                                state.accounts.forEach { a ->
                                    FluxaChip(
                                        label = a.details.name, selected = state.accountId == a.id, onSelectedChange = { viewModel.onAccountChange(a.id) },
                                        role = Role.RadioButton, enabled = enabled, modifier = Modifier.testTag("subscription-account-${a.details.name}"),
                                    )
                                }
                            } else {
                                state.cards.forEach { c ->
                                    FluxaChip(
                                        label = c.card.details.name, selected = state.cardId == c.card.id, onSelectedChange = { viewModel.onCardChange(c.card.id) },
                                        role = Role.RadioButton, enabled = enabled, modifier = Modifier.testTag("subscription-card-${c.card.details.name}"),
                                    )
                                }
                            }
                        }
                        if (state.loaded && state.sourceKind == SourceKind.ACCOUNT && state.accounts.isEmpty()) {
                            Text(stringResource(R.string.subscription_no_accounts), style = type.body, color = colors.textSecondary)
                            FluxaButton(text = stringResource(R.string.transaction_create_account), onClick = onCreateAccount, variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Plus)
                        }
                        if (state.loaded && state.sourceKind == SourceKind.CARD && state.cards.isEmpty()) {
                            Text(stringResource(R.string.subscription_no_cards), style = type.body, color = colors.textSecondary)
                            FluxaButton(text = stringResource(R.string.card_new), onClick = onCreateCard, variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Plus)
                        }
                        state.sourceError?.let { FieldError(stringResource(it)) }
                    }
                }
            }
        }
    }
    if (pickAnchor) {
        FluxaDatePicker(initial = state.anchorDate, onPicked = { viewModel.onAnchorChange(it); pickAnchor = false }, onDismiss = { pickAnchor = false })
    }
}

// endregion

// region Detalhe

@Composable
internal fun SubscriptionDetailScreen(
    viewModel: SubscriptionDetailViewModel,
    onBack: () -> Unit,
    onEdit: (UUID) -> Unit,
    onOpenOccurrence: (UUID, LocalDate) -> Unit,
    onAgenda: () -> Unit,
    onFeedback: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirm by rememberSaveable { mutableStateOf<StatusAction?>(null) }
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    var lastCount by rememberSaveable { mutableIntStateOf(state.actionCount) }
    LaunchedEffect(state.actionCount) {
        if (state.actionCount > lastCount) {
            lastCount = state.actionCount
            onFeedback(
                when (state.lastAction) {
                    StatusAction.Pause -> R.string.feedback_subscription_paused
                    StatusAction.Resume -> R.string.feedback_subscription_resumed
                    else -> R.string.feedback_subscription_cancelled
                },
            )
        }
    }
    val s = state.subscription
    FeatureScreen(
        title = s?.details?.name ?: stringResource(R.string.subscriptions_title),
        overline = stringResource(R.string.subscriptions_title),
        onBack = onBack,
        modifier = Modifier.testTag("screen-subscription-detail"),
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.loadError != null -> item { ErrorBanner(stringResource(state.loadError!!), onDismiss = null) }
            state.notFound || s == null -> item { ErrorBanner(stringResource(R.string.error_subscription_not_found), onDismiss = null) }
            else -> {
                state.actionError?.let { item { ErrorBanner(stringResource(it), onDismiss = viewModel::dismissError, modifier = Modifier.testTag("subscription-action-error")) } }
                item {
                    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("subscription-summary")) {
                        Text(frequencyLabel(s.frequency, long = true).uppercase(), style = type.overline, color = colors.textMuted, modifier = Modifier.testTag("subscription-frequency"))
                        FluxaAmount(amount = s.details.amount, style = type.headline.merge(type.amount).copy(fontSize = type.headline.fontSize))
                        SubscriptionStatusPill(s.status, modifier = Modifier.padding(top = 4.dp).testTag("subscription-status"))
                        Spacer(Modifier.height(8.dp))
                        NextLine(s, state.next)
                        InfoLine(stringResource(R.string.subscription_field_source), sourceText(state.source), modifier = Modifier.testTag("subscription-source"))
                        state.category?.let { InfoLine(stringResource(R.string.managed_field_category), it.details.name) }
                        InfoLine(stringResource(R.string.subscription_field_anchor), s.anchorDate.short(), spoken = s.anchorDate.spoken())
                    }
                }
                if (s.status == SubscriptionStatus.CANCELLED) item {
                    Text(stringResource(R.string.subscription_cancelled_note), style = type.label, color = colors.textSecondary, modifier = Modifier.testTag("subscription-cancelled-note"))
                }
                if (state.open.isNotEmpty()) {
                    item { FluxaSectionHeader(title = stringResource(R.string.subscription_detail_open)) }
                    item {
                        FluxaCard(modifier = Modifier.fillMaxWidth().testTag("subscription-open"), contentPadding = PaddingValues(vertical = 4.dp)) {
                            state.open.forEachIndexed { i, o ->
                                if (i > 0) RowDivider()
                                OccurrenceRow(o, state.names.label(o.details.source), onClick = { onOpenOccurrence(s.id, o.key.occurrenceDate) })
                            }
                        }
                    }
                }
                state.next?.let { next ->
                    item {
                        FluxaButton(
                            text = stringResource(R.string.subscription_next_label) + " · " + next.key.occurrenceDate.short(),
                            onClick = { onOpenOccurrence(s.id, next.key.occurrenceDate) },
                            variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Calendar,
                            modifier = Modifier.fillMaxWidth().testTag("action-open-next"),
                        )
                    }
                }
                item { FluxaSectionHeader(title = stringResource(R.string.subscription_detail_history)) }
                if (state.history.isEmpty()) item {
                    Text(stringResource(R.string.subscription_detail_history_empty), style = type.body, color = colors.textSecondary)
                } else item {
                    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("subscription-history"), contentPadding = PaddingValues(vertical = 4.dp)) {
                        state.history.take(HISTORY_SHOWN).forEachIndexed { i, o ->
                            if (i > 0) RowDivider()
                            OccurrenceRow(o, state.names.label(o.details.source), onClick = { onOpenOccurrence(s.id, o.key.occurrenceDate) })
                        }
                    }
                }
                item {
                    FluxaButton(
                        text = stringResource(R.string.subscriptions_open_agenda), onClick = onAgenda, variant = FluxaButtonVariant.Secondary,
                        leadingIcon = FluxaIcons.Calendar, modifier = Modifier.fillMaxWidth().testTag("action-subscription-agenda"),
                    )
                }
                if (state.canEdit) item {
                    FluxaButton(
                        text = stringResource(R.string.subscriptions_edit), onClick = { onEdit(s.id) }, variant = FluxaButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth().testTag("action-edit-subscription"),
                    )
                }
                val busy = state.actionStatus == CommandStatus.Submitting
                if (state.canPause) item {
                    FluxaButton(
                        text = stringResource(R.string.subscription_action_pause), onClick = { confirm = StatusAction.Pause },
                        variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Calendar, loading = busy && state.lastAction == StatusAction.Pause,
                        modifier = Modifier.fillMaxWidth().testTag("action-pause"),
                    )
                }
                if (state.canResume) item {
                    FluxaButton(
                        text = stringResource(R.string.subscription_action_resume), onClick = { confirm = StatusAction.Resume },
                        leadingIcon = FluxaIcons.Restore, loading = busy && state.lastAction == StatusAction.Resume,
                        modifier = Modifier.fillMaxWidth().testTag("action-resume"),
                    )
                }
                if (state.canCancel) item {
                    FluxaButton(
                        text = stringResource(R.string.subscription_action_cancel), onClick = { confirm = StatusAction.Cancel },
                        variant = FluxaButtonVariant.Destructive, leadingIcon = FluxaIcons.Close, loading = busy && state.lastAction == StatusAction.Cancel,
                        modifier = Modifier.fillMaxWidth().testTag("action-cancel-subscription"),
                    )
                }
            }
        }
    }
    confirm?.let { action ->
        val (title, body, label, icon) = when (action) {
            StatusAction.Pause -> Quad(R.string.subscription_pause_title, R.string.subscription_pause_body, R.string.subscription_action_pause, FluxaIcons.Calendar)
            StatusAction.Resume -> Quad(R.string.subscription_resume_title, R.string.subscription_resume_body, R.string.subscription_action_resume, FluxaIcons.Restore)
            StatusAction.Cancel -> Quad(R.string.subscription_cancel_title, R.string.subscription_cancel_body, R.string.subscription_action_cancel, FluxaIcons.Close)
        }
        ConfirmArchiveSheet(
            title = stringResource(title), body = stringResource(body), confirmLabel = stringResource(label),
            dismissLabel = stringResource(R.string.subscription_keep), confirmIcon = icon,
            onConfirm = {
                confirm = null
                when (action) {
                    StatusAction.Pause -> viewModel.pause()
                    StatusAction.Resume -> viewModel.resume()
                    StatusAction.Cancel -> viewModel.cancel()
                }
            },
            onDismiss = { confirm = null },
        )
    }
}

private data class Quad(val title: Int, val body: Int, val label: Int, val icon: ImageVector)

private const val HISTORY_SHOWN = 12

// endregion

// region Agenda

@Composable
internal fun SubscriptionAgendaScreen(viewModel: SubscriptionAgendaViewModel, onBack: () -> Unit, onOpenOccurrence: (UUID, LocalDate) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(
        title = stringResource(R.string.subscriptions_agenda),
        overline = stringResource(R.string.subscriptions_title),
        onBack = onBack,
        modifier = Modifier.testTag("screen-subscription-agenda"),
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.errorMessage != null -> item { ErrorBanner(stringResource(state.errorMessage!!), onDismiss = null) }
            state.isEmpty -> item { EmptyStateCard(stringResource(R.string.subscriptions_agenda), stringResource(R.string.agenda_empty), FluxaIcons.Calendar) }
            else -> {
                agendaSection(R.string.agenda_overdue, "agenda-overdue", state.overdue, state.names, onOpenOccurrence)
                agendaSection(R.string.agenda_upcoming, "agenda-upcoming", state.upcoming, state.names, onOpenOccurrence)
                agendaSection(R.string.agenda_resolved, "agenda-resolved", state.resolved, state.names, onOpenOccurrence)
            }
        }
    }
}

private fun LazyListScope.agendaSection(title: Int, tag: String, days: List<AgendaDay>, names: SourceNames, onOpen: (UUID, LocalDate) -> Unit) {
    if (days.isEmpty()) return
    item { FluxaSectionHeader(title = androidx.compose.ui.res.stringResource(title), modifier = Modifier.testTag(tag)) }
    items(days, key = { "$tag-${it.date}" }) { day ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                day.date.short(), style = FluxaTheme.typography.label, color = FluxaTheme.colors.textSecondary,
                modifier = Modifier.padding(start = 4.dp).semantics { heading(); stateDescription = day.date.spoken() },
            )
            FluxaCard(modifier = Modifier.fillMaxWidth().testTag("$tag-${day.date}"), contentPadding = PaddingValues(vertical = 4.dp)) {
                day.items.forEachIndexed { i, o ->
                    if (i > 0) RowDivider()
                    OccurrenceRow(o, names.label(o.details.source), onClick = { onOpen(o.key.subscriptionId, o.key.occurrenceDate) }, showDate = false)
                }
            }
        }
    }
}

// endregion
