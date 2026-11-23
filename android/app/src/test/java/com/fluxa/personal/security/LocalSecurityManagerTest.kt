package com.fluxa.personal.security

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalSecurityManagerTest {
    private class FakeClock(var time: Long = 1_000L) : ElapsedRealtimeClock {
        override fun nowMillis() = time
    }
    private class Store(initial: SecuritySettings) : SecuritySettingsStore {
        override val securitySettings = MutableStateFlow(initial)
        var writes = 0
        var fail = false
        var writeGate: CompletableDeferred<Unit>? = null
        override suspend fun writeSecuritySettings(settings: SecuritySettings) {
            writeGate?.await()
            if (fail) throw IOException("injected-settings-write")
            writes++
            securitySettings.value = settings
        }
    }
    private class Fixture(
        val test: TestScope,
        settings: SecuritySettings = SecuritySettings(appLockEnabled = true),
        var capability: AuthenticationCapability = AuthenticationCapability.AVAILABLE,
    ) {
        val clock = FakeClock()
        val store = Store(settings)
        val manager = LocalSecurityManager(store, { capability }, clock, test.backgroundScope)
        init { test.runCurrent() }
        suspend fun unlock() {
            val request = requireNotNull(manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
            assertTrue(manager.onAuthenticationSucceeded(request.id))
            test.runCurrent()
            assertEquals(AppLockStatus.UNLOCKED, manager.state.value.status)
        }
    }

    @Test fun initializingNeverGrantsAccessAndDisabledDefaultsLoadNormally() = runTest {
        val store = Store(SecuritySettings())
        val manager = LocalSecurityManager(store, { AuthenticationCapability.AVAILABLE }, FakeClock(), backgroundScope)
        assertEquals(AppLockStatus.INITIALIZING, manager.state.value.status)
        assertFalse(manager.state.value.canShowContent)
        runCurrent()
        assertEquals(AppLockStatus.DISABLED, manager.state.value.status)
        assertTrue(manager.state.value.canShowContent)
        assertEquals(AutoLockTimeout.IMMEDIATELY, manager.state.value.settings?.autoLockTimeout)
        assertFalse(requireNotNull(manager.state.value.settings).protectScreenContent)
    }

    @Test fun enabledStartsLockedAndSuccessIsSingleUse() = runTest {
        val f = Fixture(this)
        assertTrue(f.manager.state.value.needsAuthentication)
        assertFalse(f.manager.state.value.canShowContent)
        val request = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
        assertTrue(f.manager.onAuthenticationSucceeded(request.id))
        assertFalse(f.manager.onAuthenticationSucceeded(request.id))
        assertEquals(0, f.store.writes)
        assertTrue(f.manager.state.value.canShowContent)
    }

    @Test fun cancelledAndTerminalErrorStayLockedAndStaleSuccessCannotUnlock() = runTest {
        val f = Fixture(this)
        val first = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
        f.manager.onAuthenticationCancelled(first.id)
        assertEquals(SecurityError.CANCELLED, f.manager.state.value.error)
        assertFalse(f.manager.onAuthenticationSucceeded(first.id))
        val second = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
        f.manager.onAuthenticationError(second.id)
        assertEquals(SecurityError.AUTHENTICATION_ERROR, f.manager.state.value.error)
        assertFalse(f.manager.state.value.canShowContent)
    }

    @Test fun ordinaryBiometricRejectionLeavesTheSamePromptPendingForRetry() = runTest {
        val f = Fixture(this)
        val request = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
        repeat(3) { f.manager.onAuthenticationFailed(request.id) }
        assertEquals(request, f.manager.state.value.authentication)
        assertNull(f.manager.state.value.error)
        assertFalse(f.manager.state.value.canShowContent)
        assertTrue(f.manager.onAuthenticationSucceeded(request.id))
    }

    @Test fun immediateLocksAtRealBackgroundAndDoesNotUnlockOnReturn() = runTest {
        val f = Fixture(this)
        f.unlock()
        f.manager.onBackground()
        assertEquals(AppLockStatus.LOCKED, f.manager.state.value.status)
        f.manager.onForeground()
        assertFalse(f.manager.state.value.canShowContent)
    }

    @Test fun eachTimedPolicyUsesInclusiveBoundaryAndPreservesTheSessionBelowIt() = runTest {
        for (timeout in AutoLockTimeout.entries.filter { it != AutoLockTimeout.IMMEDIATELY }) {
            for (atBoundary in listOf(false, true)) {
                val f = Fixture(this, SecuritySettings(true, timeout))
                f.unlock()
                f.manager.onBackground()
                f.clock.time += timeout.milliseconds - if (atBoundary) 0 else 1
                f.manager.onForeground()
                assertEquals("$timeout, inclusive=$atBoundary", !atBoundary, f.manager.state.value.canShowContent)
            }
        }
    }

    @Test fun continuousForegroundNeverUsesAnInactivityTimeout() = runTest {
        val f = Fixture(this, SecuritySettings(true, AutoLockTimeout.AFTER_1_MINUTE))
        f.unlock()
        f.clock.time += 86_400_000L
        f.manager.onForeground()
        assertTrue(f.manager.state.value.canShowContent)
    }

    @Test fun repeatedLifecycleEventsCannotResetTheBackgroundTimer() = runTest {
        val f = Fixture(this, SecuritySettings(true, AutoLockTimeout.AFTER_5_MINUTES))
        f.unlock()
        f.manager.onBackground()
        f.clock.time += 299_999
        f.manager.onBackground()
        f.clock.time++
        f.manager.onForeground()
        f.manager.onForeground()
        assertFalse(f.manager.state.value.canShowContent)
    }

    @Test fun disabledPolicyDoesNotLockAndSessionIsNotWritten() = runTest {
        val f = Fixture(this, SecuritySettings())
        repeat(3) {
            f.manager.onBackground()
            f.clock.time += 900_000
            f.manager.onForeground()
        }
        assertTrue(f.manager.state.value.canShowContent)
        assertEquals(0, f.store.writes)
    }

    @Test fun newManagerAfterProcessDeathDoesNotRecoverAnUnlockedSession() = runTest {
        val f = Fixture(this)
        f.unlock()
        val replacement = LocalSecurityManager(f.store, { AuthenticationCapability.AVAILABLE }, f.clock, backgroundScope)
        assertFalse(replacement.state.value.canShowContent)
        runCurrent()
        assertEquals(AppLockStatus.LOCKED, replacement.state.value.status)
        assertEquals(0, f.store.writes)
    }

    @Test fun monotonicRollbackFailsClosedAndNoFinancialOrWallClockIsUsed() = runTest {
        val f = Fixture(this, SecuritySettings(true, AutoLockTimeout.AFTER_15_MINUTES))
        f.unlock()
        f.manager.onBackground()
        f.clock.time = 999
        f.manager.onForeground()
        assertFalse(f.manager.state.value.canShowContent)
    }

    @Test fun enablingRequiresCapabilityAndFreshAuthenticationBeforePersistingTrue() = runTest {
        for (capability in AuthenticationCapability.entries) {
            val f = Fixture(this, SecuritySettings(), capability)
            val request = f.manager.requestAuthentication(AuthenticationPurpose.ENABLE_LOCK)
            assertEquals(0, f.store.writes)
            assertFalse(f.store.securitySettings.value.appLockEnabled)
            if (capability == AuthenticationCapability.AVAILABLE) {
                assertTrue(f.manager.onAuthenticationSucceeded(requireNotNull(request).id))
                runCurrent()
                assertTrue(f.store.securitySettings.value.appLockEnabled)
                assertEquals(AppLockStatus.UNLOCKED, f.manager.state.value.status)
            } else {
                assertNull(request)
                assertEquals(0, f.store.writes)
            }
        }
    }

    @Test fun disablingRequiresANewPromptEvenInsideAnUnlockedSession() = runTest {
        val f = Fixture(this)
        assertNull(f.manager.requestAuthentication(AuthenticationPurpose.DISABLE_LOCK))
        assertFalse(f.manager.changeTimeout(AutoLockTimeout.AFTER_5_MINUTES))
        assertFalse(f.manager.changeScreenProtection(true))
        f.unlock()
        assertTrue(f.store.securitySettings.value.appLockEnabled)
        val request = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.DISABLE_LOCK))
        f.manager.onAuthenticationCancelled(request.id)
        assertTrue(f.store.securitySettings.value.appLockEnabled)
        val confirmed = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.DISABLE_LOCK))
        assertTrue(f.manager.onAuthenticationSucceeded(confirmed.id))
        runCurrent()
        assertEquals(AppLockStatus.DISABLED, f.manager.state.value.status)
    }

    @Test fun timeoutAndScreenProtectionCanChangeOnlyWithinAccessibleContent() = runTest {
        val f = Fixture(this)
        f.unlock()
        assertTrue(f.manager.changeTimeout(AutoLockTimeout.AFTER_15_MINUTES))
        assertTrue(f.manager.changeScreenProtection(true))
        runCurrent()
        assertEquals(SecuritySettings(true, AutoLockTimeout.AFTER_15_MINUTES, true), f.store.securitySettings.value)
        f.manager.onBackground()
        f.clock.time += 900_000
        f.manager.onForeground()
        assertFalse(f.manager.changeTimeout(AutoLockTimeout.IMMEDIATELY))
        assertFalse(f.manager.changeScreenProtection(false))
    }

    @Test fun capabilityIsCheckedAgainWhenCommittingAnAuthenticatedSetting() = runTest {
        val f = Fixture(this, SecuritySettings())
        val request = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.ENABLE_LOCK))
        f.capability = AuthenticationCapability.NOT_CONFIGURED
        assertFalse(f.manager.onAuthenticationSucceeded(request.id))
        assertFalse(f.store.securitySettings.value.appLockEnabled)
        assertEquals(0, f.store.writes)
    }

    @Test fun settingsReadAndWriteFailuresDoNotDisableAnExistingProtection() = runTest {
        val failedRead = object : SecuritySettingsStore {
            override val securitySettings = flow<SecuritySettings> { throw IOException("injected-read") }
            override suspend fun writeSecuritySettings(settings: SecuritySettings) = Unit
        }
        val manager = LocalSecurityManager(failedRead, { AuthenticationCapability.AVAILABLE }, FakeClock(), backgroundScope)
        runCurrent()
        assertEquals(AppLockStatus.INITIALIZATION_FAILED, manager.state.value.status)
        assertFalse(manager.state.value.canShowContent)
        val f = Fixture(this)
        f.unlock()
        f.store.fail = true
        val disable = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.DISABLE_LOCK))
        assertFalse(f.manager.onAuthenticationSucceeded(disable.id))
        assertTrue(f.store.securitySettings.value.appLockEnabled)
        assertEquals(SecurityError.SETTINGS_UNAVAILABLE, f.manager.state.value.error)
    }

    @Test fun safReturnIsExemptInEitherLifecycleCallbackOrderButTheNextBackgroundLocks() = runTest {
        for (callbackFirst in listOf(false, true)) {
            val f = Fixture(this)
            f.unlock()
            val lease = requireNotNull(f.manager.beginExternalActivity())
            f.manager.onBackground()
            f.clock.time += 5_000
            if (callbackFirst) f.manager.finishExternalActivity(lease)
            f.manager.onForeground()
            if (!callbackFirst) f.manager.finishExternalActivity(lease)
            assertTrue(f.manager.state.value.canShowContent)
            f.manager.onBackground()
            f.manager.onForeground()
            assertFalse(f.manager.state.value.canShowContent)
            f.manager.finishExternalActivity(lease)
            assertFalse(f.manager.state.value.canShowContent)
        }
    }

    @Test fun abandonedExternalFlowCannotSuspendAutoLockPermanently() = runTest {
        for (callbackFirst in listOf(false, true)) {
            val f = Fixture(this)
            f.unlock()
            val lease = requireNotNull(f.manager.beginExternalActivity())
            f.manager.onBackground()
            if (callbackFirst) f.manager.finishExternalActivity(lease)
            f.clock.time += LocalSecurityManager.MAX_EXTERNAL_FLOW_MILLIS
            f.manager.onForeground()
            assertFalse(f.manager.state.value.canShowContent)
            f.manager.finishExternalActivity(lease)
            assertFalse(f.manager.state.value.canShowContent)
        }
    }

    @Test fun promptLifecycleDoesNotLoopOrReplaceTheInFlightRequest() = runTest {
        val f = Fixture(this)
        val request = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
        f.manager.onBackground()
        f.manager.onForeground()
        assertEquals(request, f.manager.state.value.authentication)
        assertNull(f.manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
        assertTrue(f.manager.onAuthenticationSucceeded(request.id))
        f.manager.onBackground()
        f.manager.onForeground()
        assertFalse(f.manager.state.value.canShowContent)
    }

    @Test fun credentialSuccessBeforeForegroundDoesNotImmediatelyRelockOrPersistASession() = runTest {
        val f = Fixture(this)
        val request = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
        f.manager.onBackground()
        assertTrue(f.manager.onAuthenticationSucceeded(request.id))
        f.manager.onForeground()
        assertTrue(f.manager.state.value.canShowContent)
        assertEquals(0, f.store.writes)
        f.manager.onBackground()
        f.manager.onForeground()
        assertFalse(f.manager.state.value.canShowContent)
    }

    @Test fun externalCallbackBeforeForegroundCannotRenewAnAlmostExpiredLease() = runTest {
        val f = Fixture(this)
        f.unlock()
        val lease = requireNotNull(f.manager.beginExternalActivity())
        f.manager.onBackground()
        f.clock.time += LocalSecurityManager.MAX_EXTERNAL_FLOW_MILLIS - 1
        f.manager.finishExternalActivity(lease)
        f.clock.time++
        f.manager.onForeground()
        assertFalse(f.manager.state.value.canShowContent)
    }

    @Test fun expiredPromptCallbackCannotUnlockAndCanBeRetried() = runTest {
        val f = Fixture(this)
        val request = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
        f.clock.time += LocalSecurityManager.MAX_EXTERNAL_FLOW_MILLIS
        assertFalse(f.manager.onAuthenticationSucceeded(request.id))
        assertFalse(f.manager.state.value.canShowContent)
        assertNotNull(f.manager.requestAuthentication(AuthenticationPurpose.UNLOCK))
    }

    @Test fun launchFailureConsumesTheLeaseAndBackgroundLocksAgain() = runTest {
        val f = Fixture(this)
        f.unlock()
        val external = TrustedExternalActivity(f.manager)
        val failure = runCatching { external.launch { throw IllegalStateException("no external activity") } }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        f.manager.onBackground()
        f.manager.onForeground()
        assertFalse(f.manager.state.value.canShowContent)
    }

    @Test fun serializedPreferenceWritesKeepAuthenticatedEnableAndTimeoutChanges() = runTest {
        val f = Fixture(this, SecuritySettings())
        f.store.writeGate = CompletableDeferred()
        val request = requireNotNull(f.manager.requestAuthentication(AuthenticationPurpose.ENABLE_LOCK))
        val enable = async { f.manager.onAuthenticationSucceeded(request.id) }
        runCurrent()
        assertNull(f.manager.beginExternalActivity())
        assertNull(f.manager.requestAuthentication(AuthenticationPurpose.ENABLE_LOCK))
        val timeout = async { f.manager.changeTimeout(AutoLockTimeout.AFTER_5_MINUTES) }
        runCurrent()
        f.store.writeGate?.complete(Unit)
        assertTrue(enable.await())
        assertTrue(timeout.await())
        runCurrent()
        assertEquals(SecuritySettings(true, AutoLockTimeout.AFTER_5_MINUTES), f.store.securitySettings.value)
    }
}
