package com.fluxa.personal.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.ui.designsystem.components.FluxaMoneyFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FluxaDesignTokensTest {

    private fun contrast(fg: Color, bg: Color): Double {
        val a = fg.compositeOver(bg).luminance() + 0.05
        val b = bg.luminance() + 0.05
        return maxOf(a, b) / minOf(a, b)
    }

    private fun assertAA(theme: FluxaColors) {
        val backgrounds = mapOf(
            "background" to theme.background,
            "surface" to theme.surface,
            "surfaceSunken" to theme.surfaceSunken,
            "surfaceRaised" to theme.surfaceRaised,
            "surfaceGlass" to theme.surfaceGlass.compositeOver(theme.background),
        )
        val texts = mapOf(
            "textPrimary" to theme.textPrimary,
            "textSecondary" to theme.textSecondary,
            "textMuted" to theme.textMuted,
            "income" to theme.income,
            "expense" to theme.expense,
            "warning" to theme.warning,
            "success" to theme.success,
            "info" to theme.info,
        )
        val pairs = buildList {
            texts.forEach { (fn, fg) -> backgrounds.forEach { (bn, bg) -> add("$fn/$bn" to (fg to bg)) } }
            add("onSurfaceInverse/surfaceInverse" to (theme.onSurfaceInverse to theme.surfaceInverse))
            add("onSurfaceInverseMuted/surfaceInverse" to (theme.onSurfaceInverseMuted to theme.surfaceInverse))
            add("onBrand/brand" to (theme.onBrand to theme.brand))
            add("onAction/action" to (theme.onAction to theme.action))
            add("onSelected/selected" to (theme.onSelected to theme.selected))
            add("onBrandTonal/brandTonal" to (theme.onBrandTonal to theme.brandTonal))
            add("income/incomeContainer" to (theme.income to theme.incomeContainer))
            add("expense/expenseContainer" to (theme.expense to theme.expenseContainer))
            add("warning/warningContainer" to (theme.warning to theme.warningContainer))
            add("success/successContainer" to (theme.success to theme.successContainer))
        }
        val failures = pairs.mapNotNull { (name, colors) ->
            val ratio = contrast(colors.first, colors.second)
            if (ratio < 4.5) "$name=${"%.2f".format(ratio)}" else null
        }
        assertTrue("Pares abaixo de AA (dark=${theme.isDark}): $failures", failures.isEmpty())
    }

    @Test
    fun lightThemeMeetsAA() = assertAA(LightFluxaColors)

    @Test
    fun darkThemeMeetsAA() = assertAA(DarkFluxaColors)

    @Test
    fun typographyNeverGoesBelow12sp() {
        val t = DefaultFluxaTypography
        listOf(t.display, t.headline, t.title, t.titleSmall, t.body, t.bodyStrong, t.label, t.caption, t.overline, t.amountHero, t.amount)
            .forEach { assertTrue("${it.fontSize}", it.fontSize.value >= 12f) }
    }

    @Test
    fun formatsBrlFromCents() {
        fun normalize(s: String) = s.replace(' ', ' ')
        assertEquals("R$ 1.234,56", normalize(FluxaMoneyFormat.format(Money(123456))))
        assertEquals("+R$ 10,00", normalize(FluxaMoneyFormat.format(Money(1000), signed = true)))
        assertEquals("−R$ 0,05", normalize(FluxaMoneyFormat.format(Money(-5))))
        assertEquals("R$ 0,00", normalize(FluxaMoneyFormat.format(Money(0), signed = true)))
    }
}
