package com.fluxa.personal.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fluxa.personal.R
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaIconButton
import com.fluxa.personal.ui.designsystem.components.FluxaNavigationBarDefaults
import com.fluxa.personal.ui.designsystem.fluxaGlass
import com.fluxa.personal.ui.designsystem.fluxaTween
import kotlinx.coroutines.delay

/**
 * Confirmação curta após salvar/cancelar (controle flutuante em glass, padrão do DS). Anunciada pelo TalkBack,
 * some sozinha após alguns segundos e pode ser fechada.
 */
@Composable
internal fun FeedbackBanner(message: String?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(message) }
    if (message != null) shown = message
    LaunchedEffect(message) {
        if (message != null) {
            delay(4_000)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(fluxaTween()) + slideInVertically(fluxaTween()) { it / 2 },
        exit = fadeOut(fluxaTween()) + slideOutVertically(fluxaTween()) { it / 2 },
        // Embaixo, acima da barra flutuante e do botão Salvar: não cobre título nem o botão Voltar.
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = FluxaNavigationBarDefaults.ContentClearance - 16.dp),
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .fluxaGlass(FluxaTheme.shapes.pill)
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                .testTag("feedback-banner"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(FluxaIcons.Check, contentDescription = null, tint = FluxaTheme.colors.success, modifier = Modifier.size(20.dp))
            Text(shown.orEmpty(), style = FluxaTheme.typography.label, color = FluxaTheme.colors.textPrimary, modifier = Modifier.weight(1f))
            FluxaIconButton(FluxaIcons.Close, stringResource(R.string.action_dismiss), onClick = onDismiss)
        }
    }
}
