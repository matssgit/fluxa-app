package com.fluxa.personal.ui.feature.credit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.ui.common.BalanceAmount
import com.fluxa.personal.ui.common.ConfirmArchiveSheet
import com.fluxa.personal.ui.common.DateField
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
import com.fluxa.personal.ui.designsystem.components.FluxaBottomSheet
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonSize
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.feature.catalog.visual
import com.fluxa.personal.ui.feature.subscriptions.short
import com.fluxa.personal.ui.feature.subscriptions.spoken

/**
 * Detalhe de uma parcela: pendente oferece "Pagar parcela" (sheet com conta, data e resumo de confirmação);
 * paga mostra vencimento e "Pago em" separados, a conta da baixa e "Desfazer pagamento"; cancelada é só leitura.
 */
@Composable
internal fun InstallmentDetailScreen(
    viewModel: InstallmentDetailViewModel,
    paymentViewModel: InstallmentPaymentViewModel,
    onBack: () -> Unit,
    onPaid: (PaymentOutcome) -> Unit,
    onUndone: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val payment by paymentViewModel.uiState.collectAsStateWithLifecycle()
    var showPay by rememberSaveable { mutableStateOf(false) }
    var confirmUndo by rememberSaveable { mutableStateOf(false) }
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography

    // Sucesso fecha o sheet e mostra feedback uma única vez por baixa (contadores evitam repetir em recomposição).
    var lastPaid by rememberSaveable { mutableIntStateOf(payment.completedCount) }
    LaunchedEffect(payment.completedCount) {
        if (payment.completedCount > lastPaid) {
            lastPaid = payment.completedCount
            showPay = false
            payment.outcome?.let(onPaid)
        }
    }
    var lastUndone by rememberSaveable { mutableIntStateOf(state.undoneCount) }
    LaunchedEffect(state.undoneCount) {
        if (state.undoneCount > lastUndone) {
            lastUndone = state.undoneCount
            onUndone()
        }
    }

    val installment = state.installment
    val purchase = state.purchase
    FeatureScreen(
        title = purchase?.details?.title ?: stringResource(R.string.installment_detail_title),
        overline = if (installment != null) stringResource(R.string.installment_detail_overline, installment.number, state.installmentCount)
        else stringResource(R.string.installment_detail_title),
        onBack = onBack,
        modifier = Modifier.testTag("screen-installment-detail"),
    ) {
        when {
            state.isLoading -> item { LoadingBlock() }
            state.loadError != null -> item { ErrorBanner(stringResource(state.loadError!!), onDismiss = null) }
            state.notFound || installment == null || purchase == null ->
                item { ErrorBanner(stringResource(R.string.error_installment_not_found), onDismiss = null) }
            else -> {
                val status = installment.status
                item {
                    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("installment-summary")) {
                        Text(stringResource(R.string.installment_field_amount).uppercase(), style = type.overline, color = colors.textMuted)
                        FluxaAmount(amount = installment.amount, style = type.headline.merge(type.amount).copy(fontSize = type.headline.fontSize))
                        InstallmentStatusPill(status, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                        DetailLine(
                            label = stringResource(R.string.installment_field_due),
                            value = installment.dueDate.short(),
                            spoken = installment.dueDate.spoken(),
                            modifier = Modifier.testTag("installment-due"),
                        )
                        state.card?.let { DetailLine(stringResource(R.string.installment_field_card), it.card.details.name) }
                    }
                }
                when (status) {
                    InstallmentStatus.PAID -> {
                        item { PaymentInfoCard(state) }
                        state.undoError?.let { error ->
                            item { ErrorBanner(stringResource(error), onDismiss = viewModel::dismissUndoError, modifier = Modifier.testTag("undo-error")) }
                        }
                        if (state.canUndo) {
                            item {
                                Text(stringResource(R.string.installment_undo_hint), style = type.caption, color = colors.textSecondary)
                            }
                            item {
                                FluxaButton(
                                    text = stringResource(R.string.installment_undo_action),
                                    onClick = { confirmUndo = true },
                                    variant = FluxaButtonVariant.Destructive,
                                    leadingIcon = FluxaIcons.Restore,
                                    loading = state.undoStatus == CommandStatus.Submitting,
                                    modifier = Modifier.fillMaxWidth().testTag("action-undo-payment"),
                                )
                            }
                        }
                    }
                    InstallmentStatus.PENDING -> if (state.canPay) {
                        item {
                            FluxaButton(
                                text = stringResource(R.string.installment_pay_action),
                                onClick = { paymentViewModel.start(); showPay = true },
                                size = FluxaButtonSize.Large,
                                leadingIcon = FluxaIcons.Check,
                                modifier = Modifier.fillMaxWidth().testTag("action-pay-installment"),
                            )
                        }
                        item { Text(stringResource(R.string.installment_pay_hint), style = type.caption, color = colors.textSecondary) }
                    }
                    InstallmentStatus.CANCELLED -> item {
                        Row(
                            modifier = Modifier.fillMaxWidth().background(colors.surfaceSunken, FluxaTheme.shapes.medium).padding(16.dp)
                                .testTag("installment-cancelled-note"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(FluxaIcons.Close, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.installment_cancelled_note), style = type.label, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }

    if (showPay && installment != null && purchase != null) {
        PaymentSheet(
            state = payment,
            title = purchase.details.title,
            number = installment.number,
            count = state.installmentCount,
            amount = installment.amount,
            due = installment.dueDate.short(),
            viewModel = paymentViewModel,
            onDismiss = { if (!payment.isSubmitting) showPay = false },
            onCreateAccount = { showPay = false; onCreateAccount() },
        )
    }
    if (confirmUndo) {
        ConfirmArchiveSheet(
            title = stringResource(R.string.installment_undo_title),
            body = stringResource(R.string.installment_undo_body),
            confirmLabel = stringResource(R.string.installment_undo_action),
            dismissLabel = stringResource(R.string.installment_undo_keep),
            confirmIcon = FluxaIcons.Restore,
            stacked = true,
            onConfirm = { confirmUndo = false; viewModel.undoPayment() },
            onDismiss = { confirmUndo = false },
        )
    }
}

@Composable
private fun InstallmentStatusPill(status: InstallmentStatus, modifier: Modifier = Modifier) {
    val (label, icon) = when (status) {
        InstallmentStatus.PENDING -> stringResource(R.string.installment_status_pending) to FluxaIcons.Calendar
        InstallmentStatus.PAID -> stringResource(R.string.installment_status_paid) to FluxaIcons.Check
        InstallmentStatus.CANCELLED -> stringResource(R.string.installment_status_cancelled) to FluxaIcons.Close
    }
    val described = stringResource(R.string.installment_status_described, label)
    StatePill(label, icon, modifier = modifier.testTag("installment-status"), spoken = described)
}

/** Par rótulo/valor lido como uma frase pelo TalkBack ("Vencimento: 17 de dezembro de 2026"). */
@Composable
private fun DetailLine(label: String, value: String, modifier: Modifier = Modifier, spoken: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 32.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = FluxaTheme.typography.label, color = FluxaTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        Text(
            value, style = FluxaTheme.typography.bodyStrong, color = FluxaTheme.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier,
        )
    }
}

@Composable
private fun PaymentInfoCard(state: InstallmentDetailUiState) {
    val payment = state.payment
    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("payment-info")) {
        Text(stringResource(R.string.installment_payment_section).uppercase(), style = FluxaTheme.typography.overline, color = FluxaTheme.colors.textMuted)
        if (payment == null) {
            // Estado PAID sem a baixa lida: o Flow ainda vai emitir, ou o vínculo diverge (o backend sinaliza no estorno).
            Text(stringResource(R.string.installment_payment_loading), style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary)
        } else {
            DetailLine(
                stringResource(R.string.installment_field_paid_on),
                payment.details.occurredOn.short(),
                spoken = payment.details.occurredOn.spoken(),
                modifier = Modifier.testTag("payment-date"),
            )
            DetailLine(
                stringResource(R.string.installment_field_paid_account),
                state.paymentAccount?.details?.name ?: stringResource(R.string.installment_account_unknown),
                modifier = Modifier.testTag("payment-account"),
            )
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp).semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.installment_field_paid_amount), style = FluxaTheme.typography.label, color = FluxaTheme.colors.textSecondary, modifier = Modifier.weight(1f))
                FluxaAmount(amount = payment.details.amount, style = FluxaTheme.typography.amount)
            }
        }
    }
}

@Composable
private fun PaymentSheet(
    state: InstallmentPaymentUiState,
    title: String,
    number: Int,
    count: Int,
    amount: com.fluxa.personal.core.money.Money,
    due: String,
    viewModel: InstallmentPaymentViewModel,
    onDismiss: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    var pickDate by rememberSaveable { mutableStateOf(false) }
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val confirming = state.step == PaymentStep.Confirm
    FluxaBottomSheet(
        title = stringResource(if (confirming) R.string.installment_pay_confirm_title else R.string.installment_pay_action),
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("sheet-pay-installment"),
        // Na confirmação os botões ficam empilhados em largura total (lado a lado, "Confirmar pagamento" cortava em 360dp).
        footer = if (!confirming && !(state.accountsLoaded && state.accounts.isEmpty())) {
            {
                FluxaButton(
                    text = stringResource(R.string.action_continue),
                    onClick = viewModel::review,
                    size = FluxaButtonSize.Large,
                    modifier = Modifier.weight(1f).testTag("action-review-payment"),
                )
            }
        } else null,
    ) {
        // Área rolável proporcional à tela, para cabeçalho e botões do sheet caberem em 320×640 com fonte 1,3.
        val maxScroll = (LocalConfiguration.current.screenHeightDp - 280).coerceIn(200, 460).dp
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = maxScroll).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Resumo fixo do que será pago: o valor não é editável e vem da parcela.
            Column(
                modifier = Modifier.fillMaxWidth().background(colors.surfaceSunken, FluxaTheme.shapes.medium).padding(16.dp)
                    .semantics(mergeDescendants = true) {}.testTag("payment-summary"),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(title, style = type.bodyStrong, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.installment_detail_overline, number, count), style = type.label, color = colors.textSecondary)
                FluxaAmount(amount = amount, style = type.title.merge(type.amount))
                Text(stringResource(R.string.installment_due, due), style = type.caption, color = colors.textSecondary)
            }
            if (confirming) {
                ConfirmSummary(state)
                state.operationError?.let { ErrorBanner(stringResource(it), onDismiss = null, modifier = Modifier.testTag("payment-error")) }
            } else {
                state.operationError?.let { ErrorBanner(stringResource(it), onDismiss = null, modifier = Modifier.testTag("payment-error")) }
                FieldLabel(stringResource(R.string.installment_pay_account))
                if (state.accountsLoaded && state.accounts.isEmpty()) {
                    Text(stringResource(R.string.installment_pay_no_accounts), style = type.body, color = colors.textSecondary)
                    FluxaButton(
                        text = stringResource(R.string.transaction_create_account),
                        onClick = onCreateAccount,
                        variant = FluxaButtonVariant.Secondary,
                        leadingIcon = FluxaIcons.Plus,
                        modifier = Modifier.testTag("action-create-account"),
                    )
                } else {
                    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.accounts.forEach { option -> AccountOptionRow(option, selected = option.account.id == state.accountId, enabled = !state.isSubmitting) { viewModel.onAccountChange(option.account.id) } }
                    }
                    Text(stringResource(R.string.installment_pay_negative_ok), style = type.caption, color = colors.textMuted)
                }
                state.accountError?.let { FieldError(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }) }
                DateField(
                    label = stringResource(R.string.installment_pay_date),
                    supporting = stringResource(R.string.installment_pay_date_hint),
                    date = state.paidOn,
                    enabled = !state.isSubmitting,
                    onClick = { pickDate = true },
                    errorText = state.dateError?.let { stringResource(it) },
                    modifier = Modifier.testTag("field-payment-date"),
                )
            }
        }
        if (confirming) {
            FluxaButton(
                text = stringResource(R.string.installment_pay_confirm),
                onClick = viewModel::confirm,
                size = FluxaButtonSize.Large,
                loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag("action-confirm-payment"),
            )
            FluxaButton(
                text = stringResource(R.string.action_back),
                onClick = viewModel::backToForm,
                variant = FluxaButtonVariant.Secondary,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().testTag("action-back-payment"),
            )
        }
    }
    if (pickDate) {
        FluxaDatePicker(
            initial = state.paidOn,
            maxDate = state.today,
            onPicked = { viewModel.onDateChange(it); pickDate = false },
            onDismiss = { pickDate = false },
        )
    }
}

@Composable
private fun ConfirmSummary(state: InstallmentPaymentUiState) {
    val type = FluxaTheme.typography
    val colors = FluxaTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().testTag("payment-confirm-summary"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Text(stringResource(R.string.installment_pay_by_account), style = type.label, color = colors.textSecondary)
            Text(state.selected?.account?.details?.name.orEmpty(), style = type.bodyStrong, color = colors.textPrimary)
        }
        Column(modifier = Modifier.padding(top = 8.dp).semantics(mergeDescendants = true) {}) {
            Text(stringResource(R.string.installment_pay_date_label), style = type.label, color = colors.textSecondary)
            Text(state.paidOn.short(), style = type.bodyStrong, color = colors.textPrimary)
        }
    }
}

@Composable
private fun AccountOptionRow(option: PaymentAccountOption, selected: Boolean, enabled: Boolean, onSelect: () -> Unit) {
    val colors = FluxaTheme.colors
    val visual = option.account.details.type.visual()
    val shape = FluxaTheme.shapes.medium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.brandTonal else colors.surface, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.brand else colors.border, shape)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("payment-account-${option.account.details.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(36.dp).background(colors.surfaceSunken, FluxaTheme.shapes.pill), contentAlignment = Alignment.Center) {
            Icon(visual.icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
        }
        // Saldo abaixo do nome: em telas estreitas o nome não é cortado pelo valor.
        Column(modifier = Modifier.weight(1f)) {
            Text(option.account.details.name, style = FluxaTheme.typography.bodyStrong, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(stringResource(visual.label), style = FluxaTheme.typography.caption, color = colors.textSecondary)
            if (option.balance != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.installment_pay_balance), style = FluxaTheme.typography.caption, color = colors.textSecondary)
                    BalanceAmount(option.balance, style = FluxaTheme.typography.caption, color = colors.textSecondary)
                }
            }
        }
        if (selected) Icon(FluxaIcons.Check, contentDescription = null, tint = colors.brand, modifier = Modifier.size(20.dp))
    }
}
