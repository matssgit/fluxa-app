package com.fluxa.personal.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fluxa.personal.R
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme

/**
 * Padrão único de sheet (substitui os 9 overlays diferentes da web).
 * Topo arredondado, alça discreta, título + fechar, conteúdo e rodapé de ações opcional.
 * O contêiner é opaco (surfaceRaised) com o brilho e a borda de luz do glass: a 86% de opacidade o conteúdo
 * por trás vazava sob o texto do sheet (verificado no emulador), então a translucidez fica só na navegação e chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FluxaBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    closeDescription: String = stringResource(R.string.action_close),
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FluxaTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = FluxaTheme.shapes.sheet,
        containerColor = colors.surfaceRaised,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = colors.scrim,
        dragHandle = { FluxaSheetHandle() },
    ) {
        FluxaSheetLayout(
            title = title,
            onClose = onDismissRequest,
            closeDescription = closeDescription,
            footer = footer,
            content = content,
        )
    }
}

@Composable
internal fun FluxaSheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .size(width = 40.dp, height = 4.dp)
            .background(FluxaTheme.colors.borderStrong, FluxaTheme.shapes.pill),
    )
}

/** Conteúdo padronizado do sheet. Público para uso em previews e em sheets não modais futuros. */
@Composable
internal fun FluxaSheetLayout(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeDescription: String = stringResource(R.string.action_close),
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FluxaTheme.colors
    val sheen = if (colors.isDark) 0.05f else 0.5f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = sheen), 0.3f to Color.Transparent))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = FluxaTheme.typography.title,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            FluxaIconButton(icon = FluxaIcons.Close, contentDescription = closeDescription, onClick = onClose)
        }
        Column(modifier = Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        if (footer != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = footer,
            )
        }
    }
}
