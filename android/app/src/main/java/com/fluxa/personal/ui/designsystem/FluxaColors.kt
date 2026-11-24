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
    val Petrol900 = Color(0xFF14414D)
    val Sage500 = Color(0xFF829B88)
    val Cream50 = Color(0xFFFFFDF7)
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
    background = Color(0xFFF6F2EA),
    surface = Color(0xFFFFFDF9),
    surfaceSunken = Color(0xFFF0EBE1),
    surfaceRaised = Color(0xFFFDFAF3),
    surfaceGlass = Color(0xFFFFFDF9).copy(alpha = 0.96f),
    surfaceInverse = Color(0xFF123A45),
    onSurfaceInverse = Color(0xFFFFFDF7),
    onSurfaceInverseMuted = Color(0xFFC9D5D8),
    border = Color(0xFF3A2F24).copy(alpha = 0.10f),
    borderStrong = Color(0xFF3A2F24).copy(alpha = 0.24f),
    highlight = Color.White.copy(alpha = 0.75f),
    textPrimary = Color(0xFF1E2A2E),
    textSecondary = Color(0xFF505A5C),
    textMuted = Color(0xFF5F6667),
    brand = Color(0xFF14414D),
    onBrand = Color(0xFFFFFDF7),
    brandTonal = Color(0xFFE6EAE2),
    onBrandTonal = Color(0xFF14414D),
    accent = Color(0xFFB9684F),
    income = Color(0xFF2C6270),
    incomeContainer = Color(0xFFE1ECEC),
    expense = Color(0xFF964831),
    expenseContainer = Color(0xFFF5E5DD),
    warning = Color(0xFF765621),
    warningContainer = Color(0xFFF4EAD8),
    success = Color(0xFF3B6650),
    successContainer = Color(0xFFE3EBE3),
    info = Color(0xFF2F6470),
    scrim = Color(0xFF0C2229).copy(alpha = 0.40f),
    shadow = Color(0xFF2A2118),
)

internal val DarkFluxaColors = FluxaColors(
    isDark = true,
    background = Color(0xFF0E171B),
    surface = Color(0xFF162227),
    surfaceSunken = Color(0xFF111B1F),
    surfaceRaised = Color(0xFF1D2B31),
    surfaceGlass = Color(0xFF1D2B31).copy(alpha = 0.96f),
    surfaceInverse = Color(0xFF1B4A57),
    onSurfaceInverse = Color(0xFFF5F0E6),
    onSurfaceInverseMuted = Color(0xFFC6D3D6),
    border = Color(0xFFF0E9DC).copy(alpha = 0.10f),
    borderStrong = Color(0xFFF0E9DC).copy(alpha = 0.22f),
    highlight = Color.White.copy(alpha = 0.07f),
    textPrimary = Color(0xFFF5F0E6),
    textSecondary = Color(0xFFC7CBCB),
    textMuted = Color(0xFFA0A8A9),
    brand = Color(0xFF8FBBC6),
    onBrand = Color(0xFF0E171B),
    brandTonal = Color(0xFF23393F),
    onBrandTonal = Color(0xFFD9E6E8),
    accent = Color(0xFFD18A70),
    income = Color(0xFF8CBFC9),
    incomeContainer = Color(0xFF1C373E),
    expense = Color(0xFFE29A80),
    expenseContainer = Color(0xFF442C24),
    warning = Color(0xFFD9AE6A),
    warningContainer = Color(0xFF3D3220),
    success = Color(0xFF9DBBA5),
    successContainer = Color(0xFF26372E),
    info = Color(0xFF8FBBC6),
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
