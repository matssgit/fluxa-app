package com.fluxa.personal.ui.designsystem.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fluxa.personal.ui.designsystem.FluxaDepth
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.fluxaDepth
import com.fluxa.personal.ui.designsystem.fluxaGlass
import com.fluxa.personal.ui.designsystem.fluxaInnerShade
import com.fluxa.personal.ui.designsystem.fluxaPressScale

/** Tons de superfície. `Glass` só para elementos flutuantes; dados financeiros ficam em superfícies sólidas. */
internal enum class FluxaSurfaceTone { Default, Sunken, Raised, Inverse, Glass }

private fun FluxaSurfaceTone.defaultDepth() = when (this) {
    FluxaSurfaceTone.Default -> FluxaDepth.Depth1
    FluxaSurfaceTone.Sunken -> FluxaDepth.Depth0
    FluxaSurfaceTone.Raised -> FluxaDepth.Depth2
    FluxaSurfaceTone.Inverse -> FluxaDepth.Depth2
    FluxaSurfaceTone.Glass -> FluxaDepth.Depth3
}

/** Superfície base (equivalente a `Surface` do M3) com os tons e níveis de profundidade do Fluxa. */
@Composable
internal fun FluxaSurface(
    modifier: Modifier = Modifier,
    tone: FluxaSurfaceTone = FluxaSurfaceTone.Default,
    shape: Shape = FluxaTheme.shapes.large,
    depth: FluxaDepth = tone.defaultDepth(),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = FluxaTheme.colors
    val surfaceModifier = when (tone) {
        FluxaSurfaceTone.Glass -> Modifier.fluxaGlass(shape, depth)
        FluxaSurfaceTone.Sunken -> Modifier
            .fluxaDepth(depth, shape)
            .background(colors.surfaceSunken, shape)
            .fluxaInnerShade(shape)
        FluxaSurfaceTone.Inverse -> Modifier
            .fluxaDepth(depth, shape, bordered = false, highlight = Color.White.copy(alpha = 0.14f))
            .background(colors.surfaceInverse, shape)
        FluxaSurfaceTone.Raised -> Modifier.fluxaDepth(depth, shape).background(colors.surfaceRaised, shape)
        FluxaSurfaceTone.Default -> Modifier.fluxaDepth(depth, shape).background(colors.surface, shape)
    }
    val contentColor = if (tone == FluxaSurfaceTone.Inverse) colors.onSurfaceInverse else colors.textPrimary
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(modifier = modifier.then(surfaceModifier).clip(shape), content = content)
    }
}

/**
 * Card de conteúdo. `featured = true` aplica a forma folha e Depth2: reservar para destaques
 * (saldo, cartão em foco, meta em foco). Clicável quando [onClick] é informado, com feedback de pressão.
 */
@Composable
internal fun FluxaCard(
    modifier: Modifier = Modifier,
    tone: FluxaSurfaceTone = FluxaSurfaceTone.Default,
    featured: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    /** Formas orgânicas discretas nos destaques escuros, a assinatura do Fluxa na web. */
    organic: Boolean = featured && tone == FluxaSurfaceTone.Inverse,
    content: @Composable ColumnScope.() -> Unit,
) {
    require(tone != FluxaSurfaceTone.Glass) { "Cards de conteúdo não usam glass." }
    val shapes = FluxaTheme.shapes
    val interaction = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null) {
        Modifier
            .fluxaPressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = onClickLabel,
                onClick = onClick,
            )
    } else {
        Modifier
    }
    FluxaSurface(
        modifier = modifier,
        tone = tone,
        shape = if (featured) shapes.leaf else shapes.large,
        depth = if (featured) FluxaDepth.Depth2 else tone.defaultDepth(),
    ) {
        Column(modifier = clickModifier.then(if (organic) Modifier.fluxaOrganic() else Modifier).padding(contentPadding), content = content)
    }
}

/** Anel e curva em creme quase transparente, fora da área de texto. Só desenho: sem custo de composição. */
private fun Modifier.fluxaOrganic(): Modifier = drawBehind {
    val r = size.width * 0.27f
    drawCircle(Color.White.copy(alpha = 0.05f), radius = r, center = Offset(size.width - r * 0.45f, r * 0.15f), style = Stroke(width = r * 0.30f))
    drawCircle(Color.White.copy(alpha = 0.035f), radius = size.width * 0.42f, center = Offset(size.width * 0.80f, size.height + size.width * 0.20f))
}
