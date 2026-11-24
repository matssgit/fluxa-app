package com.fluxa.personal.ui.feature.security

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fluxa.personal.R
import com.fluxa.personal.security.AutoLockTimeout
import com.fluxa.personal.ui.LocalAuthenticationLauncher
import com.fluxa.personal.ui.common.ErrorBanner
import com.fluxa.personal.ui.common.FeatureScreen
import com.fluxa.personal.ui.common.FeedbackBanner
import com.fluxa.personal.ui.common.SwitchRow
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import com.fluxa.personal.ui.designsystem.components.FluxaButtonVariant
import com.fluxa.personal.ui.designsystem.components.FluxaCard

@Composable
internal fun SecurityScreen(viewModel: SecuritySettingsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val launcher = LocalAuthenticationLauncher.current
    val context = LocalContext.current
    // Ao voltar das configurações do Android (ex.: depois de cadastrar um bloqueio de tela), reavalia a capability.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    Box(Modifier.fillMaxSize()) {
        FeatureScreen(
            title = stringResource(R.string.security_title),
            overline = stringResource(R.string.security_overline),
            onBack = onBack,
            modifier = Modifier.testTag("screen-security"),
        ) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.security_intro),
                    style = FluxaTheme.typography.body,
                    color = FluxaTheme.colors.textSecondary,
                )
            }
            state.error?.let { error ->
                item(key = "error") { ErrorBanner(stringResource(error), onDismiss = viewModel::dismissMessage) }
            }
            item(key = "lock") {
                SectionCard(icon = FluxaIcons.Lock, title = stringResource(R.string.security_lock_section), tag = "security-lock-card") {
                    SwitchRow(
                        label = stringResource(R.string.security_lock_toggle),
                        supporting = stringResource(R.string.security_lock_supporting),
                        checked = state.appLockEnabled,
                        onCheckedChange = { viewModel.onLockToggle(it, launcher) },
                        enabled = state.lockToggleEnabled,
                        modifier = Modifier.testTag("security-lock-toggle"),
                    )
                    state.capabilityNotice?.let { notice ->
                        CapabilityNotice(
                            text = stringResource(notice),
                            showSettings = state.capability == com.fluxa.personal.security.AuthenticationCapability.NOT_CONFIGURED,
                            onOpenSettings = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                                } catch (_: ActivityNotFoundException) {
                                }
                            },
                        )
                    }
                }
            }
            item(key = "timeout") { TimeoutCard(state, viewModel::onTimeoutSelected) }
            item(key = "screen") {
                SectionCard(icon = FluxaIcons.EyeOff, title = stringResource(R.string.security_screen_section), tag = "security-screen-card") {
                    SwitchRow(
                        label = stringResource(R.string.security_screen_toggle),
                        supporting = stringResource(R.string.security_screen_supporting),
                        checked = state.protectScreen,
                        onCheckedChange = viewModel::onScreenProtectionToggle,
                        enabled = state.screenToggleEnabled,
                        modifier = Modifier.testTag("security-screen-toggle"),
                    )
                }
            }
            item(key = "independent") {
                Text(
                    stringResource(R.string.security_independent_note),
                    style = FluxaTheme.typography.caption,
                    color = FluxaTheme.colors.textMuted,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
        FeedbackBanner(
            message = state.message?.let { stringResource(it) },
            onDismiss = viewModel::dismissMessage,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SectionCard(icon: ImageVector, title: String, tag: String, content: @Composable () -> Unit) {
    val colors = FluxaTheme.colors
    FluxaCard(modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(colors.brandTonal, FluxaTheme.shapes.pill), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = colors.onBrandTonal, modifier = Modifier.size(18.dp))
                }
                Text(title, style = FluxaTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.semantics { heading() })
            }
            content()
        }
    }
}

@Composable
private fun CapabilityNotice(text: String, showSettings: Boolean, onOpenSettings: () -> Unit) {
    val colors = FluxaTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.warningContainer, FluxaTheme.shapes.medium)
            .padding(12.dp)
            .testTag("security-capability"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(FluxaIcons.Alert, contentDescription = null, tint = colors.warning, modifier = Modifier.size(18.dp))
            Text(text, style = FluxaTheme.typography.label, color = colors.textPrimary)
        }
        if (showSettings) {
            FluxaButton(
                text = stringResource(R.string.security_open_android_settings),
                onClick = onOpenSettings,
                variant = FluxaButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth().testTag("security-open-settings"),
            )
        }
    }
}

@Composable
private fun TimeoutCard(state: SecuritySettingsUiState, onSelect: (AutoLockTimeout) -> Unit) {
    val colors = FluxaTheme.colors
    val inactive = stringResource(R.string.security_timeout_inactive)
    SectionCard(icon = FluxaIcons.Clock, title = stringResource(R.string.security_timeout_section), tag = "security-timeout-card") {
        Text(
            stringResource(if (state.appLockEnabled) R.string.security_timeout_supporting else R.string.security_timeout_disabled_note),
            style = FluxaTheme.typography.caption,
            color = colors.textMuted,
        )
        Column(Modifier.selectableGroup()) {
            AutoLockTimeout.entries.forEach { timeout ->
                val selected = state.timeout == timeout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = selected, enabled = state.timeoutEnabled, role = Role.RadioButton) { onSelect(timeout) }
                        .semantics { if (!state.appLockEnabled) stateDescription = inactive }
                        .testTag("security-timeout-${timeout.name}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = null,
                        enabled = state.timeoutEnabled,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.brand,
                            unselectedColor = colors.borderStrong,
                            disabledSelectedColor = colors.textMuted,
                            disabledUnselectedColor = colors.border,
                        ),
                    )
                    Text(
                        stringResource(timeoutLabel(timeout)),
                        style = FluxaTheme.typography.body,
                        color = if (state.timeoutEnabled) colors.textPrimary else colors.textMuted,
                    )
                }
            }
        }
    }
}

internal fun timeoutLabel(timeout: AutoLockTimeout): Int = when (timeout) {
    AutoLockTimeout.IMMEDIATELY -> R.string.security_timeout_immediately
    AutoLockTimeout.AFTER_1_MINUTE -> R.string.security_timeout_1
    AutoLockTimeout.AFTER_5_MINUTES -> R.string.security_timeout_5
    AutoLockTimeout.AFTER_15_MINUTES -> R.string.security_timeout_15
}
