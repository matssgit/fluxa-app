package com.fluxa.personal.ui.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.fluxa.personal.core.preferences.ThemePreference

internal val LocalFluxaColors = staticCompositionLocalOf { LightFluxaColors }
internal val LocalFluxaTypography = staticCompositionLocalOf { DefaultFluxaTypography }
internal val LocalFluxaShapes = staticCompositionLocalOf { FluxaShapes() }

/** Quando verdadeiro, todo [FluxaAmount] renderiza a máscara em vez do valor. */
internal val LocalAmountsHidden = staticCompositionLocalOf { false }

/**
 * Tema do Fluxa. A preferência explícita do usuário (LIGHT/DARK) sempre vence o sistema;
 * só SYSTEM acompanha `isSystemInDarkTheme()`.
 */
@Composable
internal fun FluxaTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    amountsHidden: Boolean = false,
    reducedMotion: Boolean = rememberSystemReducedMotion(),
    content: @Composable () -> Unit,
) {
    val dark = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    val colors = if (dark) DarkFluxaColors else LightFluxaColors
    val colorScheme = remember(colors) { colors.toMaterialColorScheme() }
    val materialTypography = remember { DefaultFluxaTypography.toMaterialTypography() }

    // Ícones das barras do sistema seguem o tema escolhido no app, não o tema do sistema.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalFluxaColors provides colors,
        LocalFluxaTypography provides DefaultFluxaTypography,
        LocalFluxaShapes provides FluxaShapes(),
        LocalAmountsHidden provides amountsHidden,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = materialTypography,
            shapes = MaterialFluxaShapes,
            content = content,
        )
    }
}

internal object FluxaTheme {
    val colors: FluxaColors
        @Composable @ReadOnlyComposable get() = LocalFluxaColors.current
    val typography: FluxaTypography
        @Composable @ReadOnlyComposable get() = LocalFluxaTypography.current
    val shapes: FluxaShapes
        @Composable @ReadOnlyComposable get() = LocalFluxaShapes.current
}
