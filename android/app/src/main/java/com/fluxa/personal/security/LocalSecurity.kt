package com.fluxa.personal.security

import kotlinx.coroutines.flow.Flow

internal enum class AutoLockTimeout(val milliseconds: Long) {
    IMMEDIATELY(0),
    AFTER_1_MINUTE(60_000),
    AFTER_5_MINUTES(300_000),
    AFTER_15_MINUTES(900_000),
}

internal data class SecuritySettings(
    val appLockEnabled: Boolean = false,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATELY,
    val protectScreenContent: Boolean = false,
)

/** Device-local only. UI changes settings through LocalSecurityManager, never through this storage port. */
internal interface SecuritySettingsStore {
    val securitySettings: Flow<SecuritySettings>
    suspend fun writeSecuritySettings(settings: SecuritySettings)
}

internal fun interface ElapsedRealtimeClock {
    fun nowMillis(): Long
}

internal enum class AuthenticationCapability { AVAILABLE, NOT_CONFIGURED, UNAVAILABLE }
internal fun interface AuthenticationCapabilities {
    fun check(): AuthenticationCapability
}

internal enum class AppLockStatus { INITIALIZING, DISABLED, LOCKED, UNLOCKED, INITIALIZATION_FAILED }
internal enum class AuthenticationPurpose { UNLOCK, ENABLE_LOCK, DISABLE_LOCK }
/** Frontend requests an action; the Activity owns the Android prompt and delivers the result. */
internal fun interface AuthenticationLauncher {
    fun launch(purpose: AuthenticationPurpose): Boolean
}
internal enum class SecurityError { NOT_CONFIGURED, UNAVAILABLE, CANCELLED, AUTHENTICATION_ERROR, SETTINGS_UNAVAILABLE }

/** An in-memory, single-use request. Never put this or an external-flow lease in SavedStateHandle. */
internal data class AuthenticationRequest internal constructor(
    val id: Long,
    val purpose: AuthenticationPurpose,
    internal val startedAt: Long,
)

internal data class ExternalActivityLease internal constructor(val id: Long, internal val startedAt: Long)

internal data class AppLockState(
    val status: AppLockStatus = AppLockStatus.INITIALIZING,
    val settings: SecuritySettings? = null,
    val capability: AuthenticationCapability = AuthenticationCapability.UNAVAILABLE,
    val authentication: AuthenticationRequest? = null,
    val error: SecurityError? = null,
    val isUpdatingSettings: Boolean = false,
) {
    val canShowContent: Boolean get() = status == AppLockStatus.DISABLED || status == AppLockStatus.UNLOCKED
    val needsAuthentication: Boolean get() = status == AppLockStatus.LOCKED
}
