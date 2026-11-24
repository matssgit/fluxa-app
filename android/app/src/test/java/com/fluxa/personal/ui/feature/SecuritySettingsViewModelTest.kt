package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.security.AppLockStatus
import com.fluxa.personal.security.AuthenticationCapability
import com.fluxa.personal.security.AuthenticationLauncher
import com.fluxa.personal.security.AuthenticationPurpose
import com.fluxa.personal.security.AutoLockTimeout
import com.fluxa.personal.security.LocalSecurityManager
import com.fluxa.personal.security.SecurityError
import com.fluxa.personal.security.SecuritySettings
import com.fluxa.personal.security.SecuritySettingsStore
import com.fluxa.personal.ui.feature.security.ManagerSecurityControls
import com.fluxa.personal.ui.feature.security.SecuritySettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Armazenamento em memória que pode falhar na escrita. */
private class MemoryStore(initial: SecuritySettings = SecuritySettings()) : SecuritySettingsStore {
    val flow = MutableStateFlow(initial)
    var failWrites = false
    var writes = 0
    override val securitySettings = flow
    override suspend fun writeSecuritySettings(settings: SecuritySettings) {
        writes++
        if (failWrites) error("disk")
        flow.value = settings
    }
}

/**
 * Usa o LocalSecurityManager real (política intacta); só o "prompt do Android" é simulado: o launcher registra a
 * solicitação e o teste decide o desfecho chamando os callbacks do manager, como o host da Activity faz.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SecuritySettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var store: MemoryStore
    private var capability = AuthenticationCapability.AVAILABLE
    private var now = 1_000L
    private lateinit var manager: LocalSecurityManager
    private lateinit var vm: SecuritySettingsViewModel
    private val launched = mutableListOf<AuthenticationPurpose>()

    private val launcher = AuthenticationLauncher { purpose ->
        val request = manager.requestAuthentication(purpose)
        if (request != null) launched += purpose
        request != null
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.start(settings: SecuritySettings = SecuritySettings()) {
        store = MemoryStore(settings)
        manager = LocalSecurityManager(store, { capability }, { now }, backgroundScope)
        runCurrent()
        // Com o bloqueio já ligado, a tela só existe numa sessão desbloqueada.
        if (settings.appLockEnabled) succeed(manager.requestAuthentication(AuthenticationPurpose.UNLOCK)!!.id)
        vm = SecuritySettingsViewModel(ManagerSecurityControls(manager))
        runCurrent()
    }

    private fun TestScope.succeed(id: Long = manager.state.value.authentication!!.id) {
        backgroundScope.launch { manager.onAuthenticationSucceeded(id) }
        runCurrent()
    }

    private val state get() = vm.uiState.value

    @Test
    fun availableCapabilityAllowsTheLockToggle() = runTest(dispatcher) {
        start()
        assertTrue(state.loaded)
        assertTrue(state.lockToggleEnabled)
        assertNull(state.capabilityNotice)
        assertFalse(state.appLockEnabled)
        assertFalse(state.timeoutEnabled) // escolha inativa enquanto o bloqueio está desligado
        assertTrue(state.screenToggleEnabled)
    }

    @Test
    fun notConfiguredAndUnavailableDisableTheToggleWithGuidance() = runTest(dispatcher) {
        capability = AuthenticationCapability.NOT_CONFIGURED
        start()
        assertFalse(state.lockToggleEnabled)
        assertEquals(R.string.security_capability_not_configured, state.capabilityNotice)
        vm.onLockToggle(true, launcher)
        assertTrue(launched.isEmpty())

        capability = AuthenticationCapability.UNAVAILABLE
        vm.onResume()
        runCurrent()
        assertFalse(state.lockToggleEnabled)
        assertEquals(R.string.security_capability_unavailable, state.capabilityNotice)
    }

    @Test
    fun enablingOnlyAsksForAuthenticationAndPersistsAfterSuccess() = runTest(dispatcher) {
        start()
        vm.onLockToggle(true, launcher)
        runCurrent()
        assertEquals(listOf(AuthenticationPurpose.ENABLE_LOCK), launched)
        assertFalse(state.appLockEnabled) // nada salvo antes do sucesso
        assertTrue(state.busy)
        assertEquals(0, store.writes)
        succeed()
        assertTrue(state.appLockEnabled)
        assertTrue(store.flow.value.appLockEnabled)
        assertEquals(R.string.security_lock_on, state.message)
        assertFalse(state.busy)
        assertTrue(state.timeoutEnabled)
    }

    @Test
    fun cancellingEnableKeepsEverythingAsItWasWithoutAnError() = runTest(dispatcher) {
        start()
        vm.onLockToggle(true, launcher)
        manager.onAuthenticationCancelled(manager.state.value.authentication!!.id)
        runCurrent()
        assertFalse(state.appLockEnabled)
        assertFalse(store.flow.value.appLockEnabled)
        assertNull(state.error)
        assertNull(state.message)
        assertFalse(state.busy)
        assertEquals(0, store.writes)
    }

    @Test
    fun disablingRequiresANewAuthentication() = runTest(dispatcher) {
        start(SecuritySettings(appLockEnabled = true))
        assertTrue(state.appLockEnabled)
        vm.onLockToggle(false, launcher)
        runCurrent()
        assertEquals(AuthenticationPurpose.DISABLE_LOCK, launched.last())
        assertTrue(state.appLockEnabled) // continua ligado até o sucesso
        succeed()
        assertFalse(state.appLockEnabled)
        assertEquals(R.string.security_lock_off, state.message)
    }

    @Test
    fun cancellingDisableKeepsTheLockEnabled() = runTest(dispatcher) {
        start(SecuritySettings(appLockEnabled = true))
        vm.onLockToggle(false, launcher)
        manager.onAuthenticationCancelled(manager.state.value.authentication!!.id)
        runCurrent()
        assertTrue(state.appLockEnabled)
        assertTrue(store.flow.value.appLockEnabled)
        assertNull(state.error)
    }

    @Test
    fun authenticationErrorShowsAFriendlyMessageWithoutCodes() = runTest(dispatcher) {
        start()
        vm.onLockToggle(true, launcher)
        manager.onAuthenticationError(manager.state.value.authentication!!.id)
        runCurrent()
        assertFalse(state.appLockEnabled)
        assertEquals(R.string.security_error_auth, state.error)
    }

    @Test
    fun capabilityLostBeforeThePromptIsReportedAsGuidance() = runTest(dispatcher) {
        start()
        capability = AuthenticationCapability.NOT_CONFIGURED // removido no Android depois de abrir a tela
        vm.onLockToggle(true, launcher)
        runCurrent()
        assertTrue(launched.isEmpty())
        assertEquals(R.string.security_capability_not_configured, state.error)
        assertFalse(state.appLockEnabled)
    }

    @Test
    fun failedPersistenceAfterSuccessDoesNotEnable() = runTest(dispatcher) {
        start()
        vm.onLockToggle(true, launcher)
        store.failWrites = true
        succeed()
        assertFalse(state.appLockEnabled)
        assertEquals(R.string.security_error_save, state.error)
    }

    @Test
    fun timeoutSelectionIsSavedOnlyWhileTheLockIsActive() = runTest(dispatcher) {
        start()
        vm.onTimeoutSelected(AutoLockTimeout.AFTER_5_MINUTES)
        runCurrent()
        assertEquals(AutoLockTimeout.IMMEDIATELY, store.flow.value.autoLockTimeout) // inativo: ignorado

        start(SecuritySettings(appLockEnabled = true))
        vm.onTimeoutSelected(AutoLockTimeout.AFTER_5_MINUTES)
        runCurrent()
        assertEquals(AutoLockTimeout.AFTER_5_MINUTES, state.timeout)
        assertEquals(AutoLockTimeout.AFTER_5_MINUTES, store.flow.value.autoLockTimeout)
        assertEquals(R.string.security_timeout_saved, state.message)
    }

    @Test
    fun disablingTheLockKeepsTheChosenTimeout() = runTest(dispatcher) {
        start(SecuritySettings(appLockEnabled = true, autoLockTimeout = AutoLockTimeout.AFTER_15_MINUTES))
        vm.onLockToggle(false, launcher)
        succeed()
        assertFalse(state.appLockEnabled)
        assertEquals(AutoLockTimeout.AFTER_15_MINUTES, state.timeout)
        assertFalse(state.timeoutEnabled)
    }

    @Test
    fun screenProtectionIsIndependentOfTheLock() = runTest(dispatcher) {
        start()
        vm.onScreenProtectionToggle(true)
        runCurrent()
        assertTrue(state.protectScreen)
        assertTrue(store.flow.value.protectScreenContent)
        assertFalse(store.flow.value.appLockEnabled)
        assertEquals(R.string.security_screen_on, state.message)
        vm.onScreenProtectionToggle(false)
        runCurrent()
        assertFalse(state.protectScreen)
        assertEquals(R.string.security_screen_off, state.message)
        assertTrue(launched.isEmpty()) // não pede autenticação
    }

    @Test
    fun screenProtectionWriteFailureIsAnnounced() = runTest(dispatcher) {
        start()
        store.failWrites = true
        vm.onScreenProtectionToggle(true)
        runCurrent()
        assertFalse(state.protectScreen)
        assertEquals(R.string.security_error_save, state.error)
    }

    @Test
    fun repeatedTogglesWhileAPromptIsOpenStartASingleRequest() = runTest(dispatcher) {
        start()
        vm.onLockToggle(true, launcher)
        vm.onLockToggle(true, launcher)
        vm.onScreenProtectionToggle(true)
        runCurrent()
        assertEquals(1, launched.size)
        assertFalse(state.protectScreen)
    }

    @Test
    fun staleUnlockErrorFromTheLockScreenIsNotShownInSettings() = runTest(dispatcher) {
        start()
        // Erro anterior, não iniciado por esta tela.
        manager.requestAuthentication(AuthenticationPurpose.ENABLE_LOCK)!!.also { manager.onAuthenticationError(it.id) }
        vm = SecuritySettingsViewModel(ManagerSecurityControls(manager))
        runCurrent()
        assertNull(state.error)
        assertEquals(SecurityError.AUTHENTICATION_ERROR, manager.state.value.error)
    }

    @Test
    fun dismissClearsFeedback() = runTest(dispatcher) {
        start()
        vm.onScreenProtectionToggle(true)
        runCurrent()
        vm.dismissMessage()
        runCurrent()
        assertNull(state.message)
    }

    @Test
    fun statusOtherThanLoadedKeepsControlsDisabled() = runTest(dispatcher) {
        store = MemoryStore()
        val failing = object : SecuritySettingsStore {
            override val securitySettings = kotlinx.coroutines.flow.flow<SecuritySettings> { error("read") }
            override suspend fun writeSecuritySettings(settings: SecuritySettings) = Unit
        }
        manager = LocalSecurityManager(failing, { capability }, { now }, backgroundScope)
        vm = SecuritySettingsViewModel(ManagerSecurityControls(manager))
        runCurrent()
        assertEquals(AppLockStatus.INITIALIZATION_FAILED, manager.state.value.status)
        assertFalse(state.loaded)
        assertFalse(state.lockToggleEnabled)
        assertFalse(state.screenToggleEnabled)
    }
}
