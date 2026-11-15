package com.fluxa.personal.ui.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fluxa.personal.ui.designsystem.FluxaDepth
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.fluxaGlass
import com.fluxa.personal.ui.designsystem.fluxaPressScale
import com.fluxa.personal.ui.designsystem.fluxaTween

/**
 * Chip de filtro (equivalente a `FilterChip`). Selecionado = glass + ícone de check + peso maior,
 * portanto o estado não depende só de cor. Alvo de toque mínimo de 48dp.
 */
@Composable
internal fun FluxaChip(
    label: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    /** `Role.RadioButton` quando o chip faz parte de uma escolha única. */
    role: Role = Role.Checkbox,
) {
    val colors = FluxaTheme.colors
    val shape = FluxaTheme.shapes.pill
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val surface = if (selected) {
        Modifier
            .fluxaGlass(shape, FluxaDepth.Depth1)
            .border(1.dp, colors.brand.copy(alpha = 0.35f), shape)
    } else {
        Modifier
            .background(colors.surfaceSunken, shape)
            .border(1.dp, colors.border, shape)
    }

    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .fluxaPressScale(interaction, enabled)
            .then(surface)
            .then(if (focused) Modifier.border(2.dp, colors.brand, shape) else Modifier)
            .clip(shape)
            .toggleable(
                value = selected,
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = role,
                onValueChange = onSelectedChange,
            )
            .heightIn(min = 40.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(fluxaTween()) + scaleIn(fluxaTween(), initialScale = 0.6f),
            exit = fadeOut(fluxaTween()) + scaleOut(fluxaTween(), targetScale = 0.6f),
        ) {
            Icon(FluxaIcons.Check, contentDescription = null, tint = colors.brand, modifier = Modifier.size(16.dp))
        }
        if (leadingIcon != null && !selected) {
            Icon(leadingIcon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
        }
        Text(
            text = label,
            style = FluxaTheme.typography.label.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            ),
            color = if (selected) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
        )
    }
}
