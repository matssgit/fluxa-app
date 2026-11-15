package com.fluxa.personal.ui.designsystem.components

import androidx.compose.animation.Crossfade
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.LocalAmountsHidden
import com.fluxa.personal.ui.designsystem.fluxaTween
import java.text.NumberFormat
import java.util.Locale

internal object FluxaMoneyFormat {
    const val MASK = "••••••"
    private val Brazil = Locale("pt", "BR")

    /**
     * Apresentação de [Money] em BRL ("R$ 1.234,56"). Só formata: nenhuma aritmética financeira aqui.
     * A conversão para reais usa [Money.toReais] do domínio. Sinal explícito opcional para entradas/saídas.
     */
    fun format(amount: Money, signed: Boolean = false): String {
        val formatter = NumberFormat.getCurrencyInstance(Brazil)
        val text = formatter.format(amount.toReais().abs())
        return when {
            amount.cents < 0 -> "−$text"
            signed && amount.cents > 0 -> "+$text"
            else -> text
        }
    }
}

/**
 * Valor financeiro ocultável. Todo valor financeiro deve passar por aqui.
 * Com privacidade ativa ([LocalAmountsHidden]) exibe "••••••" e anuncia "Valor oculto" para leitores de tela.
 * Recebe [Money] (centavos em Long), sem ponto flutuante.
 */
@Composable
internal fun FluxaAmount(
    amount: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = FluxaTheme.typography.amount,
    color: Color = LocalContentColor.current,
    signed: Boolean = false,
    hidden: Boolean = LocalAmountsHidden.current,
    hiddenDescription: String = stringResource(R.string.amount_hidden),
) {
    Crossfade(targetState = hidden, animationSpec = fluxaTween(), label = "amount-privacy", modifier = modifier) { isHidden ->
        if (isHidden) {
            Text(
                text = FluxaMoneyFormat.MASK,
                style = style,
                color = color,
                maxLines = 1,
                modifier = Modifier.semantics { contentDescription = hiddenDescription },
            )
        } else {
            Text(text = FluxaMoneyFormat.format(amount, signed), style = style, color = color, maxLines = 1)
        }
    }
}
