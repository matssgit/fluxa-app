package com.fluxa.personal.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Liquid glass seletivo.
 *
 * Uso permitido: navegação inferior, controles flutuantes, sheets, chips ativos e FAB.
 * Proibido em cards de dados financeiros.
 *
 * Implementação deliberadamente sem blur de fundo: Compose não oferece backdrop blur nativo, e
 * `RenderEffect` (API 31+) desfocaria o próprio conteúdo. O efeito vem de superfície translúcida (alpha ≥ 0.86,
 * legível sobre qualquer conteúdo), brilho especular no topo, borda de luz e sombra Depth3. Custo de
 * renderização constante, sem animação de blur, adequado a aparelhos intermediários.
 */
internal fun Modifier.fluxaGlass(shape: Shape, depth: FluxaDepth = FluxaDepth.Depth3): Modifier = composed {
    val colors = FluxaTheme.colors
    val sheenAlpha = if (colors.isDark) 0.06f else 0.55f
    this
        .fluxaDepth(depth, shape)
        .background(colors.surfaceGlass, shape)
        .background(
            brush = Brush.verticalGradient(
                0f to Color.White.copy(alpha = sheenAlpha),
                0.5f to Color.Transparent,
            ),
            shape = shape,
        )
}
