package com.fluxa.personal.security

import android.app.KeyguardManager
import android.content.Context
import android.os.SystemClock
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.fluxa.personal.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal object AndroidElapsedRealtimeClock : ElapsedRealtimeClock {
    override fun nowMillis() = SystemClock.elapsedRealtime()
}

/** One capability query for both authenticators; a device credential alone is sufficient on minSdk 30. */
internal class AndroidAuthenticationCapabilities(context: Context) : AuthenticationCapabilities {
    private val manager = BiometricManager.from(context.applicationContext)
    private val keyguard = context.getSystemService(KeyguardManager::class.java)
    override fun check(): AuthenticationCapability = when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
        BiometricManager.BIOMETRIC_SUCCESS -> AuthenticationCapability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> AuthenticationCapability.NOT_CONFIGURED
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
            if (keyguard.isDeviceSecure) AuthenticationCapability.UNAVAILABLE else AuthenticationCapability.NOT_CONFIGURED
        else -> AuthenticationCapability.UNAVAILABLE
    }
}

internal const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

internal class SecurityProcessLifecycle(private val manager: LocalSecurityManager) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) = manager.onForeground()
    override fun onStop(owner: LifecycleOwner) = manager.onBackground()
}

/** Recreate this adapter with each Activity, reattaching the callback to an existing prompt on rotation. */
internal class AndroidAppAuthenticator(
    activity: FragmentActivity,
    private val manager: LocalSecurityManager,
    private val scope: CoroutineScope,
) {
    private var active = manager.state.value.authentication
    private val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val request = active ?: return
                active = null
                scope.launch { manager.onAuthenticationSucceeded(request.id) }
            }
            override fun onAuthenticationFailed() {
                active?.let { manager.onAuthenticationFailed(it.id) }
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val request = active ?: return
                active = null
                when (errorCode) {
                    BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> manager.onAuthenticationCancelled(request.id)
                    else -> manager.onAuthenticationError(request.id)
                }
            }
        })
    private val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.security_auth_title))
        .setSubtitle(activity.getString(R.string.security_auth_subtitle))
        .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
        .build()

    fun authenticate(request: AuthenticationRequest) {
        if (manager.state.value.authentication != request) return
        active = request
        try { prompt.authenticate(info) } catch (_: Exception) {
            active = null
            manager.onAuthenticationError(request.id)
        }
    }
}
