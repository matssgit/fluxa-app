package com.fluxa.personal.ui.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fluxa.personal.ui.designsystem.FluxaDepth
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.fluxaDepth
import com.fluxa.personal.ui.designsystem.fluxaGlass
import com.fluxa.personal.ui.designsystem.fluxaPressScale
import com.fluxa.personal.ui.designsystem.fluxaSpring
import com.fluxa.personal.ui.designsystem.fluxaTween

@Immutable
internal data class FluxaNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val testTag: String? = null,
)

@Immutable
internal data class FluxaNavAction(
    val contentDescription: String,
    val onClick: () -> Unit,
    val icon: ImageVector = FluxaIcons.Plus,
    val testTag: String? = null,
)

internal object FluxaNavigationBarDefaults {
    /** Espaço inferior que o conteúdo rolável deve reservar para não ficar sob a barra flutuante. */
    val ContentClearance = 112.dp
    val MaxWidth = 480.dp
}

/**
 * Navegação inferior flutuante em glass (equivalente a `NavigationBar` do M3, com FAB central integrado).
 * Itens: ícone outline → sólido quando ativo, pílula de indicação, rótulo sempre visível e em negrito quando ativo
 * (o estado não depende só de cor). Ação central `+` em verde petróleo, maior e elevada.
 * Itens são divididos igualmente antes e depois da ação central.
 */
@Composable
internal fun FluxaNavigationBar(
    items: List<FluxaNavItem>,
    selectedRoute: String?,
    onItemSelected: (FluxaNavItem) -> Unit,
    primaryAction: FluxaNavAction,
    modifier: Modifier = Modifier,
) {
    val half = items.size / 2
    val fade = FluxaTheme.colors.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Esmaece o conteúdo que passa por baixo da barra flutuante; não intercepta toques.
            .background(Brush.verticalGradient(0f to fade.copy(alpha = 0f), 0.4f to fade, 1f to fade))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = FluxaNavigationBarDefaults.MaxWidth)
                .fillMaxWidth()
                .fluxaGlass(FluxaTheme.shapes.pill)
                .heightIn(min = 76.dp)
                .selectableGroup()
                .padding(horizontal = 2.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.take(half).forEach { item -> NavItem(item, item.route == selectedRoute, onItemSelected) }
            // A ação central ocupa só o necessário (56dp + respiro); o espaço restante vai para os rótulos.
            PrimaryAction(primaryAction, Modifier.width(64.dp))
            items.drop(half).forEach { item -> NavItem(item, item.route == selectedRoute, onItemSelected) }
        }
    }
}

@Composable
private fun RowScope.NavItem(item: FluxaNavItem, selected: Boolean, onSelect: (FluxaNavItem) -> Unit) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val indicator by animateColorAsState(
        targetValue = if (selected) colors.selected else Color.Transparent,
        animationSpec = fluxaTween(),
        label = "nav-indicator",
    )
    val indicatorScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.85f,
        animationSpec = fluxaSpring(),
        label = "nav-indicator-scale",
    )
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(FluxaTheme.shapes.medium)
            .then(if (focused) Modifier.border(2.dp, colors.brand, FluxaTheme.shapes.medium) else Modifier)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = { onSelect(item) },
            )
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp)
            .then(if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = indicatorScale
                    scaleY = indicatorScale
                }
                .background(indicator, FluxaTheme.shapes.pill)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = null,
                tint = if (selected) colors.onSelected else colors.textMuted,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = item.label,
            style = type.caption.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
            color = if (selected) colors.textPrimary else colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PrimaryAction(action: FluxaNavAction, modifier: Modifier) {
    val colors = FluxaTheme.colors
    val shape = FluxaTheme.shapes.pill
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .fluxaPressScale(interaction)
                .fluxaDepth(FluxaDepth.Depth2, shape, bordered = false, highlight = Color.White.copy(alpha = 0.22f))
                .background(colors.action, shape)
                .then(if (focused) Modifier.border(2.dp, colors.accent, shape) else Modifier)
                .clip(shape)
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = action.onClick,
                )
                .semantics { contentDescription = action.contentDescription }
                .then(if (action.testTag != null) Modifier.testTag(action.testTag) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(action.icon, contentDescription = null, tint = colors.onAction, modifier = Modifier.size(26.dp))
        }
    }
}
