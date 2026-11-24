package com.fluxa.personal.ui.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Paleta de marca do Fluxa. Só os tokens semânticos de [FluxaColors] devem ser usados em componentes.
 */
internal object FluxaPalette {
    val Pine950 = Color(0xFF102F29)
    val Pine900 = Color(0xFF173F36)
    val Pine700 = Color(0xFF35695A)
    val Sage500 = Color(0xFF829B88)
    val Sage200 = Color(0xFFDFE8DE)
    val Cream50 = Color(0xFFFFFDF7)
    val Sand100 = Color(0xFFF3F0E7)
    val Terracotta500 = Color(0xFFB9684F)
}

/**
 * Tokens semânticos. Todos os pares texto/fundo usados pelos componentes passam WCAG AA (≥ 4.5:1),
 * verificados para light e dark. `accent` é decorativo: não usar para texto.
 */
@Immutable
internal data class FluxaColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val surfaceRaised: Color,
    val surfaceGlass: Color,
    val surfaceInverse: Color,
    val onSurfaceInverse: Color,
    val onSurfaceInverseMuted: Color,
    val border: Color,
    val borderStrong: Color,
    val highlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val brand: Color,
    val onBrand: Color,
    val brandTonal: Color,
    val onBrandTonal: Color,
    val accent: Color,
    val income: Color,
    val incomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val success: Color,
    val successContainer: Color,
    val info: Color,
    val scrim: Color,
    val shadow: Color,
)

internal val LightFluxaColors = FluxaColors(
    isDark = false,
    background = FluxaPalette.Sand100,
    surface = Color(0xFFFFFDF8),
    surfaceSunken = Color(0xFFECEFE8),
    surfaceRaised = Color(0xFFFDFAF2),
    surfaceGlass = Color(0xFFFFFDF8).copy(alpha = 0.95f),
    surfaceInverse = FluxaPalette.Pine900,
    onSurfaceInverse = FluxaPalette.Cream50,
    onSurfaceInverseMuted = Color(0xFFC9D6CC),
    border = FluxaPalette.Pine900.copy(alpha = 0.10f),
    borderStrong = FluxaPalette.Pine900.copy(alpha = 0.22f),
    highlight = Color.White.copy(alpha = 0.75f),
    textPrimary = Color(0xFF18312B),
    textSecondary = Color(0xFF4E5E58),
    textMuted = Color(0xFF5E6B66),
    brand = FluxaPalette.Pine900,
    onBrand = FluxaPalette.Cream50,
    brandTonal = FluxaPalette.Sage200,
    onBrandTonal = FluxaPalette.Pine900,
    accent = FluxaPalette.Terracotta500,
    income = Color(0xFF3F7259),
    incomeContainer = Color(0xFFE4EEE6),
    expense = Color(0xFF984F39),
    expenseContainer = Color(0xFFF3E4DE),
    warning = Color(0xFF80602A),
    warningContainer = Color(0xFFF4EAD8),
    success = Color(0xFF2F6E52),
    successContainer = Color(0xFFDCEBE1),
    info = Color(0xFF456F6B),
    scrim = FluxaPalette.Pine950.copy(alpha = 0.40f),
    shadow = FluxaPalette.Pine950,
)

internal val DarkFluxaColors = FluxaColors(
    isDark = true,
    background = Color(0xFF0B1E1A),
    surface = Color(0xFF122A24),
    surfaceSunken = Color(0xFF0E231E),
    surfaceRaised = Color(0xFF18342C),
    surfaceGlass = Color(0xFF18342C).copy(alpha = 0.96f),
    surfaceInverse = Color(0xFF24574A),
    onSurfaceInverse = Color(0xFFF4F0E6),
    onSurfaceInverseMuted = Color(0xFFC9D6CC),
    border = Color(0xFFEBEFE4).copy(alpha = 0.10f),
    borderStrong = Color(0xFFEBEFE4).copy(alpha = 0.22f),
    highlight = Color.White.copy(alpha = 0.07f),
    textPrimary = Color(0xFFF4F0E6),
    textSecondary = Color(0xFFC2CDC7),
    textMuted = Color(0xFF9DADA6),
    brand = Color(0xFF8DB7A1),
    onBrand = Color(0xFF0B1E1A),
    brandTonal = Color(0xFF23463A),
    onBrandTonal = Color(0xFFD7E7DC),
    accent = Color(0xFFD18A70),
    income = Color(0xFF86B59A),
    incomeContainer = Color(0xFF1F3F33),
    expense = Color(0xFFE0937A),
    expenseContainer = Color(0xFF432B24),
    warning = Color(0xFFD9AE6A),
    warningContainer = Color(0xFF3D3220),
    success = Color(0xFF7FC09C),
    successContainer = Color(0xFF1E3D30),
    info = Color(0xFF8FBDB8),
    scrim = Color.Black.copy(alpha = 0.55f),
    shadow = Color.Black,
)

/** Mapeia os tokens Fluxa para o ColorScheme do Material 3, para que componentes M3 herdem a marca. */
internal fun FluxaColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = brand,
        onPrimary = onBrand,
        primaryContainer = brandTonal,
        onPrimaryContainer = onBrandTonal,
        secondary = textSecondary,
        onSecondary = surface,
        secondaryContainer = brandTonal,
        onSecondaryContainer = onBrandTonal,
        tertiary = expense,
        onTertiary = surface,
        tertiaryContainer = expenseContainer,
        onTertiaryContainer = expense,
        background = background,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = textSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = surfaceInverse,
        inverseOnSurface = onSurfaceInverse,
        inversePrimary = brandTonal,
        error = expense,
        onError = surface,
        errorContainer = expenseContainer,
        onErrorContainer = expense,
        outline = borderStrong,
        outlineVariant = border,
        scrim = scrim,
        surfaceBright = surfaceRaised,
        surfaceDim = background,
        surfaceContainerLowest = surfaceRaised,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceSunken,
    )
}
