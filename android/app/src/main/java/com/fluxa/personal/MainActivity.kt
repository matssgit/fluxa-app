package com.fluxa.personal

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
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
            setBackgroundColor(getColor(android.R.color.background_light))
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
        content.setContent {
            CompositionLocalProvider(
                LocalAuthenticationLauncher provides AuthenticationLauncher(::requestAuthentication),
            ) {
                LocalSecurityGate(
                    manager = fluxa.security,
                    onAuthenticate = { requestAuthentication(AuthenticationPurpose.UNLOCK) },
                    onRendered = ::securityRendered,
                ) { FluxaApp(fluxa) }
            }
        }
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
