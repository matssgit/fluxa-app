package com.fluxa.personal

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.fluxa.personal.core.preferences.ThemePreference
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.FragmentActivity
import com.fluxa.personal.security.AndroidAppAuthenticator
import com.fluxa.personal.security.AppLockState
import com.fluxa.personal.security.AuthenticationPurpose
import com.fluxa.personal.security.AuthenticationLauncher
import com.fluxa.personal.ui.LocalAuthenticationLauncher
import com.fluxa.personal.ui.FluxaApp
import com.fluxa.personal.ui.LocalSecurityGate

class MainActivity : FragmentActivity() {
    private val fluxa get() = application as FluxaApplication
    private lateinit var curtain: View
    private lateinit var composeContent: ComposeView
    private var renderedContentAllowed = false
    private lateinit var authenticator: AndroidAppAuthenticator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        authenticator = AndroidAppAuthenticator(this, fluxa.security, fluxa.securityScope)
        val root = FrameLayout(this)
        val content = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        composeContent = content
        curtain = View(this).apply {
            setBackgroundColor(curtainColor())
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            isClickable = true
        }
        val match = FrameLayout.LayoutParams(-1, -1)
        root.addView(content, match)
        root.addView(curtain, FrameLayout.LayoutParams(-1, -1))
        root.viewTreeObserver.addOnPreDrawListener {
            // Read the synchronous policy, even if the Compose StateFlow collector has not run yet.
            if (!fluxa.security.state.value.canShowContent && renderedContentAllowed) setCurtain(true)
            true
        }
        setContentView(root)
        // FLAG_SECURE global, decidido só pela preferência protectScreenContent (nunca por tela).
        applyScreenProtection(fluxa.security.state.value.settings?.protectScreenContent == true)
        lifecycleScope.launch {
            fluxa.security.state.map { it.settings?.protectScreenContent == true }.distinctUntilChanged()
                .collect(::applyScreenProtection)
        }
        lifecycleScope.launch {
            fluxa.preferences.settings.map { it.theme }.distinctUntilChanged().collect {
                lastTheme = it
                curtain.setBackgroundColor(curtainColor())
            }
        }
        content.setContent {
            // Tema explícito também na barreira: lido do DataStore (não financeiro); até a primeira leitura, o do sistema.
            val themes = remember { fluxa.preferences.settings.map { it.theme } }
            val theme by themes.collectAsState(lastTheme)
            CompositionLocalProvider(
                LocalAuthenticationLauncher provides AuthenticationLauncher(::requestAuthentication),
            ) {
                LocalSecurityGate(
                    manager = fluxa.security,
                    themePreference = theme,
                    onAuthenticate = { requestAuthentication(AuthenticationPurpose.UNLOCK) },
                    onRendered = ::securityRendered,
                ) { FluxaApp(fluxa) }
            }
        }
    }

    private var lastTheme = ThemePreference.SYSTEM

    /** Fundo da cortina nativa igual ao da barreira (creme ou petróleo), sem clarão branco no tema escuro. */
    private fun curtainColor(): Int {
        val dark = when (lastTheme) {
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
            ThemePreference.SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        return if (dark) 0xFF0B1E1A.toInt() else 0xFFF3F0E7.toInt()
    }

    private fun applyScreenProtection(enabled: Boolean) {
        // Window.setFlags sempre reenvia os atributos da janela (relayout, reavaliação do teclado) mesmo sem mudança:
        // só toca na janela quando o estado desejado difere do atual.
        val secure = window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        if (secure == enabled) return
        if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun requestAuthentication(purpose: AuthenticationPurpose): Boolean {
        val request = fluxa.security.requestAuthentication(purpose) ?: return false
        authenticator.authenticate(request)
        return true
    }

    private fun securityRendered(rendered: AppLockState) {
        if (rendered != fluxa.security.state.value) return
        renderedContentAllowed = rendered.canShowContent
        setCurtain(false)
    }

    private fun setCurtain(visible: Boolean) {
        curtain.visibility = if (visible) View.VISIBLE else View.GONE
        composeContent.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
    }

    override fun onStart() {
        // Evaluate before resumed composition/window drawing, not in a LaunchedEffect after Dashboard.
        fluxa.security.onForeground()
        if (::curtain.isInitialized && !fluxa.security.state.value.canShowContent) setCurtain(true)
        super.onStart()
    }

    override fun onStop() {
        // ProcessLifecycleOwner coalesces rotation, but delays STOP. A real non-configuration Activity STOP
        // is the immediate visibility boundary; duplicate process events do not restart the timer.
        if (!isChangingConfigurations) fluxa.security.onBackground()
        super.onStop()
    }
}
