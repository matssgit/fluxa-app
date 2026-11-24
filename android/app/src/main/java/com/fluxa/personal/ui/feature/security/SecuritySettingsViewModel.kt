package com.fluxa.personal.ui.feature.security

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.security.AppLockState
import com.fluxa.personal.security.AppLockStatus
import com.fluxa.personal.security.AuthenticationCapability
import com.fluxa.personal.security.AuthenticationLauncher
import com.fluxa.personal.security.AuthenticationPurpose
import com.fluxa.personal.security.AutoLockTimeout
import com.fluxa.personal.security.LocalSecurityManager
import com.fluxa.personal.security.SecurityError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal object SecurityRoutes {
    const val SETTINGS = "security"
}

/** Recorte do [LocalSecurityManager] usado pela tela. A política continua inteira no manager. */
internal interface SecurityControls {
    val state: StateFlow<AppLockState>
    fun refreshCapability()
    suspend fun changeTimeout(timeout: AutoLockTimeout): Boolean
    suspend fun changeScreenProtection(enabled: Boolean): Boolean
}

internal class ManagerSecurityControls(private val manager: LocalSecurityManager) : SecurityControls {
    override val state get() = manager.state
    override fun refreshCapability() = manager.refreshCapability()
    override suspend fun changeTimeout(timeout: AutoLockTimeout) = manager.changeTimeout(timeout)
    override suspend fun changeScreenProtection(enabled: Boolean) = manager.changeScreenProtection(enabled)
}

internal data class SecuritySettingsUiState(
    val loaded: Boolean = false,
    val capability: AuthenticationCapability = AuthenticationCapability.UNAVAILABLE,
    /** Sempre o valor persistido: um prompt pendente ou cancelado nunca muda o toggle. */
    val appLockEnabled: Boolean = false,
    val timeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATELY,
    val protectScreen: Boolean = false,
    val busy: Boolean = false,
    @StringRes val message: Int? = null,
    @StringRes val error: Int? = null,
) {
    val capabilityAvailable get() = capability == AuthenticationCapability.AVAILABLE

    /** NOT_CONFIGURED/UNAVAILABLE: o toggle não pode ser acionado (nem para ligar, nem para desligar). */
    val lockToggleEnabled get() = loaded && !busy && capabilityAvailable

    /** A escolha do tempo só vale com o bloqueio ativo; a preferência salva é mantida ao desligar. */
    val timeoutEnabled get() = loaded && !busy && appLockEnabled
    val screenToggleEnabled get() = loaded && !busy

    @get:StringRes
    val capabilityNotice: Int? get() = when (capability) {
        AuthenticationCapability.AVAILABLE -> null
        AuthenticationCapability.NOT_CONFIGURED -> R.string.security_capability_not_configured
        AuthenticationCapability.UNAVAILABLE -> R.string.security_capability_unavailable
    }
}

/**
 * Apresentação da tela Segurança. Não decide política: ligar/desligar o bloqueio só pede a autenticação do Android
 * (o host da Activity mostra o prompt e o manager persiste depois do sucesso). Este ViewModel só acompanha a
 * solicitação que ele mesmo iniciou para dar o retorno ("Proteção ativada", cancelamento, erro).
 */
internal class SecuritySettingsViewModel(private val controls: SecurityControls) : ViewModel() {
    private val local = MutableStateFlow(Local())

    private data class Local(
        val awaiting: AuthenticationPurpose? = null,
        val requestId: Long? = null,
        val writing: Boolean = false,
        @StringRes val message: Int? = null,
        @StringRes val error: Int? = null,
    )

    val uiState: StateFlow<SecuritySettingsUiState> = combine(controls.state, local) { security, l ->
        val settings = security.settings
        SecuritySettingsUiState(
            loaded = settings != null && security.status != AppLockStatus.INITIALIZATION_FAILED,
            capability = security.capability,
            appLockEnabled = settings?.appLockEnabled == true,
            timeout = settings?.autoLockTimeout ?: AutoLockTimeout.IMMEDIATELY,
            protectScreen = settings?.protectScreenContent == true,
            busy = l.writing || l.awaiting != null || security.authentication != null || security.isUpdatingSettings,
            message = l.message,
            error = l.error,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SecuritySettingsUiState())

    init {
        controls.refreshCapability()
        viewModelScope.launch { controls.state.collect(::track) }
    }

    /** Toggle "Exigir autenticação". Nada é salvo aqui: o resultado vem do manager depois do prompt. */
    fun onLockToggle(enable: Boolean, launcher: AuthenticationLauncher) {
        val current = uiState.value
        if (!current.lockToggleEnabled || enable == current.appLockEnabled) return
        val purpose = if (enable) AuthenticationPurpose.ENABLE_LOCK else AuthenticationPurpose.DISABLE_LOCK
        local.value = Local(awaiting = purpose)
        val accepted = try { launcher.launch(purpose) } catch (_: Exception) { false }
        val security = controls.state.value
        if (!accepted) {
            local.value = Local(error = errorMessage(security.error) ?: R.string.security_error_generic)
            return
        }
        // O prompt pode já ter terminado (callback síncrono); `track` resolve pelo id da solicitação.
        local.value = local.value.copy(requestId = security.authentication?.id ?: NO_REQUEST)
        track(security)
    }

    fun onTimeoutSelected(timeout: AutoLockTimeout) {
        val current = uiState.value
        if (!current.timeoutEnabled || timeout == current.timeout) return
        write(R.string.security_timeout_saved) { controls.changeTimeout(timeout) }
    }

    fun onScreenProtectionToggle(enabled: Boolean) {
        val current = uiState.value
        if (!current.screenToggleEnabled || enabled == current.protectScreen) return
        write(if (enabled) R.string.security_screen_on else R.string.security_screen_off) { controls.changeScreenProtection(enabled) }
    }

    fun onResume() = controls.refreshCapability()

    fun dismissMessage() {
        local.value = local.value.copy(message = null, error = null)
    }

    private fun write(@StringRes success: Int, change: suspend () -> Boolean) {
        local.value = Local(writing = true)
        viewModelScope.launch {
            val ok = try { change() } catch (_: Exception) { false }
            local.value = if (ok) Local(message = success) else Local(error = R.string.security_error_save)
        }
    }

    private fun track(security: AppLockState) {
        val l = local.value
        val purpose = l.awaiting ?: return
        val requestId = l.requestId ?: return // ainda dentro de onLockToggle
        val stillRunning = (requestId != NO_REQUEST && security.authentication?.id == requestId) || security.isUpdatingSettings
        if (stillRunning) return
        val target = purpose == AuthenticationPurpose.ENABLE_LOCK
        local.value = when {
            security.settings?.appLockEnabled == target ->
                Local(message = if (target) R.string.security_lock_on else R.string.security_lock_off)
            // Cancelar não é erro: o toggle continua no estado anterior, sem mensagem agressiva.
            security.error == SecurityError.CANCELLED -> Local()
            else -> Local(error = errorMessage(security.error) ?: R.string.security_error_generic)
        }
    }

    companion object {
        private const val NO_REQUEST = -1L

        fun factory(controls: SecurityControls): ViewModelProvider.Factory = viewModelFactory {
            initializer { SecuritySettingsViewModel(controls) }
        }

        @StringRes
        internal fun errorMessage(error: SecurityError?): Int? = when (error) {
            null, SecurityError.CANCELLED -> null
            SecurityError.NOT_CONFIGURED -> R.string.security_capability_not_configured
            SecurityError.UNAVAILABLE -> R.string.security_capability_unavailable
            SecurityError.AUTHENTICATION_ERROR -> R.string.security_error_auth
            SecurityError.SETTINGS_UNAVAILABLE -> R.string.security_error_save
        }
    }
}
