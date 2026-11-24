package com.fluxa.personal.ui.feature.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionStatus
import com.fluxa.personal.domain.model.SubscriptionResolutionDetail
import com.fluxa.personal.ui.common.ConfirmArchiveSheet
import com.fluxa.personal.ui.common.DateField
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.FluxaDatePicker
import com.fluxa.personal.ui.common.LoadingBlock
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.designsystem.components.FluxaBottomSheet
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonSize
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import java.util.UUID

/**
 * Uma cobrança de assinatura. Em aberto e não futura: "Pagar" (fonte conta) ou "Lançar no cartão" (fonte cartão),
 * com resumo e data. A ocorrência resolvida oferece acesso ao efeito financeiro e confirmação de estorno.
 */
@Composable
internal fun SubscriptionOccurrenceScreen(
    viewModel: SubscriptionOccurrenceViewModel,
    onBack: () -> Unit,
    onFeedback: (Int) -> Unit,
    onOpenTransaction: (UUID) -> Unit,
    onOpenPurchase: (cardId: UUID, purchaseId: UUID) -> Unit,
    onOpenInstallment: (cardId: UUID, purchaseId: UUID, installmentId: UUID) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettle by rememberSaveable { mutableStateOf(false) }
    var confirmSkip by rememberSaveable { mutableStateOf(false) }
    val undoFocus = remember { FocusRequester() }
    var restoreUndoFocus by remember { mutableStateOf(false) }
    LaunchedEffect(state.notFound) {
        if (state.notFound) onBack()

    }
    val destructiveAction = stringResource(R.string.occurrence_destructive_action)
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    var lastCount by rememberSaveable { mutableIntStateOf(state.completedCount) }
    LaunchedEffect(state.completedCount) {
        if (state.completedCount > lastCount) {
            lastCount = state.completedCount
            showSettle = false
            onFeedback(
                when (state.outcome) {
                    OccurrenceOutcome.Paid -> R.string.feedback_occurrence_paid
                    OccurrenceOutcome.Charged -> R.string.feedback_occurrence_charged
                    OccurrenceOutcome.Skipped -> R.string.feedback_occurrence_skipped
                    OccurrenceOutcome.Reopened -> R.string.feedback_occurrence_reopened
                    else -> R.string.feedback_occurrence_already_resolved
                },
            )
        }
    }
    val occ = state.occurrence
    FeatureScreen(
        title = occ?.details?.name ?: stringResource(R.string.occurrence_title),
        overline = stringResource(R.string.occurrence_title),
        onBack = onBack,
        modifier = Modifier.testTag("screen-occurrence"),
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.notFound || occ == null -> item { ErrorBanner(stringResource(R.string.error_subscription_occurrence), onDismiss = null) }
            else -> {
                val d = occ.details
                // Erro geral só quando o sheet não está aberto (lá ele aparece junto do botão).
                if (!showSettle) state.error?.let { item { ErrorBanner(stringResource(it), onDismiss = viewModel::dismissError, modifier = Modifier.testTag("occurrence-error")) } }
                item {
                    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("occurrence-summary")) {
                        Text(stringResource(R.string.occurrence_field_amount).uppercase(), style = type.overline, color = colors.textMuted)
                        FluxaAmount(amount = d.amount, style = type.headline.merge(type.amount).copy(fontSize = type.headline.fontSize))
                        OccurrenceStatusPill(occ.status, modifier = Modifier.padding(top = 4.dp).testTag("occurrence-status"))
                        InfoLine(stringResource(R.string.occurrence_field_date), occ.key.occurrenceDate.short(), spoken = occ.key.occurrenceDate.spoken())
                        InfoLine(stringResource(R.string.occurrence_field_source), sourceText(state.source), modifier = Modifier.testTag("occurrence-source"))
                        state.resolution?.let {
                            InfoLine(
                                stringResource(if (it is SubscriptionResolutionDetail.Card) R.string.occurrence_charge_date else R.string.occurrence_pay_date),
                                it.occurredOn.short(),
                                spoken = it.occurredOn.spoken(),
                            )
                        }
                        state.subscription?.let { InfoLine(stringResource(R.string.subscription_field_frequency), frequencyLabel(it.frequency, long = true)) }
                        Text(stringResource(R.string.occurrence_terms_note), style = type.caption, color = colors.textMuted, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                val open = occ.status == SubscriptionOccurrenceStatus.PENDING || occ.status == SubscriptionOccurrenceStatus.OVERDUE
                if (open) {
                    val sub = state.subscription
                    when {
                        sub?.status == SubscriptionStatus.PAUSED -> item { Note(stringResource(R.string.occurrence_paused_note), "occurrence-paused-note") }
                        sub?.status == SubscriptionStatus.CANCELLED -> item { Note(stringResource(R.string.occurrence_cancelled_note), "occurrence-cancelled-note") }
                        state.isFuture -> item { Note(stringResource(R.string.occurrence_future_note, occ.key.occurrenceDate.short()), "occurrence-future-note") }
                    }
                    if (state.canSettle) item {
                        FluxaButton(
                            text = stringResource(if (state.isCard) R.string.occurrence_action_charge else R.string.occurrence_action_pay),
                            onClick = { viewModel.dismissError(); showSettle = true },
                            size = FluxaButtonSize.Large,
                            leadingIcon = if (state.isCard) FluxaIcons.Card else FluxaIcons.Check,
                            modifier = Modifier.fillMaxWidth().testTag("action-settle-occurrence"),
                        )
                    }
                    if (state.canSkip) item {
                        FluxaButton(
                            text = stringResource(R.string.occurrence_action_skip), onClick = { confirmSkip = true },
                            variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Skip,
                            loading = state.isSubmitting && !showSettle,
                            modifier = Modifier.fillMaxWidth().testTag("action-skip-occurrence"),
                        )
                    }
                } else {
                    val resolution = state.resolution
                    if (resolution is SubscriptionResolutionDetail.Account) item {
                        FluxaButton(
                            text = stringResource(R.string.occurrence_view_transaction), onClick = { onOpenTransaction(resolution.transactionId) },
                            variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Cash,
                            modifier = Modifier.fillMaxWidth().testTag("action-view-transaction"),
                        )
                    }
                    val card = d.source as? SubscriptionSource.Card
                    if (resolution is SubscriptionResolutionDetail.Card && card != null) item {
                        FluxaButton(
                            text = stringResource(R.string.occurrence_view_purchase), onClick = { onOpenPurchase(card.id, resolution.purchaseId) },
                            variant = FluxaButtonVariant.Secondary, leadingIcon = FluxaIcons.Card,
                            modifier = Modifier.fillMaxWidth().testTag("action-view-purchase"),
                        )
                    }
                    if (resolution is SubscriptionResolutionDetail.Card && card != null) item {
                        FluxaButton(
                            text = stringResource(R.string.occurrence_view_installment),
                            onClick = { onOpenInstallment(card.id, resolution.purchaseId, resolution.installmentId) },
                            variant = FluxaButtonVariant.Secondary,
                            modifier = Modifier.fillMaxWidth().testTag("action-view-installment"),
                        )
                    }
                    state.undoTarget?.let { target ->
                        item {
                            LaunchedEffect(state.undoConfirmation) {
                                if (restoreUndoFocus && state.undoConfirmation == null) {
                                    restoreUndoFocus = false
                                    undoFocus.requestFocus()
                                }
                            }
                            FluxaButton(
                                text = stringResource(undoLabel(target.isCard)),
                                onClick = { viewModel.requestUndo(target) },
                                variant = FluxaButtonVariant.Destructive,
                                size = FluxaButtonSize.Large,
                                loading = state.isSubmitting,
                                modifier = Modifier.fillMaxWidth().focusRequester(undoFocus)
                                    .semantics { stateDescription = destructiveAction }.testTag("action-undo-occurrence"),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettle && occ != null) {
        var pickDate by rememberSaveable { mutableStateOf(false) }
        FluxaBottomSheet(
            title = stringResource(if (state.isCard) R.string.occurrence_charge_title else R.string.occurrence_pay_title),
            onDismissRequest = { if (!state.isSubmitting) showSettle = false },
            modifier = Modifier.testTag("sheet-settle-occurrence"),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.surfaceSunken, FluxaTheme.shapes.medium).padding(16.dp)
                    .semantics(mergeDescendants = true) {}.testTag("settle-summary"),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(occ.details.name, style = type.bodyStrong, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.occurrence_field_date) + ": " + occ.key.occurrenceDate.short(), style = type.label, color = colors.textSecondary)
                FluxaAmount(amount = occ.details.amount, style = type.title.merge(type.amount))
                Text(sourceText(state.source), style = type.caption, color = colors.textSecondary)
            }
            if (state.isCard) Text(stringResource(R.string.occurrence_charge_hint), style = type.caption, color = colors.textSecondary)
            DateField(
                label = stringResource(if (state.isCard) R.string.occurrence_charge_date else R.string.occurrence_pay_date),
                supporting = stringResource(R.string.occurrence_date_hint),
                date = state.paidOn, enabled = !state.isSubmitting, onClick = { pickDate = true },
                errorText = state.dateError?.let { stringResource(it) },
                modifier = Modifier.testTag("field-occurrence-date"),
            )
            state.error?.let { ErrorBanner(stringResource(it), onDismiss = null, modifier = Modifier.testTag("settle-error")) }
            FluxaButton(
                text = stringResource(if (state.isCard) R.string.occurrence_charge_confirm else R.string.occurrence_pay_confirm),
                onClick = viewModel::settle, size = FluxaButtonSize.Large, loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag("action-confirm-settle"),
            )
            FluxaButton(
                text = stringResource(R.string.action_back), onClick = { showSettle = false }, variant = FluxaButtonVariant.Secondary,
                enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth(),
            )
        }
        if (pickDate) {
            FluxaDatePicker(initial = state.paidOn, maxDate = state.today, onPicked = { viewModel.onPaidOnChange(it); pickDate = false }, onDismiss = { pickDate = false })
        }
    }
    state.undoConfirmation?.let { target ->
        ConfirmArchiveSheet(
            title = stringResource(if (target.isCard) R.string.occurrence_undo_charge_title else R.string.occurrence_undo_payment_title),
            body = stringResource(if (target.isCard) R.string.occurrence_undo_charge_body else R.string.occurrence_undo_payment_body),
            confirmLabel = stringResource(undoLabel(target.isCard)),
            confirmIcon = FluxaIcons.Restore,
            onConfirm = viewModel::confirmUndo,
            onDismiss = {
                restoreUndoFocus = true
                viewModel.dismissUndo()
            },
        )
    }
    if (confirmSkip) {
        ConfirmArchiveSheet(
            title = stringResource(R.string.occurrence_skip_title),
            body = stringResource(R.string.occurrence_skip_body),
            confirmLabel = stringResource(R.string.occurrence_action_skip),
            dismissLabel = stringResource(R.string.occurrence_keep),
            confirmIcon = FluxaIcons.Skip,
            onConfirm = { confirmSkip = false; viewModel.skip() },
            onDismiss = { confirmSkip = false },
        )
    }
}

private fun undoLabel(isCard: Boolean) =
    if (isCard) R.string.occurrence_undo_charge else R.string.occurrence_undo_payment

@Composable
private fun Note(text: String, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FluxaTheme.colors.surfaceSunken, FluxaTheme.shapes.medium).padding(16.dp).testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(FluxaIcons.Alert, null, tint = FluxaTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
        Text(text, style = FluxaTheme.typography.label, color = FluxaTheme.colors.textSecondary)
    }
}
