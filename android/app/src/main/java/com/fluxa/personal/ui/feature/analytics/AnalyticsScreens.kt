package com.fluxa.personal.ui.feature.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.CashFlowMonth
import com.fluxa.personal.domain.model.FinancialInsight
import com.fluxa.personal.domain.model.FinancialInsightType
import com.fluxa.personal.domain.model.MonthlyFinancialSummary
import com.fluxa.personal.domain.model.SubscriptionAnalytics
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.common.BrazilLocale
import com.fluxa.personal.ui.common.EmptyStateCard
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.LoadingBlock
import com.fluxa.personal.ui.common.ReserveLine
import com.fluxa.personal.ui.common.isNarrowForText
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaPalette
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.LocalAmountsHidden
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaIconButton
import com.fluxa.personal.ui.designsystem.components.FluxaMoneyFormat
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.designsystem.fluxaTween
import com.fluxa.personal.ui.feature.catalog.categoryColorFor
import com.fluxa.personal.ui.feature.catalog.categoryIconFor
import com.fluxa.personal.ui.feature.subscriptions.RowDivider
import com.fluxa.personal.ui.feature.subscriptions.short
import com.fluxa.personal.ui.feature.subscriptions.spoken
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val MonthLong: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", BrazilLocale)
private val MonthShort: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM", BrazilLocale)

private fun YearMonth.long(): String = format(MonthLong)
private fun YearMonth.title(): String = long().replaceFirstChar { it.titlecase(BrazilLocale) }
private fun YearMonth.shortLabel(): String = format(MonthShort).removeSuffix(".")

/**
 * Cores do gráfico. O par receita/despesa do texto se confunde em deuteranopia (ΔE 5); petróleo × terracota da própria
 * paleta se separa por luminosidade nos dois temas (ΔE ≥ 14, conferido com validador). Posição e legenda reforçam.
 */
private val expenseBar: Color get() = FluxaPalette.Terracotta500

@Composable
internal fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onBack: () -> Unit,
    onOpenSafeToSpend: () -> Unit,
    onOpenSubscriptions: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FeatureScreen(
        title = stringResource(R.string.analytics_title),
        overline = stringResource(R.string.analytics_overline),
        onBack = onBack,
        modifier = Modifier.testTag("screen-analytics"),
    ) {
        item(key = "intro") {
            Text(stringResource(R.string.analytics_intro), style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary)
        }
        when (val s = state) {
            AnalyticsUiState.Loading -> item(key = "loading") { LoadingBlock() }
            is AnalyticsUiState.Error -> item(key = "error") { ErrorBanner(stringResource(s.message), onDismiss = null) }
            is AnalyticsUiState.Content -> {
                if (s.isEmpty) {
                    item(key = "empty") {
                        EmptyStateCard(
                            stringResource(R.string.analytics_empty_title), stringResource(R.string.analytics_empty_body),
                            FluxaIcons.Analytics, modifier = Modifier.testTag("analytics-empty"),
                        )
                    }
                    return@FeatureScreen
                }
                // Bloco do mês escolhido.
                item(key = "month") { MonthSelector(s, viewModel::previousMonth, viewModel::nextMonth) }
                item(key = "summary") { SummaryCard(s) }
                if (s.insights.isNotEmpty()) item(key = "insights") { InsightsSection(s) }
                // Mês sem nenhuma movimentação já diz isso no resumo; uma segunda frase vazia só repetiria.
                if (s.hasMovements) item(key = "categories") { CategoriesSection(s) }
                item(key = "evolution") { EvolutionSection(s) }
                // Bloco de hoje: não depende do mês escolhido acima.
                item(key = "projection") { ProjectionSection(s, onOpenSafeToSpend) }
                item(key = "subscriptions") { SubscriptionsSection(s.subscriptions, onOpenSubscriptions) }
            }
        }
    }
}

// region Mês

@Composable
private fun MonthSelector(s: AnalyticsUiState.Content, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FluxaIconButton(
            FluxaIcons.Back, stringResource(R.string.analytics_previous_month), onClick = onPrevious,
            modifier = Modifier.testTag("analytics-previous"),
        )
        Text(
            s.month.title(),
            style = if (isNarrowForText()) FluxaTheme.typography.titleSmall else FluxaTheme.typography.title,
            color = FluxaTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading(); liveRegion = LiveRegionMode.Polite }.testTag("analytics-month"),
        )
        FluxaIconButton(
            FluxaIcons.ChevronRight, stringResource(R.string.analytics_next_month), onClick = onNext, enabled = !s.isCurrentMonth,
            // Desabilitada, mas reconhecível: com a transparência padrão sumia em tela física.
            disabledTint = FluxaTheme.colors.textMuted,
            modifier = Modifier.testTag("analytics-next"),
        )
    }
}

// endregion

// region Resumo

@Composable
private fun SummaryCard(s: AnalyticsUiState.Content) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val m = s.summary
    FluxaCard(modifier = Modifier.fillMaxWidth().testTag("analytics-summary")) {
        if (!s.hasMovements) {
            Text(stringResource(R.string.analytics_month_empty), style = type.body, color = colors.textSecondary)
            return@FluxaCard
        }
        val figures: @Composable (Modifier) -> Unit = { mod ->
            Figure(stringResource(R.string.analytics_income), m.incomeRealized, colors.income, "analytics-income", mod)
            Figure(stringResource(R.string.analytics_expense), m.expenseRealized, colors.expense, "analytics-expense", mod)
        }
        if (isNarrowForText()) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { figures(Modifier) }
        else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { figures(Modifier.weight(1f)) }
        Spacer(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(colors.border))
        NetResult(m)
        SavingsRate(m)
        Spacer(Modifier.height(10.dp))
        Text(
            pluralStringResource(R.plurals.analytics_transactions, m.transactionCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), m.transactionCount) +
                " " + stringResource(R.string.analytics_realized_note),
            style = type.caption, color = colors.textMuted,
        )
    }
}

@Composable
private fun Figure(label: String, amount: Money, dot: Color, tag: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}.testTag(tag)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(8.dp).background(dot, FluxaTheme.shapes.pill))
            Text(label, style = FluxaTheme.typography.label, color = FluxaTheme.colors.textSecondary)
        }
        FluxaAmount(amount = amount, style = FluxaTheme.typography.amount, color = FluxaTheme.colors.textPrimary)
    }
}

@Composable
private fun NetResult(m: MonthlyFinancialSummary) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val hidden = LocalAmountsHidden.current
    Column(modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("analytics-net")) {
        Text(stringResource(R.string.analytics_net), style = type.label, color = colors.textSecondary)
        FluxaAmount(
            amount = m.netResult,
            signed = true,
            style = type.headline.merge(type.amount).copy(fontSize = type.headline.fontSize),
            // Com valores ocultos a cor não pode denunciar o sinal.
            color = when {
                hidden -> colors.textPrimary
                m.netResult.cents > 0 -> colors.income
                m.netResult.cents < 0 -> colors.expense
                else -> colors.textPrimary
            },
        )
    }
}

/** A razão revela sinal e proporção do resultado: some junto com os valores. Sem receita não existe taxa (nunca 0%). */
@Composable
private fun SavingsRate(m: MonthlyFinancialSummary) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val hidden = LocalAmountsHidden.current
    val hiddenLabel = stringResource(R.string.amount_hidden)
    val rate = m.savingsRate
    if (rate == null) {
        Text(
            stringResource(R.string.analytics_no_income), style = type.caption, color = colors.textSecondary,
            modifier = Modifier.padding(top = 6.dp).testTag("analytics-no-income"),
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp).semantics(mergeDescendants = true) {}.testTag("analytics-savings"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.analytics_savings_rate), style = type.label, color = colors.textSecondary, modifier = Modifier.weight(1f))
        Text(
            if (hidden) FluxaMoneyFormat.MASK else PercentFormat.ratio(rate), style = type.bodyStrong, color = colors.textPrimary,
            modifier = if (hidden) Modifier.semantics { contentDescription = hiddenLabel } else Modifier,
        )
    }
}

// endregion

// region Insights

@Composable
private fun InsightsSection(s: AnalyticsUiState.Content) {
    val visible = visibleInsights(s.insights, hidden = LocalAmountsHidden.current)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FluxaSectionHeader(title = stringResource(R.string.analytics_insights))
        FluxaCard(modifier = Modifier.fillMaxWidth().testTag("analytics-insights"), contentPadding = PaddingValues(vertical = 4.dp)) {
            visible.forEachIndexed { i, insight ->
                if (i > 0) RowDivider()
                InsightRow(insight, s)
            }
            if (visible.size < s.insights.size) {
                if (visible.isNotEmpty()) RowDivider()
                Text(
                    stringResource(R.string.analytics_insights_hidden), style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textSecondary,
                    modifier = Modifier.padding(16.dp).testTag("analytics-insights-hidden"),
                )
            }
        }
    }
}

@Composable
private fun InsightRow(insight: FinancialInsight, s: AnalyticsUiState.Content) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val month = s.month.long()
    val topName = s.categories.firstOrNull { it.spending.categoryId == insight.categoryId }?.spending?.categoryName.orEmpty()
    val text = when (insight.type) {
        FinancialInsightType.NEGATIVE_MONTH ->
            if (s.isCurrentMonth) stringResource(R.string.insight_negative_current) else stringResource(R.string.insight_negative_past, month)
        FinancialInsightType.POSITIVE_SAVINGS ->
            if (s.isCurrentMonth) stringResource(R.string.insight_positive_current) else stringResource(R.string.insight_positive_past, month)
        FinancialInsightType.EXPENSES_INCREASED ->
            stringResource(R.string.insight_expenses_increased, insight.comparisonPeriod?.long().orEmpty())
        FinancialInsightType.TOP_CATEGORY ->
            if (s.isCurrentMonth) stringResource(R.string.insight_top_category_current, topName)
            else stringResource(R.string.insight_top_category_past, topName, month)
        FinancialInsightType.NO_EXPENSES -> stringResource(R.string.insight_no_expenses, month)
    }
    val icon: ImageVector = when (insight.type) {
        FinancialInsightType.NEGATIVE_MONTH -> FluxaIcons.Minus
        FinancialInsightType.POSITIVE_SAVINGS -> FluxaIcons.Plus
        FinancialInsightType.EXPENSES_INCREASED -> FluxaIcons.Analytics
        FinancialInsightType.TOP_CATEGORY -> FluxaIcons.Tag
        FinancialInsightType.NO_EXPENSES -> FluxaIcons.Check
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {}.testTag("insight-${insight.type.name}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(colors.brandTonal, FluxaTheme.shapes.pill), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.onBrandTonal, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text, style = type.label, color = colors.textPrimary)
            when (insight.type) {
                FinancialInsightType.EXPENSES_INCREASED -> insight.changeRate?.let {
                    Text(stringResource(R.string.insight_change, PercentFormat.ratio(it, signed = true)), style = type.caption, color = colors.textSecondary)
                }
                FinancialInsightType.NEGATIVE_MONTH, FinancialInsightType.POSITIVE_SAVINGS, FinancialInsightType.TOP_CATEGORY ->
                    insight.primaryValue?.let {
                        FluxaAmount(
                            amount = it, signed = insight.type == FinancialInsightType.POSITIVE_SAVINGS,
                            style = type.caption, color = colors.textSecondary,
                        )
                    }
                FinancialInsightType.NO_EXPENSES -> Unit
            }
        }
    }
}

// endregion

// region Categorias

@Composable
private fun CategoriesSection(s: AnalyticsUiState.Content) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FluxaSectionHeader(title = stringResource(R.string.analytics_categories))
        FluxaCard(modifier = Modifier.fillMaxWidth().testTag("analytics-categories")) {
            if (!s.hasExpenses) {
                Text(
                    stringResource(R.string.analytics_no_expenses), style = type.body, color = colors.textSecondary,
                    modifier = Modifier.testTag("analytics-no-expenses"),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    s.categories.forEach { CategoryRow(it, s.summary.month) }
                    if (s.otherCategories > 0) {
                        Text(
                            pluralStringResource(R.plurals.analytics_other_categories, s.otherCategories, s.otherCategories),
                            style = type.caption, color = colors.textMuted, modifier = Modifier.testTag("analytics-other-categories"),
                        )
                    }
                }
            }
        }
    }
}

/** "Alimentação. R$ 850,00. 32 por cento das despesas." A barra é só desenho; texto é a fonte de leitura. */
@Composable
private fun CategoryRow(item: CategoryItem, month: YearMonth) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val color = categoryColorFor(item.color)
    val share = PercentFormat.share(item.spending)
    val spokenShare = stringResource(R.string.analytics_share_spoken, share.removeSuffix("%").replace("<", stringResource(R.string.analytics_less_than) + " "))
    val narrow = isNarrowForText()
    val growth = rememberGrowth(month to item.spending.categoryId)
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("category-share-${item.spending.categoryName}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(color.copy(alpha = 0.2f), FluxaTheme.shapes.pill).border(1.5.dp, color, FluxaTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(categoryIconFor(item.icon), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val amount: @Composable () -> Unit = { FluxaAmount(amount = item.spending.amount, style = type.amount, color = colors.textPrimary) }
            if (narrow) {
                Text(item.spending.categoryName, style = type.label, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                amount()
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.spending.categoryName, style = type.label, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    amount()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val track = colors.surfaceSunken
                Canvas(Modifier.weight(1f).height(8.dp).clip(FluxaTheme.shapes.pill).background(track)) {
                    val w = size.width * (item.spending.percentage / 100f) * growth.value
                    drawRoundRect(color, size = Size(w.coerceAtLeast(if (item.spending.amount.cents > 0) 4.dp.toPx() else 0f), size.height), cornerRadius = CornerRadius(4.dp.toPx()))
                }
                Text(
                    share, style = type.caption, color = colors.textSecondary,
                    modifier = Modifier.semantics { contentDescription = spokenShare },
                )
            }
        }
    }
}

// endregion

// region Evolução

@Composable
private fun EvolutionSection(s: AnalyticsUiState.Content) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val hidden = LocalAmountsHidden.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FluxaSectionHeader(title = stringResource(R.string.analytics_evolution))
        FluxaCard(modifier = Modifier.fillMaxWidth().testTag("analytics-evolution")) {
            if (!s.hasEvolution) {
                Text(stringResource(R.string.analytics_evolution_empty), style = type.body, color = colors.textSecondary)
                return@FluxaCard
            }
            if (hidden) {
                // A altura relativa das barras denunciaria o sinal do resultado, que o app já esconde junto com os valores.
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.surfaceSunken, FluxaTheme.shapes.medium).padding(14.dp).testTag("analytics-chart-hidden"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(FluxaIcons.EyeOff, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.analytics_chart_hidden), style = type.label, color = colors.textSecondary)
                }
            } else {
                Legend()
                Spacer(Modifier.height(12.dp))
                EvolutionChart(s)
            }
            Spacer(Modifier.height(12.dp))
            // Lista do mais recente para o mais antigo: leitura principal, inclusive para o TalkBack.
            s.evolution.asReversed().forEachIndexed { i, month ->
                if (i > 0) Spacer(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                EvolutionRow(month)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend() {
    // Legenda só visual: a lista abaixo já nomeia cada valor.
    FlowRow(modifier = Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LegendItem(FluxaTheme.colors.brand, stringResource(R.string.analytics_income))
        LegendItem(expenseBar, stringResource(R.string.analytics_expense))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).background(color, FluxaTheme.shapes.small))
        Text(label, style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textSecondary)
    }
}

@Composable
private fun EvolutionChart(s: AnalyticsUiState.Content) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val growth = rememberGrowth(s.month)
    val months = s.evolution
    val peak = s.evolutionPeak.cents
    val description = stringResource(R.string.analytics_chart_description, months.first().month.long(), months.last().month.long())
    val incomeColor = colors.brand
    val grid = colors.border
    val baseline = colors.borderStrong
    Column(modifier = Modifier.fillMaxWidth().testTag("analytics-chart").clearAndSetSemantics { contentDescription = description }) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.analytics_chart_top), style = type.caption, color = colors.textMuted)
            FluxaAmount(amount = s.evolutionPeak, style = type.caption, color = colors.textMuted)
        }
        Spacer(Modifier.height(4.dp))
        // Os valores só são lidos na fase de desenho: a animação não recompõe a tela.
        Canvas(Modifier.fillMaxWidth().height(148.dp)) {
            listOf(0f, 0.5f).forEach { f -> drawLine(grid, Offset(0f, size.height * f), Offset(size.width, size.height * f), strokeWidth = 1.dp.toPx()) }
            val slot = size.width / months.size
            val gap = 2.dp.toPx()
            val bar = minOf(18.dp.toPx(), (slot - gap) * 0.36f)
            months.forEachIndexed { i, m ->
                val center = slot * i + slot / 2
                column(m.income.cents, peak, center - gap / 2 - bar, bar, incomeColor, growth.value)
                column(m.expense.cents, peak, center + gap / 2, bar, expenseBar, growth.value)
            }
            drawLine(baseline, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            months.forEach { m ->
                val selected = m.month == s.month
                Text(
                    m.month.shortLabel(),
                    style = type.caption.copy(fontWeight = if (selected) FontWeight.Bold else type.caption.fontWeight),
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Fração em ponto flutuante só para pixels; nenhum valor monetário passa por aqui. */
private fun DrawScope.column(cents: Long, peak: Long, x: Float, width: Float, color: Color, growth: Float) {
    if (cents <= 0L || peak <= 0L) return
    val h = (size.height * (cents.toDouble() / peak.toDouble()).toFloat() * growth).coerceAtLeast(2.dp.toPx())
    val r = CornerRadius(minOf(4.dp.toPx(), h))
    val path = Path().apply {
        addRoundRect(RoundRect(x, size.height - h, x + width, size.height, topLeftCornerRadius = r, topRightCornerRadius = r))
    }
    drawPath(path, color)
}

/** Meses sem dados aparecem como zero. Lido como uma frase: "Novembro de 2026. Entradas… Despesas… Resultado…". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EvolutionRow(m: CashFlowMonth) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val hidden = LocalAmountsHidden.current
    val mask = stringResource(R.string.amount_hidden)
    fun spoken(amount: Money, signed: Boolean = false) = if (hidden) mask else FluxaMoneyFormat.format(amount, signed)
    val description = stringResource(
        R.string.analytics_evolution_spoken, m.month.title(), spoken(m.income), spoken(m.expense), spoken(m.net, signed = true),
    )
    val narrow = isNarrowForText(340)
    val net: @Composable () -> Unit = {
        FluxaAmount(
            amount = m.net, signed = true, style = type.amount,
            color = when {
                hidden || m.net.cents == 0L -> colors.textPrimary
                m.net.cents > 0 -> colors.income
                else -> colors.expense
            },
        )
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).semantics(mergeDescendants = true) { contentDescription = description }
            .testTag("evolution-${m.month}"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(m.month.title(), style = type.label, color = colors.textPrimary, modifier = Modifier.weight(1f).clearAndSetSemantics {})
            if (!narrow) Box(Modifier.clearAndSetSemantics {}) { net() }
        }
        if (narrow) Box(Modifier.clearAndSetSemantics {}) { net() }
        FlowRow(modifier = Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowPart(stringResource(R.string.analytics_income), m.income)
            FlowPart(stringResource(R.string.analytics_expense), m.expense)
        }
    }
}

@Composable
private fun FlowPart(label: String, amount: Money) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textSecondary)
        FluxaAmount(amount = amount, style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textSecondary)
    }
}

/** Entrada discreta dos gráficos; com animações reduzidas aparece pronto. */
@Composable
private fun rememberGrowth(key: Any): Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    // Uma vez por mês exibido: rolar para fora e voltar não repete a entrada.
    var played by rememberSaveable(key.toString()) { mutableStateOf(false) }
    val growth = remember(key) { Animatable(if (played) 1f else 0f) }
    val spec = fluxaTween<Float>(GROWTH_MS)
    LaunchedEffect(growth) {
        growth.animateTo(1f, spec)
        played = true
    }
    return growth
}

private const val GROWTH_MS = 420

// endregion

// region Projeção

@Composable
private fun ProjectionSection(s: AnalyticsUiState.Content, onOpenSafeToSpend: () -> Unit) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val hidden = LocalAmountsHidden.current
    val p = s.projection
    fun tone(amount: Money) = if (!hidden && amount.cents < 0) colors.expense else colors.textPrimary
    val endDate = p.horizonEndExclusive.minusDays(1)
    val stacked = isNarrowForText()
    val monthEnd = s.remainingMonth.horizonEndExclusive.minusDays(1)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FluxaSectionHeader(title = stringResource(R.string.analytics_projection))
        FluxaCard(modifier = Modifier.fillMaxWidth().testTag("analytics-projection")) {
            val fromToday = stringResource(R.string.analytics_from_today, s.today.short())
            val fromTodaySpoken = stringResource(R.string.analytics_from_today, s.today.spoken())
            Text(fromToday, style = type.caption, color = colors.textMuted, modifier = Modifier.semantics { contentDescription = fromTodaySpoken })
            Spacer(Modifier.height(6.dp))
            ReserveLine(
                stringResource(R.string.analytics_available_now), CalculatedBalance.Exact(p.availableNow),
                stacked = stacked, modifier = Modifier.testTag("projection-available"), color = tone(p.availableNow),
            )
            if (p.obligationsTotal.cents > 0) {
                // Magnitude do contrato exibida como saída; a conta já vem pronta em projectedEndingBalance.
                ReserveLine(
                    stringResource(R.string.analytics_obligations), CalculatedBalance.Exact(Money(-p.obligationsTotal.cents)),
                    stacked = stacked, modifier = Modifier.testTag("projection-obligations"),
                )
            } else {
                Text(
                    stringResource(R.string.analytics_no_obligations), style = type.caption, color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = 4.dp).testTag("projection-no-obligations"),
                )
            }
            // Hoje o domínio não tem receita futura confirmada: a linha só existe quando há valor.
            if (p.confirmedIncomeTotal.cents > 0) {
                ReserveLine(
                    stringResource(R.string.analytics_confirmed_income), CalculatedBalance.Exact(p.confirmedIncomeTotal),
                    stacked = stacked, modifier = Modifier.testTag("projection-income"),
                )
            }
            ProjectionDivider()
            Column(modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("projection-ending")) {
                Text(
                    stringResource(R.string.analytics_projected_on, endDate.short()), style = type.label, color = colors.textSecondary,
                    modifier = Modifier.spokenAs(stringResource(R.string.analytics_projected_on, endDate.spoken())),
                )
                FluxaAmount(
                    amount = p.projectedEndingBalance,
                    style = type.headline.merge(type.amount).copy(fontSize = type.headline.fontSize), color = tone(p.projectedEndingBalance),
                )
            }
            ProjectionDivider()
            val lowest: @Composable (Modifier) -> Unit = { mod -> Column(modifier = mod.semantics(mergeDescendants = true) {}.testTag("projection-lowest"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.analytics_lowest), style = type.caption, color = colors.textSecondary)
                FluxaAmount(amount = p.lowestProjectedBalance, style = type.amount, color = tone(p.lowestProjectedBalance))
                if (p.lowestBalanceDate == s.today) Text(stringResource(R.string.analytics_lowest_today), style = type.caption, color = colors.textSecondary)
                else Text(
                    stringResource(R.string.analytics_lowest_on, p.lowestBalanceDate.short()), style = type.caption, color = colors.textSecondary,
                    modifier = Modifier.spokenAs(stringResource(R.string.analytics_lowest_on, p.lowestBalanceDate.spoken())),
                )
            } }
            // Contexto do mês corrente, sem uma segunda tela de projeção.
            val atMonthEnd: @Composable (Modifier) -> Unit = { mod -> Column(modifier = mod.semantics(mergeDescendants = true) {}.testTag("projection-month"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.analytics_month_end_label), style = type.caption, color = colors.textSecondary,
                    modifier = Modifier.spokenAs(stringResource(R.string.analytics_month_end, monthEnd.spoken())),
                )
                FluxaAmount(amount = s.remainingMonth.projectedEndingBalance, style = type.amount, color = tone(s.remainingMonth.projectedEndingBalance))
                Text(
                    stringResource(R.string.analytics_lowest_on, monthEnd.short()), style = type.caption, color = colors.textSecondary,
                    modifier = Modifier.clearAndSetSemantics {}, // a data já está no rótulo falado
                )
            } }
            // Os dois pontos secundários dividem a linha quando cabem inteiros.
            when {
                monthEnd == endDate -> lowest(Modifier)
                isNarrowForText(350) -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { lowest(Modifier); atMonthEnd(Modifier) }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { lowest(Modifier.weight(1f)); atMonthEnd(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(16.dp))
            val note = type.caption.copy(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Normal)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.analytics_projection_note), style = note, color = colors.textMuted)
                Text(stringResource(R.string.analytics_safe_note), style = note, color = colors.textMuted)
            }
            Spacer(Modifier.height(14.dp))
            FluxaButton(
                text = stringResource(R.string.analytics_open_safe), onClick = onOpenSafeToSpend, variant = FluxaButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().testTag("action-analytics-safe"),
            )
        }
    }
}

private fun Modifier.spokenAs(text: String): Modifier = semantics { contentDescription = text }

@Composable
private fun ProjectionDivider() {
    Spacer(Modifier.padding(vertical = 14.dp).fillMaxWidth().height(1.dp).background(FluxaTheme.colors.border))
}

// endregion

// region Assinaturas

@Composable
private fun SubscriptionsSection(a: SubscriptionAnalytics, onOpenSubscriptions: () -> Unit) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FluxaSectionHeader(title = stringResource(R.string.analytics_subscriptions))
        FluxaCard(modifier = Modifier.fillMaxWidth().testTag("analytics-subscriptions")) {
            if (a.activeCount == 0L) {
                Text(stringResource(R.string.analytics_no_subscriptions), style = type.body, color = colors.textSecondary)
                return@FluxaCard
            }
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp).semantics(mergeDescendants = true) {}.testTag("subscriptions-active"),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.analytics_active_subscriptions), style = type.label, color = colors.textSecondary, modifier = Modifier.weight(1f))
                Text(a.activeCount.toString(), style = type.bodyStrong, color = colors.textPrimary)
            }
            ReserveLine(
                stringResource(R.string.analytics_monthly_equivalent), CalculatedBalance.Exact(a.monthlyEquivalentCost),
                stacked = isNarrowForText(), modifier = Modifier.testTag("subscriptions-monthly"),
            )
            ReserveLine(
                stringResource(R.string.analytics_annual_cost), CalculatedBalance.Exact(a.annualProjectedCost),
                stacked = isNarrowForText(), modifier = Modifier.testTag("subscriptions-annual"),
            )
            Text(stringResource(R.string.analytics_equivalent_note), style = type.caption, color = colors.textMuted, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(12.dp))
            FluxaButton(
                text = stringResource(R.string.analytics_open_subscriptions), onClick = onOpenSubscriptions, variant = FluxaButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().testTag("action-analytics-subscriptions"),
            )
        }
    }
}

// endregion
