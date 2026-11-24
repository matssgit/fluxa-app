package com.fluxa.personal.ui.designsystem.catalog

import com.fluxa.personal.core.money.Money
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fluxa.personal.core.preferences.ThemePreference
import com.fluxa.personal.ui.designsystem.FluxaDepth
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaAmount
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonSize
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaChip
import com.fluxa.personal.ui.designsystem.components.FluxaIconButton
import com.fluxa.personal.ui.designsystem.components.FluxaIconButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaInput
import com.fluxa.personal.ui.designsystem.components.FluxaNavAction
import com.fluxa.personal.ui.designsystem.components.FluxaNavItem
import com.fluxa.personal.ui.designsystem.components.FluxaNavigationBar
import com.fluxa.personal.ui.designsystem.components.FluxaSectionHeader
import com.fluxa.personal.ui.designsystem.components.FluxaSheetHandle
import com.fluxa.personal.ui.designsystem.components.FluxaSheetLayout
import com.fluxa.personal.ui.designsystem.components.FluxaSurface
import com.fluxa.personal.ui.designsystem.components.FluxaSurfaceTone
import com.fluxa.personal.ui.designsystem.components.FluxaTopBar

/*
 * Catálogo visual inicial do design system. Cada grupo tem preview em light e dark.
 * Abra este arquivo no Android Studio (Split/Design) para navegar pelo catálogo.
 */

private const val W = 390

@Composable
private fun Catalog(dark: Boolean, content: @Composable ColumnScope.() -> Unit) {
    FluxaTheme(themePreference = if (dark) ThemePreference.DARK else ThemePreference.LIGHT, reducedMotion = true) {
        Column(
            modifier = Modifier
                .background(FluxaTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

// region Cores

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorsCatalog() {
    val c = FluxaTheme.colors
    val swatches = listOf(
        "background" to c.background, "surface" to c.surface, "surfaceSunken" to c.surfaceSunken,
        "surfaceRaised" to c.surfaceRaised, "surfaceInverse" to c.surfaceInverse, "brand" to c.brand,
        "brandTonal" to c.brandTonal, "accent" to c.accent, "textPrimary" to c.textPrimary,
        "textSecondary" to c.textSecondary, "textMuted" to c.textMuted, "income" to c.income,
        "expense" to c.expense, "warning" to c.warning, "success" to c.success, "info" to c.info,
    )
    FluxaSectionHeader(title = "Cores", overline = if (c.isDark) "Dark" else "Light")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        swatches.forEach { (name, color) ->
            Column(Modifier.size(width = 108.dp, height = 76.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .size(height = 44.dp, width = 108.dp)
                        .background(color, FluxaTheme.shapes.small)
                        .border(1.dp, c.border, FluxaTheme.shapes.small),
                )
                Text(name, style = FluxaTheme.typography.caption, color = c.textSecondary)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SemanticPill("Entrada", c.income, c.incomeContainer)
        SemanticPill("Saída", c.expense, c.expenseContainer)
        SemanticPill("Pendente", c.warning, c.warningContainer)
        SemanticPill("Pago", c.success, c.successContainer)
    }
}

@Composable
private fun SemanticPill(label: String, fg: Color, bg: Color) {
    Text(
        label,
        style = FluxaTheme.typography.caption,
        color = fg,
        modifier = Modifier.background(bg, FluxaTheme.shapes.pill).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Preview(name = "Cores · light", widthDp = W)
@Composable
private fun ColorsLight() = Catalog(dark = false) { ColorsCatalog() }

@Preview(name = "Cores · dark", widthDp = W)
@Composable
private fun ColorsDark() = Catalog(dark = true) { ColorsCatalog() }

// endregion

// region Tipografia

@Composable
private fun TypographyCatalog() {
    val t = FluxaTheme.typography
    val c = FluxaTheme.colors
    FluxaSectionHeader(title = "Tipografia")
    Text("SALDO DISPONÍVEL", style = t.overline, color = c.textMuted)
    FluxaAmount(amount = Money(12_345_67), style = t.amountHero, color = c.textPrimary)
    Text("Display 40", style = t.display, color = c.textPrimary)
    Text("Headline 28", style = t.headline, color = c.textPrimary)
    Text("Title 20", style = t.title, color = c.textPrimary)
    Text("Title small 16", style = t.titleSmall, color = c.textPrimary)
    Text("Body 16 — texto corrido do aplicativo.", style = t.body, color = c.textPrimary)
    Text("Body strong 16", style = t.bodyStrong, color = c.textPrimary)
    Text("Label 14", style = t.label, color = c.textSecondary)
    Text("Caption 12 — mínimo do sistema", style = t.caption, color = c.textMuted)
}

@Preview(name = "Tipografia · light", widthDp = W)
@Composable
private fun TypographyLight() = Catalog(dark = false) { TypographyCatalog() }

@Preview(name = "Tipografia · dark", widthDp = W)
@Composable
private fun TypographyDark() = Catalog(dark = true) { TypographyCatalog() }

@Preview(name = "Tipografia · fonte 1.5x", widthDp = W, fontScale = 1.5f)
@Composable
private fun TypographyLargeFont() = Catalog(dark = false) { TypographyCatalog() }

// endregion

// region Botões

@Composable
private fun ButtonsCatalog() {
    FluxaSectionHeader(title = "Botões")
    FluxaButton("Salvar lançamento", onClick = {}, size = FluxaButtonSize.Large, modifier = Modifier.fillMaxWidth())
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FluxaButton("Primário", onClick = {})
        FluxaButton("Secundário", onClick = {}, variant = FluxaButtonVariant.Secondary)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FluxaButton("Glass", onClick = {}, variant = FluxaButtonVariant.Glass, leadingIcon = FluxaIcons.Plus)
        FluxaButton("Excluir", onClick = {}, variant = FluxaButtonVariant.Destructive)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FluxaButton("Desabilitado", onClick = {}, enabled = false)
        FluxaButton("Salvando", onClick = {}, loading = true)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FluxaIconButton(FluxaIcons.Eye, "Mostrar valores", onClick = {})
        FluxaIconButton(FluxaIcons.Search, "Buscar", onClick = {}, variant = FluxaIconButtonVariant.Tonal)
        FluxaIconButton(FluxaIcons.Plus, "Adicionar", onClick = {}, variant = FluxaIconButtonVariant.Glass)
    }
}

@Preview(name = "Botões · light", widthDp = W)
@Composable
private fun ButtonsLight() = Catalog(dark = false) { ButtonsCatalog() }

@Preview(name = "Botões · dark", widthDp = W)
@Composable
private fun ButtonsDark() = Catalog(dark = true) { ButtonsCatalog() }

// endregion

// region Cards, superfícies e profundidade

@Composable
private fun CardsCatalog(hidden: Boolean = false) {
    val c = FluxaTheme.colors
    val t = FluxaTheme.typography
    FluxaSectionHeader(title = "Cards", actionLabel = "Ver tudo", onAction = {})
    FluxaCard(tone = FluxaSurfaceTone.Inverse, featured = true) {
        Text("SALDO DISPONÍVEL", style = t.overline, color = c.onSurfaceInverseMuted)
        FluxaAmount(amount = Money(8_420_90), style = t.amountHero, hidden = hidden)
        Text("Forma folha: só em destaques", style = t.caption, color = c.onSurfaceInverseMuted)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FluxaCard(modifier = Modifier.weight(1f), onClick = {}) {
            Text("Entradas", style = t.caption, color = c.textSecondary)
            FluxaAmount(amount = Money(6_000_00), color = c.income, signed = true, hidden = hidden)
        }
        FluxaCard(modifier = Modifier.weight(1f), onClick = {}) {
            Text("Saídas", style = t.caption, color = c.textSecondary)
            FluxaAmount(amount = Money(-2_310_45), color = c.expense, hidden = hidden)
        }
    }
    FluxaSectionHeader(title = "Profundidade")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FluxaDepth.entries.forEach { depth ->
            FluxaSurface(
                modifier = Modifier.size(72.dp),
                tone = if (depth == FluxaDepth.Depth0) FluxaSurfaceTone.Sunken else FluxaSurfaceTone.Default,
                depth = depth,
            ) {
                Text(depth.name.removePrefix("Depth"), style = t.label, color = c.textSecondary, modifier = Modifier.padding(12.dp))
            }
        }
    }
    FluxaSurface(modifier = Modifier.fillMaxWidth().height(64.dp), tone = FluxaSurfaceTone.Glass, shape = FluxaTheme.shapes.pill) {
        Text("Glass: só em elementos flutuantes", style = t.label, color = c.textPrimary, modifier = Modifier.padding(20.dp))
    }
}

@Preview(name = "Cards · light", widthDp = W)
@Composable
private fun CardsLight() = Catalog(dark = false) { CardsCatalog() }

@Preview(name = "Cards · dark", widthDp = W)
@Composable
private fun CardsDark() = Catalog(dark = true) { CardsCatalog() }

@Preview(name = "Cards · valores ocultos", widthDp = W)
@Composable
private fun CardsHidden() = Catalog(dark = false) { CardsCatalog(hidden = true) }

// endregion

// region Inputs e chips

@Composable
private fun InputsCatalog() {
    FluxaSectionHeader(title = "Inputs")
    FluxaInput(value = "", onValueChange = {}, label = "Descrição", placeholder = "Ex.: Mercado do bairro", supportingText = "Aparece no Caixa")
    FluxaInput(value = "Supermercado", onValueChange = {}, label = "Buscar", leadingIcon = FluxaIcons.Search)
    FluxaInput(value = "abc", onValueChange = {}, label = "Valor", errorText = "Informe um valor maior que zero")
    FluxaInput(value = "Bloqueado", onValueChange = {}, label = "Conta", enabled = false)
    FluxaSectionHeader(title = "Chips")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FluxaChip("Tudo", selected = true, onSelectedChange = {})
        FluxaChip("Entradas", selected = false, onSelectedChange = {})
        FluxaChip("Pendentes", selected = false, onSelectedChange = {})
    }
}

@Preview(name = "Inputs e chips · light", widthDp = W)
@Composable
private fun InputsLight() = Catalog(dark = false) { InputsCatalog() }

@Preview(name = "Inputs e chips · dark", widthDp = W)
@Composable
private fun InputsDark() = Catalog(dark = true) { InputsCatalog() }

// endregion

// region Navegação, top bar e sheet

@Composable
private fun NavigationCatalog() {
    val items = listOf(
        FluxaNavItem("dashboard", "Início", FluxaIcons.Home, FluxaIcons.HomeFilled),
        FluxaNavItem("cash", "Caixa", FluxaIcons.Cash, FluxaIcons.CashFilled),
        FluxaNavItem("cards", "Cartões", FluxaIcons.Card, FluxaIcons.CardFilled),
        FluxaNavItem("more", "Mais", FluxaIcons.More, FluxaIcons.MoreFilled),
    )
    FluxaTopBar(title = "Início", overline = "Novembro") {
        FluxaIconButton(FluxaIcons.Eye, "Ocultar valores", onClick = {})
    }
    FluxaNavigationBar(
        items = items,
        selectedRoute = "dashboard",
        onItemSelected = {},
        primaryAction = FluxaNavAction("Novo lançamento", onClick = {}),
    )
    FluxaNavigationBar(
        items = items,
        selectedRoute = "cards",
        onItemSelected = {},
        primaryAction = FluxaNavAction("Novo lançamento", onClick = {}),
    )
    FluxaSurface(tone = FluxaSurfaceTone.Raised, shape = FluxaTheme.shapes.sheet) {
        Column {
            Box(Modifier.fillMaxWidth().padding(top = 0.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { FluxaSheetHandle() }
            FluxaSheetLayout(
                title = "Novo lançamento",
                onClose = {},
                footer = { FluxaButton("Salvar", onClick = {}, modifier = Modifier.weight(1f)) },
            ) {
                FluxaInput(value = "", onValueChange = {}, label = "Descrição", placeholder = "Ex.: Farmácia")
            }
        }
    }
}

@Preview(name = "Navegação e sheet · light", widthDp = W)
@Composable
private fun NavigationLight() = Catalog(dark = false) { NavigationCatalog() }

@Preview(name = "Navegação e sheet · dark", widthDp = W)
@Composable
private fun NavigationDark() = Catalog(dark = true) { NavigationCatalog() }

@Preview(name = "Navegação · 360dp fonte 1.3x", widthDp = 360, fontScale = 1.3f)
@Composable
private fun NavigationSmall() = Catalog(dark = false) { NavigationCatalog() }

// endregion
