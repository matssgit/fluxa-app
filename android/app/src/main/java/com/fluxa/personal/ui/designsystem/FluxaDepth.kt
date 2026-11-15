package com.fluxa.personal.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Níveis de profundidade. Sombra externa suave + borda quase invisível + highlight interno no topo.
 * Sem neumorphism de duas sombras.
 *
 * - [Depth0]: plano/afundado (inputs, trilhas, segmented). Sem sombra, inner shade discreto.
 * - [Depth1]: cards de conteúdo.
 * - [Depth2]: hero e cards pressionáveis em destaque.
 * - [Depth3]: elementos flutuantes (navegação, FAB, sheets).
 */
internal enum class FluxaDepth(
    val elevation: Dp,
    val ambientAlpha: Float,
    val spotAlpha: Float,
    val highlight: Boolean,
) {
    Depth0(0.dp, 0f, 0f, highlight = false),
    Depth1(3.dp, 0.05f, 0.10f, highlight = true),
    Depth2(8.dp, 0.07f, 0.16f, highlight = true),
    Depth3(16.dp, 0.10f, 0.24f, highlight = true),
}

/**
 * Aplica sombra, borda e highlight do nível. Não pinta o fundo: combine com `background` ou use [FluxaSurface].
 */
internal fun Modifier.fluxaDepth(
    depth: FluxaDepth,
    shape: Shape,
    bordered: Boolean = true,
    highlight: Color? = null,
): Modifier = composed {
    val colors = FluxaTheme.colors
    var result = this
    if (depth.elevation > 0.dp) {
        result = result.shadow(
            elevation = depth.elevation,
            shape = shape,
            clip = false,
            ambientColor = colors.shadow.copy(alpha = depth.ambientAlpha),
            spotColor = colors.shadow.copy(alpha = depth.spotAlpha),
        )
    }
    if (bordered) result = result.border(1.dp, colors.border, shape)
    if (depth.highlight) result = result.border(1.dp, highlightBrush(highlight ?: colors.highlight), shape)
    result
}

/** Sombra interna discreta para superfícies afundadas (Depth0): 8dp de sombreamento no topo. */
internal fun Modifier.fluxaInnerShade(shape: Shape): Modifier = composed {
    val shadow = FluxaTheme.colors.shadow
    val alpha = if (FluxaTheme.colors.isDark) 0.18f else 0.05f
    this.background(
        brush = Brush.verticalGradient(
            0f to shadow.copy(alpha = alpha),
            0.35f to Color.Transparent,
        ),
        shape = shape,
    )
}

internal fun highlightBrush(highlight: Color): Brush = Brush.verticalGradient(
    0f to highlight,
    0.45f to Color.Transparent,
)
