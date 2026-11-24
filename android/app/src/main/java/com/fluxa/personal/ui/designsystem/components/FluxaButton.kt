package com.fluxa.personal.ui.designsystem.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fluxa.personal.R
import com.fluxa.personal.ui.designsystem.FluxaDepth
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.fluxaDepth
import com.fluxa.personal.ui.designsystem.fluxaGlass
import com.fluxa.personal.ui.designsystem.fluxaPressScale

internal enum class FluxaButtonVariant { Primary, Secondary, Glass, Destructive }

internal enum class FluxaButtonSize(val minHeight: Dp, val horizontalPadding: Dp) {
    Medium(48.dp, 20.dp),
    Large(56.dp, 24.dp),
}

@Immutable
private data class ButtonStyle(val container: Color, val content: Color, val border: Color?, val depth: FluxaDepth)

@Composable
private fun FluxaButtonVariant.style(): ButtonStyle {
    val c = FluxaTheme.colors
    return when (this) {
        FluxaButtonVariant.Primary -> ButtonStyle(c.brand, c.onBrand, null, FluxaDepth.Depth1)
        FluxaButtonVariant.Secondary -> ButtonStyle(c.surface, c.textPrimary, c.borderStrong, FluxaDepth.Depth1)
        FluxaButtonVariant.Glass -> ButtonStyle(Color.Transparent, c.textPrimary, null, FluxaDepth.Depth2)
        FluxaButtonVariant.Destructive -> ButtonStyle(c.expenseContainer, c.expense, null, FluxaDepth.Depth0)
    }
}

/**
 * Botão do Fluxa. Estados: normal, pressionado (escala + ripple), foco (anel de 2dp), desabilitado e
 * carregando (spinner no lugar do ícone, largura estável, cliques bloqueados).
 * Altura mínima de 48dp; cresce com o tamanho de fonte do sistema.
 */
@Composable
internal fun FluxaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FluxaButtonVariant = FluxaButtonVariant.Primary,
    size: FluxaButtonSize = FluxaButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    loadingDescription: String = stringResource(R.string.loading),
) {
    val style = variant.style()
    val colors = FluxaTheme.colors
    val shape: Shape = FluxaTheme.shapes.pill
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val active = enabled && !loading

    val surface = when {
        variant == FluxaButtonVariant.Glass -> Modifier.fluxaGlass(shape, style.depth)
        else -> Modifier
            .fluxaDepth(
                depth = if (enabled) style.depth else FluxaDepth.Depth0,
                shape = shape,
                bordered = style.border == null && variant != FluxaButtonVariant.Primary &&
                    variant != FluxaButtonVariant.Destructive,
                highlight = if (variant == FluxaButtonVariant.Primary) Color.White.copy(alpha = 0.18f) else null,
            )
            .then(if (style.border != null) Modifier.border(1.dp, style.border, shape) else Modifier)
            .background(style.container, shape)
    }

    Box(
        modifier = modifier
            .fluxaPressScale(interaction, active)
            .then(surface)
            .then(if (focused) Modifier.border(2.dp, colors.brand, shape) else Modifier)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = active,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { if (loading) stateDescription = loadingDescription }
            .defaultMinSize(minWidth = size.minHeight, minHeight = size.minHeight)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(PaddingValues(horizontal = size.horizontalPadding, vertical = 12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = style.content,
                    strokeWidth = 2.dp,
                )
                leadingIcon != null -> Icon(leadingIcon, contentDescription = null, tint = style.content, modifier = Modifier.size(20.dp))
            }
            Text(
                text = text,
                color = style.content,
                style = if (size == FluxaButtonSize.Large) FluxaTheme.typography.bodyStrong else FluxaTheme.typography.label,
                maxLines = 2, // com fonte ampliada o rótulo quebra em vez de ser cortado
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

internal enum class FluxaIconButtonVariant { Standard, Tonal, Glass }

/** Botão de ícone. `contentDescription` é obrigatório. Alvo de toque de 48dp. */
@Composable
internal fun FluxaIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FluxaIconButtonVariant = FluxaIconButtonVariant.Standard,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
) {
    val colors = FluxaTheme.colors
    val shape = FluxaTheme.shapes.pill
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val surface = when (variant) {
        FluxaIconButtonVariant.Standard -> Modifier
        FluxaIconButtonVariant.Tonal -> Modifier.background(colors.brandTonal, shape)
        FluxaIconButtonVariant.Glass -> Modifier.fluxaGlass(shape, FluxaDepth.Depth2)
    }
    val iconTint = when {
        tint != Color.Unspecified -> tint
        variant == FluxaIconButtonVariant.Tonal -> colors.onBrandTonal
        else -> colors.textPrimary
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .fluxaPressScale(interaction, enabled)
            .then(surface)
            .then(if (focused) Modifier.border(2.dp, colors.brand, shape) else Modifier)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = null,
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = iconTint, modifier = Modifier.size(22.dp))
    }
}
