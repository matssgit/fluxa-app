package com.fluxa.personal.ui.feature.cash

import androidx.compose.foundation.background
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.layout.heightIn
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.common.isNonZero
import com.fluxa.personal.ui.common.isNegative
import com.fluxa.personal.ui.common.ReserveLine
import com.fluxa.personal.ui.common.BrazilLocale
import com.fluxa.personal.domain.model.PeriodSummary
import com.fluxa.personal.core.money.Money
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.common.BalanceAmount
import com.fluxa.personal.ui.common.bleedHorizontal
import com.fluxa.personal.ui.common.EmptyStateCard
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.LedgerRow
import com.fluxa.personal.ui.common.LoadingBlock
import com.fluxa.personal.ui.common.StatePill
import com.fluxa.personal.ui.common.dayLabel
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.LocalAmountsHidden
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaChip
import com.fluxa.personal.ui.designsystem.components.FluxaIconButton
import com.fluxa.personal.ui.designsystem.components.FluxaNavigationBarDefaults
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.designsystem.components.FluxaSurfaceTone
import com.fluxa.personal.ui.designsystem.components.FluxaTopBar
import com.fluxa.personal.ui.feature.catalog.visual
import java.time.LocalDate
import java.util.UUID

private val ContentMaxWidth = 640.dp

/** Tela de aba (com a navegação flutuante): top bar com privacidade e lista rolável com folga para a barra. */
@Composable
internal fun TabScreen(
    route: String,
    title: String,
    onToggleAmounts: () -> Unit,
    overline: String? = null,
    content: LazyListScope.() -> Unit,
) {
    val hidden = LocalAmountsHidden.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FluxaTheme.colors.background)
            // Edge-to-edge: o conteúdo rolável não passa sob a barra de status (a top bar consome o mesmo inset).
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("screen-$route"),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxSize().testTag("list-$route"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = FluxaNavigationBarDefaults.ContentClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FluxaTopBar(title = title, overline = overline, modifier = Modifier.bleedHorizontal(16.dp)) {
                    FluxaIconButton(
                        icon = if (hidden) FluxaIcons.EyeOff else FluxaIcons.Eye,
                        contentDescription = stringResource(if (hidden) R.string.action_show_amounts else R.string.action_hide_amounts),
                        onClick = onToggleAmounts,
                        modifier = Modifier.testTag("toggle-amounts"),
                    )
                }
            }
            content()
        }
    }
}

/** Cartão de saldo disponível (consolidado do repositório). */
@Composable
internal fun AvailableBalanceCard(
    available: CalculatedBalance?,
    caption: String,
    modifier: Modifier = Modifier,
    /** Totais de `observeReserveBalances`; exibidos como vieram (o disponível já é líquido das reservas). */
    reserves: com.fluxa.personal.ui.common.ReserveTotals? = null,
) {
    val colors = FluxaTheme.colors
    FluxaCard(
        modifier = modifier.fillMaxWidth().testTag("available-balance"),
        tone = FluxaSurfaceTone.Inverse,
        featured = true,
        contentPadding = PaddingValues(24.dp),
    ) {
        Text(stringResource(R.string.cash_available_label).uppercase(), style = FluxaTheme.typography.overline, color = colors.onSurfaceInverseMuted)
        Spacer(Modifier.height(12.dp))
        BalanceAmount(balance = available, style = FluxaTheme.typography.amountHero, color = colors.onSurfaceInverse)
        Spacer(Modifier.height(12.dp))
        Text(caption, style = FluxaTheme.typography.caption, color = colors.onSurfaceInverseMuted)
        if (reserves != null && reserves.reserved.isNonZero) {
            Spacer(Modifier.height(16.dp))
            ReserveLine(
                stringResource(R.string.reserve_total_balance), reserves.total, modifier = Modifier.testTag("reserve-total"),
                color = colors.onSurfaceInverse, labelColor = colors.onSurfaceInverseMuted,
            )
            ReserveLine(
                stringResource(R.string.reserve_reserved), reserves.reserved, modifier = Modifier.testTag("reserve-reserved-total"),
                color = colors.onSurfaceInverse, labelColor = colors.onSurfaceInverseMuted,
            )
            Text(stringResource(R.string.reserve_available_explain), style = FluxaTheme.typography.caption, color = colors.onSurfaceInverseMuted, modifier = Modifier.padding(top = 4.dp))
        }
        if (available.isNegative) {
            com.fluxa.personal.ui.common.NegativeAvailableAlert(
                stringResource(R.string.reserve_negative_alert_total), onInverse = true, modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
internal fun CashScreen(
    viewModel: CashViewModel,
    onToggleAmounts: () -> Unit,
    onManageAccounts: () -> Unit,
    onCreateAccount: () -> Unit,
    onOpenTransaction: (UUID) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = FinancialCalendar().today()
    TabScreen(route = "cash", title = stringResource(R.string.cash), onToggleAmounts = onToggleAmounts) {
        if (state.isLoading) {
            item { LoadingBlock() }
            return@TabScreen
        }
        state.errorMessage?.let { item { ErrorBanner(stringResource(it), onDismiss = null) } }
        if (!state.hasAccounts) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.cash_no_accounts_title),
                    body = stringResource(R.string.cash_no_accounts_body),
                    icon = FluxaIcons.Wallet,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FluxaButton(
                        text = stringResource(R.string.transaction_create_account),
                        onClick = onCreateAccount,
                        leadingIcon = FluxaIcons.Plus,
                        modifier = Modifier.testTag("action-create-account"),
                    )
                    FluxaButton(
                        text = stringResource(R.string.cash_manage_accounts),
                        onClick = onManageAccounts,
                        variant = FluxaButtonVariant.Secondary,
                        modifier = Modifier.testTag("action-manage-accounts"),
                    )
                }
            }
            return@TabScreen
        }
        item { AvailableBalanceCard(state.available, stringResource(R.string.cash_available_explain), reserves = state.reserveTotals) }

        item {
            FluxaSectionHeader(
                title = stringResource(R.string.cash_accounts_title),
                actionLabel = stringResource(R.string.cash_manage_accounts),
                onAction = onManageAccounts,
                actionModifier = Modifier.testTag("action-manage-accounts"),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.accounts.forEach { item ->
                    AccountBalanceTile(
                        item = item,
                        selected = state.filters.accountId == item.account.id,
                        onClick = {
                            viewModel.setAccount(if (state.filters.accountId == item.account.id) null else item.account.id)
                        },
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(R.string.cash_history_title),
                        style = FluxaTheme.typography.title,
                        color = FluxaTheme.colors.textPrimary,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                    Text(
                        if (state.hasMore) pluralStringResource(R.plurals.cash_entries_more, state.entryCount, state.entryCount)
                        else pluralStringResource(R.plurals.cash_entries, state.entryCount, state.entryCount),
                        style = FluxaTheme.typography.caption,
                        color = FluxaTheme.colors.textSecondary,
                        modifier = Modifier.testTag("history-count"),
                    )
                }
                FilterRow {
                    HistoryPeriod.entries.forEach { period ->
                        FluxaChip(
                            label = stringResource(period.label),
                            selected = state.filters.period == period,
                            onSelectedChange = { viewModel.setPeriod(period) },
                            role = Role.RadioButton,
                            modifier = Modifier.testTag("period-${period.name}"),
                        )
                    }
                }
                FilterRow {
                    DirectionFilter.entries.forEach { filter ->
                        FluxaChip(
                            label = stringResource(filter.label),
                            selected = state.filters.direction == filter,
                            onSelectedChange = { viewModel.setDirection(filter) },
                            role = Role.RadioButton,
                            modifier = Modifier.testTag("direction-filter-${filter.name}"),
                        )
                    }
                    if (state.filters.accountId != null) {
                        FluxaChip(
                            label = stringResource(R.string.cash_filter_all_accounts),
                            selected = false,
                            onSelectedChange = { viewModel.setAccount(null) },
                            leadingIcon = FluxaIcons.Close,
                            modifier = Modifier.testTag("clear-account-filter"),
                        )
                    }
                }
            }
        }

        if (state.days.isEmpty()) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.cash_history_empty),
                    body = stringResource(R.string.cash_history_empty_body),
                    icon = FluxaIcons.Cash,
                )
            }
        } else {
            items(state.days, key = { it.date.toEpochDay() }) { day -> LedgerDayCard(day, today, onOpenTransaction) }
            if (state.hasMore) {
                item {
                    FluxaButton(
                        text = stringResource(R.string.cash_load_more),
                        onClick = viewModel::loadMore,
                        variant = FluxaButtonVariant.Secondary,
                        modifier = Modifier.fillMaxWidth().testTag("action-load-more"),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun AccountBalanceTile(item: AccountBalanceItem, selected: Boolean, onClick: () -> Unit) {
    val colors = FluxaTheme.colors
    val visual = item.account.details.type.visual()
    FluxaCard(
        modifier = Modifier
            .widthIn(min = 168.dp, max = 220.dp)
            .then(if (selected) Modifier.border(2.dp, colors.brand, FluxaTheme.shapes.large) else Modifier)
            .testTag("balance-${item.account.details.name}"),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(visual.icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
            Text(item.account.details.name, style = FluxaTheme.typography.label, color = colors.textPrimary, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            if (selected) Icon(FluxaIcons.Check, contentDescription = null, tint = colors.brand, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.cash_current_balance), style = FluxaTheme.typography.caption, color = colors.textMuted)
        val negative = (item.balance as? CalculatedBalance.Exact)?.amount?.cents?.let { it < 0 } == true
        BalanceAmount(item.balance, color = if (negative) colors.expense else colors.textPrimary)
        // Reserva de metas desta conta: reservado e disponível do contrato (sem subtrair aqui).
        val reserve = item.reserve
        if (reserve != null && reserve.reservedAmount.isNonZero) {
            Spacer(Modifier.height(8.dp))
            ReserveLine(stringResource(R.string.reserve_reserved), reserve.reservedAmount, modifier = Modifier.testTag("account-reserved-${item.account.details.name}"))
            ReserveLine(
                stringResource(R.string.reserve_available), reserve.availableAmount, modifier = Modifier.testTag("account-available-${item.account.details.name}"),
                color = if (reserve.availableAmount.isNegative) colors.expense else colors.textPrimary,
            )
            if (reserve.availableAmount.isNegative) {
                // Versão curta no card estreito; a frase completa aparece no detalhe da meta e no card de disponível.
                com.fluxa.personal.ui.common.NegativeAvailableAlert(stringResource(R.string.reserve_negative_alert_short), modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (!item.account.details.includeInAvailable) {
            StatePill(stringResource(R.string.accounts_not_in_available), null, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
internal fun LedgerDayCard(day: LedgerDay, today: LocalDate, onOpenTransaction: (UUID) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            dayLabel(day.date, today),
            style = FluxaTheme.typography.label,
            color = FluxaTheme.colors.textSecondary,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() },
        )
        FluxaCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
            day.entries.forEachIndexed { index, entry ->
                if (index > 0) Box(Modifier.padding(start = 72.dp).fillMaxWidth().height(1.dp).background(FluxaTheme.colors.border))
                LedgerEntryRow(entry, onClick = { onOpenTransaction(entry.transaction.id) })
            }
        }
    }
}

@Composable
internal fun LedgerEntryRow(entry: LedgerEntry, onClick: () -> Unit) {
    val details = entry.transaction.details
    val categoryName = entry.category?.details?.name.orEmpty()
    val accountName = entry.account?.details?.name.orEmpty()
    val title = details.description ?: categoryName
    val meta = listOfNotNull(categoryName.takeIf { details.description != null }, accountName).filter { it.isNotEmpty() }.joinToString(" · ")
    LedgerRow(
        title = title,
        meta = meta,
        amount = details.amount,
        direction = details.direction,
        cancelled = entry.transaction.cancelledAt != null,
        icon = entry.category?.details?.icon,
        color = entry.category?.details?.color,
        onClick = onClick,
        modifier = Modifier.testTag("tx-$title"),
        fromCard = entry.transaction.installmentId != null,
        fromSubscription = entry.transaction.subscriptionOccurrence != null,
    )
}

// region Início

@Composable
internal fun DashboardScreen(
    viewModel: DashboardViewModel,
    onToggleAmounts: () -> Unit,
    onOpenCash: () -> Unit,
    onCreateAccount: () -> Unit,
    onOpenTransaction: (UUID) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TabScreen(
        route = "dashboard",
        title = stringResource(R.string.dashboard),
        overline = stringResource(R.string.placeholder_dashboard_overline),
        onToggleAmounts = onToggleAmounts,
    ) {
        if (state.isLoading) {
            item { LoadingBlock() }
            return@TabScreen
        }
        state.errorMessage?.let { item { ErrorBanner(stringResource(it), onDismiss = null) } }
        if (state.activeAccounts == 0) {
            item {
                EmptyStateCard(
                    title = stringResource(R.string.cash_no_accounts_title),
                    body = stringResource(R.string.cash_no_accounts_body),
                    icon = FluxaIcons.Wallet,
                )
            }
            item {
                FluxaButton(
                    text = stringResource(R.string.transaction_create_account),
                    onClick = onCreateAccount,
                    leadingIcon = FluxaIcons.Plus,
                    modifier = Modifier.testTag("action-create-account"),
                )
            }
            return@TabScreen
        }
        item {
            AvailableBalanceCard(
                available = state.available,
                caption = pluralStringResource(R.plurals.dashboard_accounts, state.activeAccounts, state.activeAccounts),
                reserves = state.reserveTotals,
            )
        }
        state.summary?.let { summary ->
            item { MonthSummaryCard(summary, state.monthStart) }
        }
        item {
            FluxaSectionHeader(
                title = stringResource(R.string.dashboard_recent_title),
                actionLabel = stringResource(R.string.dashboard_recent_action),
                onAction = onOpenCash,
            )
        }
        if (state.recent.isEmpty()) {
            item {
                FluxaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dashboard_recent_empty), style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary)
                }
            }
        } else {
            item {
                FluxaCard(modifier = Modifier.fillMaxWidth().testTag("recent-list"), contentPadding = PaddingValues(vertical = 4.dp)) {
                    state.recent.forEachIndexed { index, entry ->
                        if (index > 0) Box(Modifier.padding(start = 72.dp).fillMaxWidth().height(1.dp).background(FluxaTheme.colors.border))
                        LedgerEntryRow(entry, onClick = { onOpenTransaction(entry.transaction.id) })
                    }
                }
            }
        }
    }
}

/**
 * Resumo do mês vindo de `observePeriodSummary`. Os três valores são exibidos como chegam do repositório:
 * nenhuma soma na UI. Sinal e texto indicam entrada/saída/resultado (não só cor); valores passam pela ocultação.
 */
@Composable
private fun MonthSummaryCard(summary: PeriodSummary, monthStart: LocalDate?) {
    val colors = FluxaTheme.colors
    val monthLabel = monthStart?.format(MonthTitle)?.replaceFirstChar { it.titlecase(BrazilLocale) }.orEmpty()
    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("month-summary")) {
        Text(stringResource(R.string.summary_overline).uppercase(), style = FluxaTheme.typography.overline, color = colors.textMuted)
        Text(monthLabel, style = FluxaTheme.typography.title, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        SummaryRow(stringResource(R.string.summary_income), summary.income, SummaryKind.Income, "summary-income")
        SummaryRow(stringResource(R.string.summary_expenses), summary.expenses, SummaryKind.Expense, "summary-expenses")
        Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(colors.border))
        SummaryRow(stringResource(R.string.summary_net), summary.net, SummaryKind.Net, "summary-net")
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.summary_explain), style = FluxaTheme.typography.caption, color = colors.textMuted)
    }
}

private enum class SummaryKind { Income, Expense, Net }

private val MonthTitle = java.time.format.DateTimeFormatter.ofPattern("MMMM 'de' yyyy", BrazilLocale)

@Composable
private fun SummaryRow(label: String, value: CalculatedBalance, kind: SummaryKind, tag: String) {
    val colors = FluxaTheme.colors
    val hidden = LocalAmountsHidden.current
    val exact = (value as? CalculatedBalance.Exact)?.amount
    val netState = if (kind == SummaryKind.Net && exact != null) {
        when {
            exact.cents > 0 -> R.string.summary_net_positive
            exact.cents < 0 -> R.string.summary_net_negative
            else -> R.string.summary_net_zero
        }
    } else null
    val color = when {
        kind == SummaryKind.Income && (exact?.cents ?: 0) != 0L -> colors.income
        kind == SummaryKind.Expense && (exact?.cents ?: 0) != 0L -> colors.expense
        netState == R.string.summary_net_positive -> colors.income
        netState == R.string.summary_net_negative -> colors.expense
        else -> colors.textPrimary
    }
    val stateText = netState?.let { stringResource(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) { if (stateText != null && !hidden) stateDescription = stateText }
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = FluxaTheme.typography.label, color = colors.textSecondary)
            // O estado do resultado não é exibido com valores ocultos (revelaria o sinal).
            if (stateText != null && !hidden) Text(stateText, style = FluxaTheme.typography.caption, color = colors.textMuted)
        }
        when {
            exact == null -> BalanceAmount(value, color = color)
            // Entradas e saídas chegam como magnitudes: o sinal é só apresentação (+ entrada, − saída).
            kind == SummaryKind.Expense && exact.cents != 0L -> FluxaAmount(amount = Money(-exact.cents), signed = true, color = color)
            else -> FluxaAmount(amount = exact, signed = kind != SummaryKind.Net || exact.cents != 0L, color = color)
        }
    }
}

// endregion
