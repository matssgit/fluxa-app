package com.fluxa.personal

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.fluxa.personal.core.preferences.LocalSettings
import com.fluxa.personal.core.preferences.ThemePreference
import com.fluxa.personal.security.AndroidAppAuthenticator
import com.fluxa.personal.security.AppLockState
import com.fluxa.personal.security.AppLockStatus
import com.fluxa.personal.security.AuthenticationLauncher
import com.fluxa.personal.security.AuthenticationPurpose
import com.fluxa.personal.ui.FluxaApp
import com.fluxa.personal.ui.LocalAuthenticationLauncher
import com.fluxa.personal.ui.LocalSecurityGate
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val fluxa get() = application as FluxaApplication
    private lateinit var curtain: View
    private lateinit var composeContent: ComposeView
    private lateinit var authenticator: AndroidAppAuthenticator
    private var renderedContentAllowed = false
    private var startupPreferences by mutableStateOf<LocalSettings?>(null)
    private var preferencesReadFailed by mutableStateOf(false)
    private var preferencesJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Wait only for local data, never for a Compose frame (which can wait for drawing on recreation).
        // The opaque native curtain separately protects the interval until the gate composition commits.
        splash.setKeepOnScreenCondition {
            !preferencesReadFailed && (startupPreferences == null ||
                fluxa.security.state.value.status == AppLockStatus.INITIALIZING)
        }
        enableEdgeToEdge()
        authenticator = AndroidAppAuthenticator(this, fluxa.security, fluxa.securityScope)
        val root = FrameLayout(this).apply { setBackgroundColor(getColor(R.color.fluxa_startup_background)) }
        composeContent = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        curtain = View(this).apply {
            setBackgroundColor(curtainColor())
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            isClickable = true
        }
        root.addView(composeContent, FrameLayout.LayoutParams(-1, -1))
        root.addView(curtain, FrameLayout.LayoutParams(-1, -1))
        root.viewTreeObserver.addOnPreDrawListener {
            if (!fluxa.security.state.value.canShowContent && renderedContentAllowed) setCurtain(true)
            true
        }
        setContentView(root)
        applyScreenProtection(fluxa.security.state.value.settings?.protectScreenContent == true)
        lifecycleScope.launch {
            fluxa.security.state.map { it.settings?.protectScreenContent == true }.distinctUntilChanged()
                .collect(::applyScreenProtection)
        }
        loadStartupPreferences()
        composeContent.setContent {
            val settings = startupPreferences
            when {
                preferencesReadFailed -> FluxaTheme(themePreference = ThemePreference.DARK) {
                    Column(
                        Modifier.fillMaxSize().background(FluxaTheme.colors.background)
                            .padding(24.dp).testTag("startup-settings-error"),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.app_name), color = FluxaTheme.colors.textPrimary)
                        Text(stringResource(R.string.security_settings_failed), color = FluxaTheme.colors.textPrimary)
                        FluxaButton(stringResource(R.string.security_retry), onClick = {
                            loadStartupPreferences()
                            fluxa.security.retryLoadingSettings()
                        })
                    }
                    SideEffect {
                        renderedContentAllowed = false
                        setCurtain(false)
                    }
                }
                settings != null -> CompositionLocalProvider(
                    LocalAuthenticationLauncher provides AuthenticationLauncher(::requestAuthentication),
                ) {
                    LocalSecurityGate(
                        manager = fluxa.security,
                        themePreference = settings.theme,
                        onAuthenticate = { requestAuthentication(AuthenticationPurpose.UNLOCK) },
                        onRendered = ::securityRendered,
                    ) { FluxaApp(fluxa, initialSettings = settings) }
                }
            }
        }
    }

    private fun loadStartupPreferences() {
        preferencesJob?.cancel()
        preferencesReadFailed = false
        startupPreferences = null
        preferencesJob = lifecycleScope.launch {
            try {
                fluxa.preferences.settings.collect { settings ->
                    startupPreferences = settings
                    curtain.setBackgroundColor(curtainColor())
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Never release financial composition with guessed privacy preferences.
                preferencesReadFailed = true
            }
        }
    }

    private fun curtainColor(): Int {
        val theme = startupPreferences?.theme ?: return getColor(R.color.fluxa_startup_background)
        val dark = when (theme) {
            ThemePreference.LIGHT -> false
            ThemePreference.DARK -> true
            ThemePreference.SYSTEM ->
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        return if (dark) 0xFF0E171B.toInt() else 0xFFF6F2EA.toInt()
    }

    private fun applyScreenProtection(enabled: Boolean) {
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
        // Also synchronize the flag before the first authorized frame, independent of collector scheduling.
        applyScreenProtection(rendered.settings?.protectScreenContent == true)
        renderedContentAllowed = rendered.canShowContent
        setCurtain(false)
    }

    private fun setCurtain(visible: Boolean) {
        curtain.visibility = if (visible) View.VISIBLE else View.GONE
        composeContent.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
    }

    override fun onStart() {
        fluxa.security.onForeground()
        if (::curtain.isInitialized && !fluxa.security.state.value.canShowContent) setCurtain(true)
        super.onStart()
    }

    override fun onStop() {
        if (!isChangingConfigurations) fluxa.security.onBackground()
        super.onStop()
    }
}
