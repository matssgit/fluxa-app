package com.fluxa.personal.ui.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Escala tipográfica fechada. Fonte do sistema (Roboto/Google Sans conforme o aparelho).
 * Tudo em `sp`, portanto respeita o tamanho de fonte do usuário. Mínimo: 12sp.
 */
@Immutable
internal data class FluxaTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    /** Eyebrow/overline: 12sp, espaçado. O texto deve ser passado em maiúsculas pelo chamador. */
    val overline: TextStyle,
    /** Valor financeiro de destaque (saldo). Algarismos tabulares. */
    val amountHero: TextStyle,
    /** Valor financeiro em cards e linhas. Algarismos tabulares. */
    val amount: TextStyle,
)

private val Family = FontFamily.Default
private const val TABULAR = "tnum, lnum"

internal val DefaultFluxaTypography = FluxaTypography(
    display = TextStyle(fontFamily = Family, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.03).em),
    headline = TextStyle(fontFamily = Family, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em),
    title = TextStyle(fontFamily = Family, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
    titleSmall = TextStyle(fontFamily = Family, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    body = TextStyle(fontFamily = Family, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyStrong = TextStyle(fontFamily = Family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    label = TextStyle(fontFamily = Family, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.01.em),
    caption = TextStyle(fontFamily = Family, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.em),
    overline = TextStyle(fontFamily = Family, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.08.em),
    amountHero = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 46.sp,
        letterSpacing = (-0.035).em, fontFeatureSettings = TABULAR,
    ),
    amount = TextStyle(
        fontFamily = Family, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp,
        fontFeatureSettings = TABULAR,
    ),
)

/** Mapeamento para Material 3: nenhum estilo abaixo de 12sp. */
internal fun FluxaTypography.toMaterialTypography(): Typography = Typography(
    displayLarge = display.copy(fontSize = 48.sp, lineHeight = 54.sp),
    displayMedium = display,
    displaySmall = display.copy(fontSize = 34.sp, lineHeight = 40.sp),
    headlineLarge = headline.copy(fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = headline,
    headlineSmall = headline.copy(fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = title,
    titleMedium = titleSmall,
    titleSmall = label.copy(fontWeight = FontWeight.Bold),
    bodyLarge = body,
    bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = caption,
    labelLarge = label,
    labelMedium = caption.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = caption,
)
