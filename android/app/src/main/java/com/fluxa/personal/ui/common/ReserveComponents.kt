package com.fluxa.personal.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme

/*
 * Exibição de reservas de metas (Sprint 6-B). Todos os valores vêm de `GoalRepository.observeReserveBalances()` /
 * `observeAccountReserves()`; nada aqui soma ou subtrai. O disponível do contrato JÁ desconta as reservas elegíveis.
 */

/** Totais consolidados exatamente como o contrato entrega. */
internal data class ReserveTotals(val total: CalculatedBalance, val reserved: CalculatedBalance, val available: CalculatedBalance)

/** Só para decidir o que exibir (alerta, linhas extras); nenhum valor é derivado daqui. */
internal val CalculatedBalance?.isNegative: Boolean get() = (this as? CalculatedBalance.Exact)?.amount?.cents?.let { it < 0 } == true
internal val CalculatedBalance?.isNonZero: Boolean get() = this is CalculatedBalance.Overflow || (this as? CalculatedBalance.Exact)?.amount?.cents?.let { it != 0L } == true

/** Alerta de disponível negativo: ícone + texto, nunca só cor. */
@Composable
internal fun NegativeAvailableAlert(text: String, modifier: Modifier = Modifier, onInverse: Boolean = false) {
    val colors = FluxaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (onInverse) colors.onSurfaceInverse.copy(alpha = 0.12f) else colors.expenseContainer, FluxaTheme.shapes.medium)
            .padding(12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .testTag("negative-available-alert"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(FluxaIcons.Alert, null, tint = if (onInverse) colors.onSurfaceInverse else colors.expense, modifier = Modifier.size(18.dp))
        Text(text, style = FluxaTheme.typography.label, color = if (onInverse) colors.onSurfaceInverse else colors.expense)
    }
}

/** Linha rótulo/valor monetário (saldo real, reservado, disponível), sempre com valores do contrato. */
@Composable
internal fun ReserveLine(label: String, balance: CalculatedBalance?, modifier: Modifier = Modifier, color: Color = FluxaTheme.colors.textPrimary, labelColor: Color = FluxaTheme.colors.textSecondary) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 32.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = FluxaTheme.typography.label, color = labelColor, modifier = Modifier.weight(1f))
        BalanceAmount(balance, style = FluxaTheme.typography.bodyStrong, color = color)
    }
}

