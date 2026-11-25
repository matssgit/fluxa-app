package com.fluxa.personal.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.LocalAmountsHidden
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.feature.catalog.categoryColorFor
import com.fluxa.personal.ui.feature.catalog.categoryIconFor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val BrazilLocale: Locale = Locale("pt", "BR")
/** Só para campos de data editáveis; datas de leitura usam o formato curto com o mês por extenso ("17 dez. 2026"). */
internal val BrazilDate: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", BrazilLocale)
private val DayHeader: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, d 'de' MMMM", BrazilLocale)
private val DateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy 'às' HH:mm", BrazilLocale)

internal fun Instant.formatLocal(): String = atZone(ZoneId.systemDefault()).format(DateTime)

@Composable
internal fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.today)
    today.minusDays(1) -> stringResource(R.string.yesterday)
    else -> date.format(DayHeader).replaceFirstChar { it.titlecase(BrazilLocale) }
}

/**
 * Saldo calculado pelo repositório. `Overflow` nunca vira zero: mostra um aviso com ícone e texto.
 * Respeita a ocultação de valores.
 */
@Composable
internal fun BalanceAmount(
    balance: CalculatedBalance?,
    modifier: Modifier = Modifier,
    style: TextStyle = FluxaTheme.typography.amount,
    color: Color = LocalContentColor.current,
) {
    when (balance) {
        is CalculatedBalance.Exact -> FluxaAmount(amount = balance.amount, modifier = modifier, style = style, color = color)
        CalculatedBalance.Overflow -> {
            if (LocalAmountsHidden.current) {
                FluxaAmount(amount = Money(0), modifier = modifier, style = style, color = color, hidden = true)
            } else {
                Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(FluxaIcons.Alert, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.balance_overflow), style = FluxaTheme.typography.label, color = color)
                }
            }
        }
        null -> Text(stringResource(R.string.balance_unavailable), style = FluxaTheme.typography.label, color = color, modifier = modifier)
    }
}

/** Valor de lançamento com sinal explícito e rótulo de direção para leitores de tela (não depende de cor). */
@Composable
internal fun SignedTransactionAmount(
    amount: Money,
    direction: TransactionDirection,
    cancelled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = FluxaTheme.colors
    val color = when {
        cancelled -> colors.textMuted
        direction == TransactionDirection.INCOME -> colors.income
        else -> colors.expense
    }
    val signed = if (direction == TransactionDirection.INCOME) amount else Money(-amount.cents)
    val style = FluxaTheme.typography.amount.copy(textDecoration = if (cancelled) TextDecoration.LineThrough else null)
    FluxaAmount(amount = signed, signed = true, style = style, color = color, modifier = modifier)
}

/** Selo do lançamento: ícone/cor da categoria, com marcador de direção (seta) no canto. */
@Composable
internal fun TransactionBadge(icon: String?, color: String?, direction: TransactionDirection, cancelled: Boolean) {
    val colors = FluxaTheme.colors
    val tone = if (cancelled) colors.textMuted else categoryColorFor(color)
    Box(modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(tone.copy(alpha = 0.2f), FluxaTheme.shapes.pill)
                .border(1.5.dp, tone, FluxaTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(categoryIconFor(icon), contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .background(if (direction == TransactionDirection.INCOME) colors.income else colors.expense, FluxaTheme.shapes.pill)
                .border(1.5.dp, colors.surface, FluxaTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (direction == TransactionDirection.INCOME) "+" else "−",
                style = FluxaTheme.typography.caption,
                color = colors.surface,
                modifier = Modifier.clearAndSetSemantics {}, // a direção já é anunciada pela linha
            )
        }
    }
}

/** Linha de lançamento para listas (Caixa e Início). */
@Composable
internal fun LedgerRow(
    title: String,
    meta: String,
    amount: Money,
    direction: TransactionDirection,
    cancelled: Boolean,
    icon: String?,
    color: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Saída gerada pelo pagamento de uma parcela (`installmentId != null`): só uma marca discreta, sem duplicar dados. */
    fromCard: Boolean = false,
    /** Saída gerada pelo pagamento de uma assinatura: marca discreta "Assinatura". */
    fromSubscription: Boolean = false,
) {
    val colors = FluxaTheme.colors
    val baseLabel = stringResource(if (direction == TransactionDirection.INCOME) R.string.direction_income else R.string.direction_expense)
    val cardLabel = stringResource(R.string.ledger_from_card)
    val subscriptionLabel = stringResource(R.string.origin_subscription)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { stateDescription = baseLabel },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TransactionBadge(icon, color, direction, cancelled)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = FluxaTheme.typography.bodyStrong,
                color = if (cancelled) colors.textSecondary else colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(meta, style = FluxaTheme.typography.caption, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (fromCard) StatePill(cardLabel, FluxaIcons.Card, modifier = Modifier.padding(top = 4.dp).testTag("ledger-from-card"))
            if (fromSubscription) StatePill(subscriptionLabel, FluxaIcons.Repeat, modifier = Modifier.padding(top = 4.dp).testTag("ledger-from-subscription"))
            if (cancelled) StatePill(stringResource(R.string.state_cancelled), FluxaIcons.Close, modifier = Modifier.padding(top = 4.dp))
        }
        SignedTransactionAmount(amount, direction, cancelled)
    }
}

/** Faz um item ocupar também o padding horizontal do contêiner (ex.: top bar dentro de uma LazyColumn com margem). */
internal fun Modifier.bleedHorizontal(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = (amount * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = (constraints.minWidth + extra).coerceAtLeast(0),
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-amount.roundToPx(), 0) }
}

/**
 * Pouca largura útil para o texto (largura dividida pela escala da fonte): linhas "rótulo + valor" passam a empilhar,
 * em vez de quebrar palavras ao meio em 320dp ou com fonte 1,3.
 */
@Composable
internal fun isNarrowForText(threshold: Int = 300): Boolean =
    with(LocalConfiguration.current) { screenWidthDp / fontScale <= threshold }

/** Erro de um grupo de seleção (conta, categoria): ícone + texto. */
@Composable
internal fun FieldError(text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(FluxaIcons.Alert, contentDescription = null, tint = FluxaTheme.colors.expense, modifier = Modifier.size(16.dp))
        Text(text, style = FluxaTheme.typography.caption, color = FluxaTheme.colors.expense)
    }
}

/** Campo de data com a aparência do FluxaInput; abre o seletor do M3. */
@Composable
internal fun DateField(
    label: String,
    supporting: String?,
    date: LocalDate,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
) {
    val colors = FluxaTheme.colors
    val shape = FluxaTheme.shapes.medium
    val formatted = date.format(BrazilDate)
    val description = stringResource(R.string.date_change, formatted)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldLabel(label)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(if (colors.isDark) colors.surfaceSunken else colors.surface, shape)
                .border(if (errorText != null) 2.dp else 1.dp, if (errorText != null) colors.expense else colors.borderStrong, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(FluxaIcons.Calendar, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
            Text(
                formatted, style = FluxaTheme.typography.body, color = colors.textPrimary,
                modifier = Modifier.weight(1f).semantics { contentDescription = description },
            )
        }
        when {
            errorText != null -> FieldError(errorText)
            supporting != null -> Text(supporting, style = FluxaTheme.typography.caption, color = colors.textMuted)
        }
    }
}

/**
 * Seletor de data do M3. O DatePicker usa meia-noite UTC em milissegundos; a conversão usa UTC nos dois sentidos.
 * [maxDate] limita a escolha (ex.: lançamentos não podem ser futuros).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FluxaDatePicker(
    initial: LocalDate,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    maxDate: LocalDate? = null,
) {
    val maxMillis = maxDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = maxMillis == null || utcTimeMillis <= maxMillis
            override fun isSelectableYear(year: Int) = maxDate == null || year <= maxDate.year
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        ?.let(onPicked) ?: onDismiss()
                },
                modifier = Modifier.testTag("date-confirm"),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = state, title = { Text(stringResource(R.string.date_pick_title), modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}
