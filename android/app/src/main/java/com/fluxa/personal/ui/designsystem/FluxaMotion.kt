package com.fluxa.personal.ui.designsystem

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * Linguagem de movimento: fade, escala pequena, slide e spring discreto. Sem blur animado.
 * Respeita "Remover animações" do sistema (ANIMATOR_DURATION_SCALE = 0) via [LocalReducedMotion].
 */
internal object FluxaMotion {
    const val Press = 120
    const val Short = 200
    const val Medium = 300
    const val PressedScale = 0.96f
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Emphasized = CubicBezierEasing(0.3f, 0f, 0f, 1f)
}

internal val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
internal fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

@Composable
internal fun <T> fluxaTween(durationMillis: Int = FluxaMotion.Short): FiniteAnimationSpec<T> =
    if (LocalReducedMotion.current) snap() else tween(durationMillis, easing = FluxaMotion.Standard)

@Composable
internal fun <T> fluxaSpring(): FiniteAnimationSpec<T> =
    if (LocalReducedMotion.current) snap() else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow)

/** Feedback físico de toque: leve redução de escala enquanto pressionado. */
internal fun Modifier.fluxaPressScale(interactionSource: InteractionSource, enabled: Boolean = true): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) FluxaMotion.PressedScale else 1f,
        animationSpec = fluxaSpring(),
        label = "press-scale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
