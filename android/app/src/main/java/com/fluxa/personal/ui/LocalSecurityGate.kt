package com.fluxa.personal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import com.fluxa.personal.R
import com.fluxa.personal.core.preferences.ThemePreference
import com.fluxa.personal.security.AppLockState
import com.fluxa.personal.security.AppLockStatus
import com.fluxa.personal.security.AuthenticationCapability
import com.fluxa.personal.security.AuthenticationLauncher
import com.fluxa.personal.security.LocalSecurityManager
import com.fluxa.personal.security.SecurityError
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

internal val LocalAuthenticationLauncher = staticCompositionLocalOf<AuthenticationLauncher> {
    error("Authentication host is not installed")
}

/** Somente para testes instrumentados que conduzem o callback do manager sem o prompt real. Produção: sempre true. */
internal object SecurityGateTestHook {
    @Volatile
    var autoPrompt: Boolean = true
}

/**
 * Raiz de segurança: `FluxaTheme → SecurityGate → (Initializing | Locked | AppContent)`.
 * [content] (navegação, Dashboard, consultas) só compõe com `canShowContent`; bloqueado ou inicializando, não existe
 * nenhum nó financeiro na árvore de composição nem na semântica.
 *
 * Ao bloquear, o estado salvável do conteúdo (back stack e campos `rememberSaveable`) fica guardado em
 * [rememberSaveableStateHolder] e volta ao desbloquear: detalhe → background → lock → unlock → mesmo detalhe.
 */
@Composable
internal fun LocalSecurityGate(
    manager: LocalSecurityManager,
    themePreference: ThemePreference,
    onAuthenticate: () -> Unit,
    onRendered: (AppLockState) -> Unit,
    content: @Composable () -> Unit,
) {
    val state by manager.state.collectAsState()
    val holder = rememberSaveableStateHolder()
    if (state.canShowContent) {
        Box(Modifier.fillMaxSize().testTag("security-content")) {
            holder.SaveableStateProvider(CONTENT_KEY) { content() }
        }
    } else {
        FluxaTheme(themePreference = themePreference) {
            SecurityBarrier(
                state = state,
                onUnlock = {
                    manager.refreshCapability()
                    onAuthenticate()
                },
                onRetry = manager::retryLoadingSettings,
            )
        }
        AutoUnlockRequest(state, manager, onAuthenticate)
    }
    SideEffect { onRendered(state) }
}

/**
 * Pede a autenticação uma vez ao entrar em LOCKED, só com o Activity em RESUMED e sem solicitação nem erro
 * pendentes. Cancelar ou falhar deixa `error` preenchido no manager: não há nova abertura automática (sem loop);
 * o usuário toca em Desbloquear. Uma solicitação já em andamento (rotação) é reaproveitada pelo host.
 */
@Composable
private fun AutoUnlockRequest(state: AppLockState, manager: LocalSecurityManager, onAuthenticate: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val eligible = state.wantsAutoPrompt()
    LaunchedEffect(eligible) {
        if (!eligible || !SecurityGateTestHook.autoPrompt) return@LaunchedEffect
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        if (manager.state.value.wantsAutoPrompt()) onAuthenticate()
    }
}

private fun AppLockState.wantsAutoPrompt() = status == AppLockStatus.LOCKED &&
    capability == AuthenticationCapability.AVAILABLE && authentication == null && error == null && !isUpdatingSettings

@Composable
private fun SecurityBarrier(state: AppLockState, onUnlock: () -> Unit, onRetry: () -> Unit) {
    val colors = FluxaTheme.colors
    val lockedTitle = stringResource(R.string.security_lock_title)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .semantics { paneTitle = lockedTitle }
            .testTag("security-gate"),
    ) {
        OrganicBackdrop()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.security_wordmark),
                style = FluxaTheme.typography.display,
                color = colors.brand,
                modifier = Modifier.testTag("security-wordmark"),
            )
            when (state.status) {
                AppLockStatus.INITIALIZING -> InitializingHint()
                AppLockStatus.INITIALIZATION_FAILED -> {
                    Spacer24()
                    Message(stringResource(R.string.security_settings_failed), announce = true)
                    Spacer24()
                    FluxaButton(
                        text = stringResource(R.string.security_retry),
                        onClick = onRetry,
                        modifier = Modifier.widthIn(min = 200.dp).testTag("security-retry"),
                    )
                }
                else -> LockedContent(state, onUnlock)
            }
        }
    }
}

@Composable
private fun LockedContent(state: AppLockState, onUnlock: () -> Unit) {
    val colors = FluxaTheme.colors
    Spacer24()
    Text(
        stringResource(R.string.security_lock_title),
        style = FluxaTheme.typography.title,
        color = colors.textPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() }.testTag("security-lock-title"),
    )
    Spacer24()
    FluxaButton(
        text = stringResource(R.string.security_unlock),
        onClick = onUnlock,
        enabled = state.authentication == null && !state.isUpdatingSettings,
        modifier = Modifier.widthIn(min = 200.dp).fillMaxWidth(0.8f).testTag("security-unlock"),
    )
    Box(Modifier.height(16.dp))
    val notice = when {
        state.capability == AuthenticationCapability.NOT_CONFIGURED -> R.string.security_capability_not_configured
        state.capability == AuthenticationCapability.UNAVAILABLE -> R.string.security_capability_unavailable
        state.error == SecurityError.CANCELLED -> R.string.security_lock_cancelled
        state.error == SecurityError.AUTHENTICATION_ERROR -> R.string.security_error_auth
        state.error == SecurityError.SETTINGS_UNAVAILABLE -> R.string.security_error_generic
        else -> null
    }
    if (notice != null) {
        Message(stringResource(notice), announce = true, modifier = Modifier.testTag("security-lock-notice"))
    } else {
        Message(stringResource(R.string.security_lock_help), announce = false)
    }
}

@Composable
private fun Message(text: String, announce: Boolean, modifier: Modifier = Modifier) {
    Text(
        text,
        style = FluxaTheme.typography.body,
        color = FluxaTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .widthIn(max = 360.dp)
            .semantics { if (announce) liveRegion = LiveRegionMode.Polite },
    )
}

/** Leitura da política costuma ser instantânea: o indicador discreto só aparece se demorar. */
@Composable
private fun InitializingHint() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(600)
        visible = true
    }
    Box(Modifier.height(56.dp), contentAlignment = Alignment.BottomCenter) {
        if (visible) {
            CircularProgressIndicator(
                color = FluxaTheme.colors.brand,
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp).testTag("security-initializing"),
            )
        }
    }
}

@Composable
private fun Spacer24() = Box(Modifier.height(24.dp))

/** Formas orgânicas sutis nos cantos (tom da marca, baixa opacidade). Puramente decorativo. */
@Composable
private fun OrganicBackdrop() {
    val tone = FluxaTheme.colors.brandTonal
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val top = Path().apply {
            moveTo(w * 0.45f, 0f)
            cubicTo(w * 0.62f, h * 0.10f, w * 0.80f, h * 0.02f, w, h * 0.16f)
            lineTo(w, 0f)
            close()
        }
        val bottom = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(w * 0.22f, h * 0.70f, w * 0.40f, h * 0.92f, w * 0.62f, h)
            lineTo(0f, h)
            close()
        }
        drawPath(top, tone.copy(alpha = 0.55f))
        drawPath(bottom, tone.copy(alpha = 0.45f))
        drawCircle(tone.copy(alpha = 0.25f), radius = w * 0.18f, center = Offset(w * 0.12f, h * 0.18f))
    }
}

private const val CONTENT_KEY = "fluxa-app-content"
