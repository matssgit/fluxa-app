package com.fluxa.personal.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fluxa.personal.R
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaIconButton
import com.fluxa.personal.ui.designsystem.components.FluxaTopBar

/** Largura máxima de leitura em telas grandes; no celular ocupa a tela toda. */
private val ContentMaxWidth = 640.dp

/**
 * Tela secundária (fora das abas): top bar com voltar, conteúdo rolável e uma área de ação fixa
 * no rodapé, acima da barra de gestos e do teclado. A navegação flutuante fica oculta nessas rotas,
 * então ela nunca cobre o botão de salvar.
 */
@Composable
internal fun FeatureScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    overline: String? = null,
    bottomAction: (@Composable ColumnScope.() -> Unit)? = null,
    /** Falso enquanto uma operação não pode ser interrompida (ex.: restauração em andamento). */
    backEnabled: Boolean = true,
    content: LazyListScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(FluxaTheme.colors.background)
            .imePadding(),
    ) {
        FluxaTopBar(
            title = title,
            overline = overline,
            navigation = {
                FluxaIconButton(
                    icon = FluxaIcons.Back,
                    contentDescription = stringResource(R.string.action_back),
                    onClick = onBack,
                    enabled = backEnabled,
                    modifier = Modifier.testTag("action-back"),
                )
            },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxSize().testTag("feature-list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
        if (bottomAction != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FluxaTheme.colors.background)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.widthIn(max = ContentMaxWidth), verticalArrangement = Arrangement.spacedBy(8.dp), content = bottomAction)
            }
        }
    }
}

@Composable
internal fun LoadingBlock(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.loading)
    Box(
        modifier = modifier.fillMaxWidth().heightIn(min = 160.dp).semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = FluxaTheme.colors.brand, modifier = Modifier.size(32.dp))
        Text(label, style = FluxaTheme.typography.caption, color = FluxaTheme.colors.textMuted, modifier = Modifier.padding(top = 72.dp))
    }
}

/** Mensagem de erro anunciada pelo TalkBack, com ícone e texto (não depende só de cor). */
@Composable
internal fun ErrorBanner(message: String, onDismiss: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = FluxaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.expenseContainer, FluxaTheme.shapes.medium)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive }
            .testTag("error-banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(FluxaIcons.Alert, contentDescription = null, tint = colors.expense, modifier = Modifier.size(20.dp))
        Text(message, style = FluxaTheme.typography.label, color = colors.expense, modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp))
        if (onDismiss != null) {
            FluxaIconButton(FluxaIcons.Close, stringResource(R.string.action_dismiss), onClick = onDismiss, tint = colors.expense)
        }
    }
}

@Composable
internal fun EmptyStateCard(title: String, body: String, icon: ImageVector, modifier: Modifier = Modifier) {
    FluxaCard(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.size(48.dp).background(FluxaTheme.colors.brandTonal, FluxaTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = FluxaTheme.colors.onBrandTonal, modifier = Modifier.size(24.dp))
        }
        Text(title, style = FluxaTheme.typography.titleSmall, color = FluxaTheme.colors.textPrimary, modifier = Modifier.padding(top = 16.dp))
        Text(body, style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Pílula de estado com ícone + texto. [spoken] substitui o texto no TalkBack ("Situação: Paga"), sem anunciar duas vezes. */
@Composable
internal fun StatePill(text: String, icon: ImageVector?, modifier: Modifier = Modifier, spoken: String? = null) {
    val colors = FluxaTheme.colors
    Row(
        modifier = modifier
            .background(colors.surfaceSunken, FluxaTheme.shapes.pill)
            .border(1.dp, colors.border, FluxaTheme.shapes.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
        Text(
            text, style = FluxaTheme.typography.caption, color = colors.textSecondary,
            modifier = if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier,
        )
    }
}

/** Linha com rótulo, texto de apoio e Switch do M3 tematizado; a linha inteira é o alvo de toque. */
@Composable
internal fun SwitchRow(
    label: String,
    supporting: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = FluxaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = FluxaTheme.typography.label, color = colors.textPrimary)
            if (supporting != null) Text(supporting, style = FluxaTheme.typography.caption, color = colors.textMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.brand,
                checkedThumbColor = colors.onBrand,
                uncheckedTrackColor = colors.surfaceSunken,
                uncheckedBorderColor = colors.borderStrong,
                uncheckedThumbColor = colors.textMuted,
            ),
        )
    }
}

/** Confirmação usando o padrão único de sheet; [destructive] falso para ações reversíveis (pausar, retomar, concluir). */
@Composable
internal fun ConfirmArchiveSheet(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String = stringResource(R.string.action_cancel),
    confirmIcon: ImageVector = FluxaIcons.Archive,
    destructive: Boolean = true,
    /**
     * Botões empilhados em largura total (padrão). Lado a lado, rótulos como "Manter compra"/"Cancelar compra"
     * ficavam ilegíveis em 320dp com fonte 1,3 (verificado no emulador).
     */
    stacked: Boolean = true,
) {
    val confirmVariant = if (destructive) FluxaButtonVariant.Destructive else FluxaButtonVariant.Primary
    com.fluxa.personal.ui.designsystem.components.FluxaBottomSheet(
        title = title,
        onDismissRequest = onDismiss,
        footer = if (stacked) null else {
            {
                FluxaButton(
                    text = dismissLabel,
                    onClick = onDismiss,
                    variant = FluxaButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                FluxaButton(
                    text = confirmLabel,
                    onClick = onConfirm,
                    variant = confirmVariant,
                    leadingIcon = confirmIcon,
                    modifier = Modifier.weight(1f).testTag("action-confirm-archive"),
                )
            }
        },
    ) {
        Text(body, style = FluxaTheme.typography.body, color = FluxaTheme.colors.textSecondary)
        if (stacked) {
            FluxaButton(
                text = confirmLabel,
                onClick = onConfirm,
                variant = confirmVariant,
                size = com.fluxa.personal.ui.designsystem.components.FluxaButtonSize.Large,
                leadingIcon = confirmIcon,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).testTag("action-confirm-archive"),
            )
            FluxaButton(
                text = dismissLabel,
                onClick = onDismiss,
                variant = FluxaButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Rótulo de grupo de seleção (tipo, ícone, cor). */
@Composable
internal fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = FluxaTheme.typography.label, color = FluxaTheme.colors.textPrimary, modifier = modifier)
}
