package com.fluxa.personal.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalConfiguration
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
 * O contêiner é opaco (surfaceRaised, creme): translúcido, o conteúdo por trás vazava sob o texto (verificado no emulador),
 * e o brilho vertical criava uma faixa visível abaixo da alça no tema escuro.
 * Sheet e scrim ocupam a janela inteira (sem faixa clara nas barras do sistema); a barra de navegação é descontada no conteúdo.
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
    // Altura útil (sem as barras do sistema) + barra de navegação, que o próprio conteúdo desconta: nunca passa sob o status.
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp +
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.heightIn(max = maxHeight),
        windowInsets = WindowInsets(0),
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
    Column(
        modifier = modifier
            .fillMaxWidth()
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
