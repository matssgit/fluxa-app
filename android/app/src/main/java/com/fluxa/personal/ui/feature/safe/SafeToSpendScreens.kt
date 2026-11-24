package com.fluxa.personal.ui.feature.safe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.ProjectedCashSource
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.LoadingBlock
import com.fluxa.personal.ui.common.ReserveLine
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.designsystem.components.FluxaSurfaceTone
import com.fluxa.personal.ui.feature.subscriptions.RowDivider
import com.fluxa.personal.ui.feature.subscriptions.short
import com.fluxa.personal.ui.feature.subscriptions.spoken
import java.time.LocalDate
import java.util.UUID

// region Textos de apresentação

private fun SafeSituation.body(): Int = when (this) {
    SafeSituation.Reduced -> R.string.safe_body_reduced
    SafeSituation.NoObligations -> R.string.safe_body_no_obligations
    SafeSituation.ZeroByObligations -> R.string.safe_body_zero
    SafeSituation.NegativeAvailable -> R.string.safe_body_negative
}

/** Atenção só quando o limite é zero (fato do contrato), sempre com ícone e texto — nunca só cor. */
private val SafeSituation.needsAttention: Boolean
    get() = this == SafeSituation.ZeroByObligations || this == SafeSituation.NegativeAvailable

@Composable
private fun lowestText(c: SafeToSpendUiState.Content): String =
    if (c.lowestSlackIsToday) stringResource(R.string.safe_lowest_today) else stringResource(R.string.safe_lowest_on, c.lowestSlackDate.short())

@Composable
private fun lowestSpoken(c: SafeToSpendUiState.Content): String =
    if (c.lowestSlackIsToday) stringResource(R.string.safe_lowest_today) else stringResource(R.string.safe_lowest_on, c.lowestSlackDate.spoken())

// endregion

// region Card do Início

/**
 * Card do Limite seguro no Início, logo abaixo do disponível. O valor é grande, mas menor que o saldo principal
 * (hierarquia: disponível → limite seguro). "Entender cálculo" abre o detalhe.
 */
@Composable
internal fun SafeToSpendCard(viewModel: SafeToSpendViewModel, onDetails: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    FluxaCard(modifier = modifier.fillMaxWidth().testTag("safe-to-spend-card"), contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(28.dp).background(colors.brandTonal, FluxaTheme.shapes.pill), contentAlignment = Alignment.Center) {
                Icon(FluxaIcons.Wallet, null, tint = colors.onBrandTonal, modifier = Modifier.size(16.dp))
            }
            Text(stringResource(R.string.safe_title).uppercase(), style = type.overline, color = colors.textMuted, modifier = Modifier.semantics { heading() })
        }
        Spacer(Modifier.height(8.dp))
        when (val s = state) {
            // Compacto: o bloco de carregamento de tela cheia fazia o Início "pular" quando o valor chegava.
            SafeToSpendUiState.Loading -> Text(
                stringResource(R.string.loading), style = type.body, color = colors.textMuted,
                modifier = Modifier.heightIn(min = 96.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
            is SafeToSpendUiState.Unavailable -> Text(stringResource(s.message), style = type.body, color = colors.textSecondary)
            is SafeToSpendUiState.Content -> {
                FluxaAmount(
                    amount = s.safeAmount,
                    style = type.headline.merge(type.amount).copy(fontSize = type.headline.fontSize),
                    color = if (s.situation.needsAttention) colors.expense else colors.textPrimary,
                    modifier = Modifier.testTag("safe-amount"),
                )
                Spacer(Modifier.height(6.dp))
                if (s.situation.needsAttention) {
                    SituationAlert(stringResource(s.situation.body()), modifier = Modifier.testTag("safe-situation"))
                } else {
                    Text(stringResource(s.situation.body()), style = type.label, color = colors.textSecondary, modifier = Modifier.testTag("safe-situation"))
                }
                Spacer(Modifier.height(8.dp))
                // Só a data é anunciada por extenso (sem valores: respeita "ocultar valores").
                val lowestSpokenText = lowestSpoken(s)
                Row(
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("safe-lowest"),
                ) {
                    Icon(FluxaIcons.Calendar, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    Text(lowestText(s), style = type.caption, color = colors.textSecondary, modifier = Modifier.semantics { contentDescription = lowestSpokenText })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FluxaButton(
            text = stringResource(R.string.safe_details_action), onClick = onDetails, variant = FluxaButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth().testTag("action-safe-details"),
        )
    }
}

@Composable
private fun SituationAlert(text: String, modifier: Modifier = Modifier) {
    val colors = FluxaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().background(colors.expenseContainer, FluxaTheme.shapes.medium).padding(12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(FluxaIcons.Alert, null, tint = colors.expense, modifier = Modifier.size(18.dp))
        Text(text, style = FluxaTheme.typography.label, color = colors.expense)
    }
}

// endregion

// region Detalhe

@Composable
internal fun SafeToSpendDetailScreen(
    viewModel: SafeToSpendViewModel,
    onBack: () -> Unit,
    onOpenSubscription: (UUID, LocalDate) -> Unit,
    onOpenInstallment: (cardId: UUID, purchaseId: UUID, installmentId: UUID) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    FeatureScreen(
        title = stringResource(R.string.safe_title),
        overline = stringResource(R.string.safe_horizon, SafeToSpendViewModel.HORIZON_DAYS),
        onBack = onBack,
        modifier = Modifier.testTag("screen-safe-to-spend"),
    ) {
        when (val s = state) {
            SafeToSpendUiState.Loading -> item { LoadingBlock() }
            is SafeToSpendUiState.Unavailable -> item { ErrorBanner(stringResource(s.message), onDismiss = null) }
            is SafeToSpendUiState.Content -> {
                item {
                    FluxaCard(
                        modifier = Modifier.fillMaxWidth().testTag("safe-hero"),
                        tone = FluxaSurfaceTone.Inverse, featured = true, contentPadding = PaddingValues(24.dp),
                    ) {
                        Text(stringResource(R.string.safe_title).uppercase(), style = type.overline, color = colors.onSurfaceInverseMuted)
                        FluxaAmount(amount = s.safeAmount, style = type.amountHero, color = colors.onSurfaceInverse, modifier = Modifier.testTag("safe-detail-amount"))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.safe_subtitle), style = type.label, color = colors.onSurfaceInverseMuted)
                    }
                }
                // "Reduzido" já é explicado pelo subtítulo do destaque; as demais situações ganham uma linha própria.
                if (s.situation != SafeSituation.Reduced) item {
                    if (s.situation.needsAttention) SituationAlert(stringResource(s.situation.body()), modifier = Modifier.testTag("safe-detail-situation"))
                    else Text(stringResource(s.situation.body()), style = type.body, color = colors.textSecondary, modifier = Modifier.testTag("safe-detail-situation"))
                }
                item {
                    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("safe-how")) {
                        Text(stringResource(R.string.safe_how_title).uppercase(), style = type.overline, color = colors.textMuted)
                        Spacer(Modifier.height(4.dp))
                        ReserveLine(
                            stringResource(R.string.safe_available_now), CalculatedBalance.Exact(s.availableNow), modifier = Modifier.testTag("safe-available-now"),
                            color = if (s.availableNow.cents < 0) colors.expense else colors.textPrimary,
                        )
                        Text(stringResource(R.string.safe_available_explain), style = type.caption, color = colors.textMuted)
                        s.committedForDisplay?.let {
                            Spacer(Modifier.height(4.dp))
                            ReserveLine(stringResource(R.string.safe_committed_prefix), CalculatedBalance.Exact(it), modifier = Modifier.testTag("safe-committed"))
                        }
                        Spacer(Modifier.height(4.dp))
                        ReserveLine(stringResource(R.string.safe_title), CalculatedBalance.Exact(s.safeAmount), modifier = Modifier.testTag("safe-line"))
                        Text(stringResource(R.string.safe_limit_explain), style = type.caption, color = colors.textMuted)
                        SafeDivider()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp).semantics(mergeDescendants = true) {}.testTag("safe-lowest-date"),
                        ) {
                            Text(stringResource(R.string.safe_lowest_label), style = type.label, color = colors.textSecondary, modifier = Modifier.weight(1f))
                            Text(
                                if (s.lowestSlackIsToday) stringResource(R.string.safe_today) else s.lowestSlackDate.short(),
                                style = type.bodyStrong, color = colors.textPrimary,
                                modifier = if (s.lowestSlackIsToday) Modifier else Modifier.semantics { contentDescription = s.lowestSlackDate.spoken() },
                            )
                        }
                        ReserveLine(
                            stringResource(R.string.safe_lowest_balance), CalculatedBalance.Exact(s.lowestProjectedBalance), modifier = Modifier.testTag("safe-lowest-balance"),
                            color = if (s.lowestProjectedBalance.cents < 0) colors.expense else colors.textPrimary,
                        )
                        SafeDivider()
                        // Empilhado: em 320dp com fonte grande, lado a lado espremia o rótulo (revisão visual).
                        Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("safe-horizon")) {
                            Text(stringResource(R.string.safe_horizon, s.horizonDays), style = type.label, color = colors.textSecondary)
                            Text(
                                stringResource(R.string.safe_horizon_range, s.horizonStart.short(), s.horizonEndExclusive.minusDays(1).short()),
                                style = type.caption, color = colors.textMuted,
                            )
                        }
                    }
                }
                item { FluxaSectionHeader(title = stringResource(R.string.safe_breakdown_title)) }
                if (s.breakdown.isEmpty()) {
                    item { Text(stringResource(R.string.safe_breakdown_empty), style = type.body, color = colors.textSecondary, modifier = Modifier.testTag("safe-breakdown-empty")) }
                } else {
                    item { Text(stringResource(R.string.safe_breakdown_hint), style = type.caption, color = colors.textMuted) }
                    item {
                        FluxaCard(modifier = Modifier.fillMaxWidth().testTag("safe-breakdown"), contentPadding = PaddingValues(vertical = 4.dp)) {
                            s.breakdown.forEachIndexed { i, item ->
                                if (i > 0) RowDivider()
                                SafeEventRow(item, s.today, onOpenSubscription, onOpenInstallment)
                            }
                        }
                    }
                }
                item { Note(stringResource(R.string.safe_reserves_note), FluxaIcons.Target, "safe-note-reserves") }
                item { Note(stringResource(R.string.safe_cards_note), FluxaIcons.Card, "safe-note-cards") }
                item { Note(stringResource(R.string.safe_registered_only), FluxaIcons.Alert, "safe-note-registered") }
            }
        }
    }
}

@Composable
private fun SafeDivider() {
    Spacer(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(FluxaTheme.colors.border))
}

/** Uma linha do breakdown: data efetiva, descrição, fonte e valor (sinal da projeção), exatamente como o backend deu. */
@Composable
private fun SafeEventRow(
    item: SafeEventItem,
    today: LocalDate,
    onOpenSubscription: (UUID, LocalDate) -> Unit,
    onOpenInstallment: (UUID, UUID, UUID) -> Unit,
) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val e = item.event
    val title = when (e.source) {
        is ProjectedCashSource.Installment ->
            if (item.title != null && item.installmentNumber != null && item.installmentCount != null) {
                stringResource(R.string.safe_event_installment, item.title, item.installmentNumber, item.installmentCount)
            } else item.title ?: stringResource(R.string.safe_event_installment_unknown)
        is ProjectedCashSource.Subscription -> item.title ?: stringResource(R.string.safe_event_subscription_unknown)
        is ProjectedCashSource.ConfirmedIncome -> item.title ?: stringResource(R.string.safe_event_income_unknown)
    }
    val sourceLabel = when (val src = e.source) {
        is ProjectedCashSource.Installment -> stringResource(R.string.safe_source_installment)
        is ProjectedCashSource.Subscription ->
            stringResource(if (src.paymentSource is SubscriptionSource.Card) R.string.safe_source_subscription_card else R.string.safe_source_subscription)
        is ProjectedCashSource.ConfirmedIncome -> stringResource(R.string.safe_source_income)
    }
    val onClick: (() -> Unit)? = when (val src = e.source) {
        is ProjectedCashSource.Subscription -> ({ onOpenSubscription(src.key.subscriptionId, src.key.occurrenceDate) })
        is ProjectedCashSource.Installment -> ({ onOpenInstallment(src.cardId, src.purchaseId, src.installmentId) })
        is ProjectedCashSource.ConfirmedIncome -> null
    }
    val dateText = if (e.date == today) stringResource(R.string.safe_today) else e.date.short()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            // Data, descrição e tipo anunciados; o valor vem do FluxaAmount (respeita "ocultar valores").
            .semantics(mergeDescendants = true) {}
            .testTag("safe-event-$title"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(dateText, style = type.caption, color = colors.textSecondary, modifier = if (e.date == today) Modifier else Modifier.semantics { contentDescription = e.date.spoken() })
                Text(title, style = type.bodyStrong, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(sourceLabel, style = type.caption, color = colors.textSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 18.dp)) {
                Text(if (item.isIncome) "+" else "−", style = type.amount, color = if (item.isIncome) colors.income else colors.textPrimary, modifier = Modifier.clearAndSetSemantics {})
                FluxaAmount(amount = e.amount, style = type.amount, color = if (item.isIncome) colors.income else colors.textPrimary)
            }
        }
        // Nota de vencida em largura total (no 320dp/1,3 ficava espremida ao lado do valor).
        item.originalDate?.takeIf { item.isOverdue }?.let {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(FluxaIcons.Alert, null, tint = colors.expense, modifier = Modifier.padding(top = 2.dp).size(14.dp))
                Text(stringResource(R.string.safe_overdue_since, it.short()), style = type.caption, color = colors.expense)
            }
        }
    }
}

@Composable
private fun Note(text: String, icon: ImageVector, tag: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(FluxaTheme.colors.surfaceSunken, FluxaTheme.shapes.medium).padding(14.dp).testTag(tag),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = FluxaTheme.colors.textSecondary, modifier = Modifier.size(18.dp))
        Text(text, style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textSecondary)
    }
}

// endregion
