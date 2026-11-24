package com.fluxa.personal.ui.designsystem.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.fluxaGlass

/**
 * Barra superior (equivalente a `TopAppBar`). Título de página forte, overline opcional e ações à direita.
 * `floating = true` aplica glass, para quando o conteúdo rolar por baixo.
 */
@Composable
internal fun FluxaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    floating: Boolean = false,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (floating) Modifier.fluxaGlass(FluxaTheme.shapes.extraLarge) else Modifier)
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 72.dp)
            .padding(start = if (navigation != null) 8.dp else 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        navigation?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            if (overline != null) {
                Text(overline.uppercase(), style = type.overline, color = colors.textMuted, maxLines = 1)
            }
            Text(
                text = title,
                style = type.headline,
                color = colors.textPrimary,
                maxLines = 2, // nomes longos (assinatura, compra, cartão) quebram em vez de sumir
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        actions()
    }
}

/** Cabeçalho de seção com ação opcional ("Ver tudo"). */
@Composable
internal fun FluxaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    actionModifier: Modifier = Modifier,
) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    Row(
        modifier = modifier.fillMaxWidth(),
        // Sem overline, título e ação ficam centrados um com o outro; com overline, a ação acompanha o título.
        verticalAlignment = if (overline == null) Alignment.CenterVertically else Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (overline != null) {
                Text(overline.uppercase(), style = type.overline, color = colors.textMuted)
            }
            Text(title, style = type.title, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
        }
        if (actionLabel != null && onAction != null) {
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = actionModifier
                    .heightIn(min = 48.dp)
                    .clip(FluxaTheme.shapes.pill)
                    .clickable(interaction, LocalIndication.current, role = Role.Button, onClick = onAction)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, style = type.label, color = colors.brand)
            }
        }
    }
}
