package com.fluxa.personal.ui.placeholder

import com.fluxa.personal.core.money.Money
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.fluxa.personal.R
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.LocalAmountsHidden
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaChip
import com.fluxa.personal.ui.designsystem.components.FluxaIconButton
import com.fluxa.personal.ui.designsystem.components.FluxaInput
import com.fluxa.personal.ui.designsystem.components.FluxaNavigationBarDefaults
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.designsystem.components.FluxaSurfaceTone
import com.fluxa.personal.ui.designsystem.components.FluxaTopBar

/*
 * Superfícies mínimas para validar o design system no shell. Não são telas definitivas
 * e não usam dados reais: os valores exibidos são fictícios.
 */

@Composable
private fun PlaceholderScreen(
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
            .testTag("screen-$route"),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = FluxaNavigationBarDefaults.ContentClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FluxaTopBar(title = title, overline = overline) {
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

private val Gutter = Modifier.fillMaxWidth().padding(horizontal = 16.dp)

@Composable
internal fun DashboardPlaceholder(onToggleAmounts: () -> Unit) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    PlaceholderScreen(
        route = "dashboard",
        title = stringResource(R.string.dashboard),
        overline = stringResource(R.string.placeholder_dashboard_overline),
        onToggleAmounts = onToggleAmounts,
    ) {
        item {
            FluxaCard(modifier = Gutter, tone = FluxaSurfaceTone.Inverse, featured = true, contentPadding = PaddingValues(24.dp)) {
                Text(
                    stringResource(R.string.placeholder_balance_label).uppercase(),
                    style = type.overline,
                    color = colors.onSurfaceInverseMuted,
                )
                Spacer(Modifier.height(12.dp))
                FluxaAmount(amount = Money(1_234_56), style = type.amountHero)
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.placeholder_preview_note), style = type.caption, color = colors.onSurfaceInverseMuted)
            }
        }
        item {
            Row(modifier = Gutter, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowTile(stringResource(R.string.filter_income), Money(4_200_00), colors.income, Modifier.weight(1f))
                FlowTile(stringResource(R.string.filter_expense), Money(-2_965_44), colors.expense, Modifier.weight(1f))
            }
        }
        item { FluxaSectionHeader(title = stringResource(R.string.coming_soon), modifier = Gutter) }
        item { BodyCard(stringResource(R.string.placeholder_dashboard_body)) }
    }
}

@Composable
private fun FlowTile(label: String, amount: Money, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    FluxaCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Text(label, style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        FluxaAmount(amount = amount, color = color, signed = true)
    }
}

@Composable
private fun BodyCard(text: String) {
    FluxaCard(modifier = Gutter) {
        Text(text, style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary)
    }
}

@Composable
internal fun CashPlaceholder(
    onToggleAmounts: () -> Unit,
    activeAccounts: Int? = null,
    onManageAccounts: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val filters = listOf(R.string.filter_all, R.string.filter_income, R.string.filter_expense, R.string.filter_pending)
    PlaceholderScreen(route = "cash", title = stringResource(R.string.cash), onToggleAmounts = onToggleAmounts) {
        // Contas ficam no topo do Caixa como atalho; o restante da aba continua reservado às movimentações.
        item { AccountsShortcut(activeAccounts, onManageAccounts) }
        item {
            FluxaInput(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.placeholder_cash_search_label),
                placeholder = stringResource(R.string.placeholder_cash_search_hint),
                leadingIcon = FluxaIcons.Search,
                modifier = Gutter,
            )
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filters.forEachIndexed { index, label ->
                    FluxaChip(
                        label = stringResource(label),
                        selected = filter == index,
                        onSelectedChange = { filter = index },
                    )
                }
            }
        }
        item { BodyCard(stringResource(R.string.placeholder_cash_body)) }
    }
}

@Composable
internal fun CardsPlaceholder(onToggleAmounts: () -> Unit) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    PlaceholderScreen(route = "cards", title = stringResource(R.string.cards), onToggleAmounts = onToggleAmounts) {
        item {
            FluxaCard(modifier = Gutter, tone = FluxaSurfaceTone.Inverse, featured = true, contentPadding = PaddingValues(24.dp)) {
                Text(stringResource(R.string.placeholder_card_label), style = type.titleSmall, color = colors.onSurfaceInverse)
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.placeholder_card_available).uppercase(),
                    style = type.overline,
                    color = colors.onSurfaceInverseMuted,
                )
                FluxaAmount(amount = Money(3_400_00), style = type.headline)
                Spacer(Modifier.height(16.dp))
                UsageBar(fraction = 0.32f)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.placeholder_preview_note), style = type.caption, color = colors.onSurfaceInverseMuted)
            }
        }
        item { BodyCard(stringResource(R.string.placeholder_cards_body)) }
    }
}

@Composable
private fun UsageBar(fraction: Float) {
    val on = FluxaTheme.colors.onSurfaceInverse
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(on.copy(alpha = 0.18f), FluxaTheme.shapes.pill),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .background(on, FluxaTheme.shapes.pill),
        )
    }
}

@Composable
private fun AccountsShortcut(activeAccounts: Int?, onManage: () -> Unit) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    FluxaCard(
        modifier = Gutter.testTag("cash-accounts-card"),
        onClick = onManage,
        onClickLabel = stringResource(R.string.accounts_cash_card_action),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(44.dp).background(colors.brandTonal, FluxaTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(FluxaIcons.Wallet, contentDescription = null, tint = colors.onBrandTonal, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.accounts_cash_card_title), style = type.bodyStrong, color = colors.textPrimary)
                Text(
                    text = when (activeAccounts) {
                        null -> stringResource(R.string.loading)
                        0 -> stringResource(R.string.accounts_cash_card_none)
                        else -> pluralStringResource(R.plurals.accounts_cash_card_count, activeAccounts, activeAccounts)
                    },
                    style = type.caption,
                    color = colors.textSecondary,
                )
                Text(stringResource(R.string.accounts_cash_card_action), style = type.label, color = colors.brand, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(FluxaIcons.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
        }
    }
}

private data class MoreDestination(val label: Int, val icon: ImageVector, val route: String? = null)

@Composable
internal fun MorePlaceholder(onToggleAmounts: () -> Unit, onOpenCategories: () -> Unit = {}) {
    val destinations = listOf(
        MoreDestination(R.string.more_subscriptions, FluxaIcons.Repeat),
        MoreDestination(R.string.more_goals, FluxaIcons.Target),
        MoreDestination(R.string.more_categories, FluxaIcons.Tag, route = "categories"),
        MoreDestination(R.string.more_analytics, FluxaIcons.Analytics),
        MoreDestination(R.string.more_settings, FluxaIcons.Sliders),
    )
    PlaceholderScreen(
        route = "more",
        title = stringResource(R.string.more),
        overline = stringResource(R.string.placeholder_more_overline),
        onToggleAmounts = onToggleAmounts,
    ) {
        item {
            FluxaCard(modifier = Gutter, contentPadding = PaddingValues(vertical = 4.dp)) {
                destinations.forEachIndexed { index, destination ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(start = 72.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(FluxaTheme.colors.border),
                        )
                    }
                    MoreRow(
                        label = stringResource(destination.label),
                        icon = destination.icon,
                        onClick = if (destination.route != null) onOpenCategories else null,
                        modifier = if (destination.route != null) Modifier.testTag("more-${destination.route}") else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreRow(label: String, icon: ImageVector, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = FluxaTheme.colors
    val soon = stringResource(R.string.coming_soon)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) { if (onClick == null) stateDescription = soon },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(colors.brandTonal, FluxaTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.onBrandTonal, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = FluxaTheme.typography.bodyStrong, color = colors.textPrimary)
        }
        if (onClick != null) {
            Icon(FluxaIcons.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
        } else {
            Text(
                soon,
                style = FluxaTheme.typography.caption,
                color = colors.textMuted,
                modifier = Modifier
                    .background(colors.surfaceSunken, FluxaTheme.shapes.pill)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}
