package com.fluxa.personal.security

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * No Android/UI/financial-clock dependency. Session and leases live only in this process.
 * A short monitor serializes lifecycle/callback transitions; the mutex serializes suspending preference writes.
 */
internal class LocalSecurityManager(
    private val store: SecuritySettingsStore,
    private val capabilities: AuthenticationCapabilities,
    private val clock: ElapsedRealtimeClock,
    private val scope: CoroutineScope,
) {
    private val monitor = Any()
    private val settingsWrites = Mutex()
    private val mutableState = MutableStateFlow(AppLockState())
    val state: StateFlow<AppLockState> = mutableState.asStateFlow()
    private var settings: SecuritySettings? = null
    private var capability = AuthenticationCapability.UNAVAILABLE
    private var unlocked = false
    private var foreground = true
    private var backgroundAt: Long? = null
    private var pending: AuthenticationRequest? = null
    private var external: ExternalActivityLease? = null
    private var returningExternalAt: Long? = null
    private var nextId = 0L
    private var error: SecurityError? = null
    private var loadFailed = false
    private var authenticatedChangeInFlight = false
    private var loading: Job? = null

    init { retryLoadingSettings() }

    fun retryLoadingSettings() = synchronized(monitor) {
        if (loading?.isActive == true) return@synchronized
        loading = scope.launch {
            try {
                store.securitySettings.collect { value ->
                    synchronized(monitor) {
                        applySettings(value)
                        loadFailed = false
                        refreshCapabilityLocked()
                        publish()
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                synchronized(monitor) {
                    loadFailed = true
                    unlocked = false
                    pending = null
                    external = null
                    error = SecurityError.SETTINGS_UNAVAILABLE
                    publish()
                }
            }
        }
    }

    fun refreshCapability() = synchronized(monitor) {
        refreshCapabilityLocked()
        publish()
    }

    private fun refreshCapabilityLocked() {
        capability = try { capabilities.check() } catch (_: Exception) { AuthenticationCapability.UNAVAILABLE }
    }

    private fun applySettings(value: SecuritySettings) {
        if (settings?.appLockEnabled != value.appLockEnabled) {
            // First read with lock enabled and every newly enabled policy start locked.
            unlocked = false
            pending = null
            external = null
            returningExternalAt = null
        }
        settings = value
    }

    fun onBackground() = synchronized(monitor) {
        if (!foreground) return@synchronized
        foreground = false
        backgroundAt = clock.nowMillis()
        if (!hasTrustedFlow() && settings?.autoLockTimeout == AutoLockTimeout.IMMEDIATELY) lockSession()
        publish()
    }

    /** Called synchronously before Activity drawing; duplicates from process/activity lifecycle are harmless. */
    fun onForeground() = synchronized(monitor) {
        refreshCapabilityLocked()
        if (!foreground) {
            val now = clock.nowMillis()
            val hadFlow = external != null || pending != null || returningExternalAt != null
            val trusted = hasTrustedFlow(now) || returningExternalAt?.let { validAge(it) } == true
            if (hadFlow && !trusted) {
                external = null
                pending = null
                lockSession()
            } else if (!trusted) {
                backgroundAt?.let { started ->
                    val elapsed = now - started
                    if (elapsed < 0 || elapsed >= (settings?.autoLockTimeout?.milliseconds ?: 0)) lockSession()
                }
            }
            foreground = true
            backgroundAt = null
            returningExternalAt = null
        }
        publish()
    }

    fun requestAuthentication(purpose: AuthenticationPurpose): AuthenticationRequest? = synchronized(monitor) {
        val current = settings ?: return@synchronized null
        if (loadFailed || !foreground || pending != null || external != null || authenticatedChangeInFlight) return@synchronized null
        val allowed = when (purpose) {
            AuthenticationPurpose.UNLOCK -> current.appLockEnabled && !unlocked
            AuthenticationPurpose.ENABLE_LOCK -> !current.appLockEnabled
            AuthenticationPurpose.DISABLE_LOCK -> current.appLockEnabled && unlocked
        }
        if (!allowed) return@synchronized null
        refreshCapabilityLocked()
        if (capability != AuthenticationCapability.AVAILABLE) {
            error = if (capability == AuthenticationCapability.NOT_CONFIGURED) SecurityError.NOT_CONFIGURED else SecurityError.UNAVAILABLE
            publish()
            return@synchronized null
        }
        AuthenticationRequest(++nextId, purpose, clock.nowMillis()).also {
            pending = it
            error = null
            publish()
        }
    }

    /** Nonterminal biometric rejection: leave the same system prompt in charge of retries. */
    fun onAuthenticationFailed(requestId: Long) = synchronized(monitor) {
        if (pending?.id != requestId) return@synchronized
        publish()
    }

    fun onAuthenticationCancelled(requestId: Long) = endAuthentication(requestId, SecurityError.CANCELLED)
    fun onAuthenticationError(requestId: Long) = endAuthentication(requestId, SecurityError.AUTHENTICATION_ERROR)

    private fun endAuthentication(requestId: Long, failure: SecurityError) = synchronized(monitor) {
        if (pending?.id != requestId) return@synchronized
        pending = null
        error = failure
        if (!foreground && settings?.autoLockTimeout == AutoLockTimeout.IMMEDIATELY) lockSession()
        publish()
    }

    /**
     * Only the system-prompt adapter calls this. Request identity/expiry prevents stale or repeated callbacks.
     * Enabling and disabling always require a new authenticated request; an unlocked session is not a disable proof.
     */
    suspend fun onAuthenticationSucceeded(requestId: Long): Boolean = settingsWrites.withLock {
        val authorization = synchronized(monitor) {
            val request = pending
            if (request?.id != requestId) return@synchronized null
            if (!validAge(request.startedAt)) {
                lockSession()
                error = SecurityError.AUTHENTICATION_ERROR
                publish()
                return@synchronized null
            }
            refreshCapabilityLocked()
            if (capability != AuthenticationCapability.AVAILABLE) {
                pending = null
                error = SecurityError.UNAVAILABLE
                publish()
                return@synchronized null
            }
            val current = settings ?: return@synchronized null
            authenticatedChangeInFlight = true
            pending = null
            backgroundAt = if (foreground) null else clock.nowMillis()
            returningExternalAt = if (foreground) null else clock.nowMillis()
            external = null
            error = null
            publish()
            request to current
        } ?: return@withLock false
        val (request, previous) = authorization
        val next = when (request.purpose) {
            AuthenticationPurpose.UNLOCK -> previous
            AuthenticationPurpose.ENABLE_LOCK -> previous.copy(appLockEnabled = true)
            AuthenticationPurpose.DISABLE_LOCK -> previous.copy(appLockEnabled = false)
        }
        try {
            if (next != previous && !persist(next)) return@withLock false
            synchronized(monitor) {
                applySettings(next)
                unlocked = next.appLockEnabled
                // A successful system authentication itself is a fresh proof, including credential-Activity return.
                error = null
            }
            true
        } finally {
            synchronized(monitor) {
                authenticatedChangeInFlight = false
                publish()
            }
        }
    }

    suspend fun changeTimeout(timeout: AutoLockTimeout): Boolean = changeSettings { it.copy(autoLockTimeout = timeout) }
    suspend fun changeScreenProtection(enabled: Boolean): Boolean = changeSettings { it.copy(protectScreenContent = enabled) }

    private suspend fun changeSettings(change: (SecuritySettings) -> SecuritySettings): Boolean = settingsWrites.withLock {
        val next = synchronized(monitor) {
            if (!foreground || !state.value.canShowContent || pending != null) return@synchronized null
            settings?.let(change)
        } ?: return@withLock false
        if (!persist(next)) return@withLock false
        synchronized(monitor) { applySettings(next); publish() }
        true
    }

    private suspend fun persist(value: SecuritySettings): Boolean = try {
        store.writeSecuritySettings(value)
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        synchronized(monitor) { error = SecurityError.SETTINGS_UNAVAILABLE; publish() }
        false
    }

    /** Only for an explicit external Activity launched by Fluxa, not for generic pauses. One lease at a time. */
    fun beginExternalActivity(): ExternalActivityLease? = synchronized(monitor) {
        if (!foreground || !state.value.canShowContent || external != null || pending != null || authenticatedChangeInFlight) return@synchronized null
        ExternalActivityLease(++nextId, clock.nowMillis()).also { external = it }
    }

    /** Callback or launch failure consumes the lease. Repeated/stale callbacks cannot grant another exemption. */
    fun finishExternalActivity(lease: ExternalActivityLease) = synchronized(monitor) {
        if (external != lease) return@synchronized
        val valid = validAge(lease.startedAt)
        external = null
        if (valid) {
            backgroundAt = null
            returningExternalAt = if (foreground) null else lease.startedAt
        } else {
            lockSession()
        }
        publish()
    }

    fun abandonExternalActivity(lease: ExternalActivityLease) = synchronized(monitor) {
        if (external != lease) return@synchronized
        external = null
        if (!foreground) lockSession()
        publish()
    }

    private fun validAge(startedAt: Long): Boolean = (clock.nowMillis() - startedAt) in 0 until MAX_EXTERNAL_FLOW_MILLIS
    private fun hasTrustedFlow(now: Long = clock.nowMillis()): Boolean =
        external?.let { now - it.startedAt in 0 until MAX_EXTERNAL_FLOW_MILLIS } == true ||
            pending?.let { now - it.startedAt in 0 until MAX_EXTERNAL_FLOW_MILLIS } == true

    private fun lockSession() {
        unlocked = false
        pending = null
        returningExternalAt = null
    }

    private fun publish() {
        val current = settings
        val status = when {
            loadFailed -> AppLockStatus.INITIALIZATION_FAILED
            current == null -> AppLockStatus.INITIALIZING
            !current.appLockEnabled -> AppLockStatus.DISABLED
            unlocked -> AppLockStatus.UNLOCKED
            else -> AppLockStatus.LOCKED
        }
        mutableState.value = AppLockState(status, current, capability, pending, error, authenticatedChangeInFlight)
    }

    companion object {
        /** Technical safety ceiling for abandoned SAF/prompt callbacks; not the user's auto-lock setting. */
        const val MAX_EXTERNAL_FLOW_MILLIS = 300_000L
    }
}
