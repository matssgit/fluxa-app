package com.fluxa.personal.security

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.backup.BackupResult
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LocalSecurityActivityTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun seed() {
        // 9-B: a barreira pede a autenticação sozinha; estes casos conduzem o callback do manager diretamente.
        com.fluxa.personal.ui.SecurityGateTestHook.autoPrompt = false
        runBlocking {
            app.preferences.writeSecuritySettings(SecuritySettings())
            app.preferences.apply(PortableSettingsV1("LIGHT", false))
            app.database.clearAllTables()
            app.accounts.create(AccountDetails(
                "Dado financeiro protegido", AccountType.CHECKING, Money(123_456), LocalDate.of(2020, 1, 1),
            ))
        }
        waitForPolicy(SecuritySettings())
        app.security.onForeground()
    }

    @After fun reset() {
        com.fluxa.personal.ui.SecurityGateTestHook.autoPrompt = true
        if (app.security.state.value.authentication != null) shell("input keyevent 4")
        scenario?.close()
        runBlocking { app.preferences.writeSecuritySettings(SecuritySettings()) }
        waitForPolicy(SecuritySettings())
        app.security.onForeground()
    }

    private fun waitForPolicy(settings: SecuritySettings) = compose.waitUntil(10_000) {
        app.security.state.value.settings == settings
    }
    private fun enable(timeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATELY) {
        val settings = SecuritySettings(true, timeout)
        runBlocking { app.preferences.writeSecuritySettings(settings) } // fixture, not the production UI API
        waitForPolicy(settings)
    }
    private fun launch() { scenario = ActivityScenario.launch(MainActivity::class.java) }
    private fun waitForTag(tag: String) = compose.waitUntil(15_000) {
        compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    private fun assertNoFinancialContent() {
        for (tag in listOf("security-content", "list-dashboard", "safe-to-spend-card", "list-ledger", "list-cards", "list-goals")) {
            compose.onNodeWithTag(tag).assertDoesNotExist()
        }
        assertTrue(compose.onAllNodes(hasText("Dado financeiro protegido", substring = true), useUnmergedTree = true)
            .fetchSemanticsNodes().isEmpty())
    }
    private fun callbackSuccess() {
        val request = requireNotNull(app.security.requestAuthentication(AuthenticationPurpose.UNLOCK))
        assertTrue(runBlocking { app.security.onAuthenticationSucceeded(request.id) })
        waitForTag("security-content")
    }
    private fun shell(command: String): String = InstrumentationRegistry.getInstrumentation().uiAutomation
        .executeShellCommand(command).let { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        }

    @Test fun enabledStartupAndActivityRecreationNeverComposeSeededFinancialDataBeforeAuthentication() {
        enable()
        launch()
        waitForTag("security-gate")
        assertNoFinancialContent()
        scenario?.recreate()
        waitForTag("security-gate")
        assertNoFinancialContent()
        callbackSuccess()
        waitForTag("list-dashboard")
        assertTrue(app.security.state.value.canShowContent)
    }

    @Test fun cancelledAuthenticationKeepsGateAndFinancialDataAbsent() {
        enable()
        launch()
        waitForTag("security-gate")
        val request = requireNotNull(app.security.requestAuthentication(AuthenticationPurpose.UNLOCK))
        app.security.onAuthenticationCancelled(request.id)
        assertNoFinancialContent()
        assertFalse(app.security.state.value.canShowContent)
    }

    @Test fun realActivityStopAndStartRequireAuthenticationWithImmediatePolicy() {
        enable()
        launch()
        callbackSuccess()
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        waitForTag("security-gate")
        assertNoFinancialContent()
    }

    @Test fun disabledPolicyAndConfigurationRecreationKeepNormalNavigation() {
        launch()
        waitForTag("list-dashboard")
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        waitForTag("security-content")
        scenario?.recreate()
        waitForTag("list-dashboard")
        assertEquals(AppLockStatus.DISABLED, app.security.state.value.status)
    }

    @Test fun rotatingAnUnlockedActivityDoesNotActAsARealBackgroundTransition() {
        enable()
        launch()
        callbackSuccess()
        scenario?.recreate()
        waitForTag("security-content")
        assertEquals(AppLockStatus.UNLOCKED, app.security.state.value.status)
    }

    @Test fun realSafPickerCancellationDoesNotLockOrAbandonTheBackupFlow() {
        enable()
        launch()
        callbackSuccess()
        waitForTag("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        waitForTag("more-backup")
        compose.onNodeWithTag("more-backup").performClick()
        waitForTag("action-backup-restore")
        compose.onNodeWithTag("action-backup-restore").performClick()
        compose.waitUntil(10_000) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.packageName?.toString()
                ?.contains("documentsui") == true
        }
        assertEquals(AppLockStatus.UNLOCKED, app.security.state.value.status)
        shell("input keyevent 4")
        waitForTag("action-backup-restore")
        assertEquals(AppLockStatus.UNLOCKED, app.security.state.value.status)
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        waitForTag("security-gate")
        assertNoFinancialContent()
    }

    @Test fun realDeviceCredentialPromptCanBeCancelledWithoutOpeningContent() {
        assumeTrue(AndroidAuthenticationCapabilities(app).check() == AuthenticationCapability.AVAILABLE)
        enable()
        launch()
        waitForTag("security-unlock")
        compose.onNodeWithTag("security-unlock").performClick()
        compose.waitUntil(10_000) { app.security.state.value.authentication != null }
        // Wait until Android, rather than Fluxa, owns the credential window.
        compose.waitUntil(10_000) {
            val packageName = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.packageName?.toString()
            packageName != null && packageName != app.packageName
        }
        shell("input keyevent 4")
        try {
            compose.waitUntil(2_000) { app.security.state.value.authentication == null }
        } catch (_: androidx.compose.ui.test.ComposeTimeoutException) {
            // On the credential Activity the first Back can dismiss only the numeric keyboard.
            shell("input keyevent 4")
            compose.waitUntil(10_000) { app.security.state.value.authentication == null }
        }
        waitForTag("security-gate")
        assertNoFinancialContent()
    }

    @Test fun credentialOnlyDeviceAuthenticatesThroughTheRealAndroidxPrompt() {
        val credential = InstrumentationRegistry.getArguments().getString("deviceCredential")
        assumeTrue("Provide an emulator-only device credential fixture", !credential.isNullOrBlank())
        assertEquals(AuthenticationCapability.AVAILABLE, AndroidAuthenticationCapabilities(app).check())
        assertEquals(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            ALLOWED_AUTHENTICATORS,
        )
        enable()
        launch()
        waitForTag("security-unlock")
        compose.onNodeWithTag("security-unlock").performClick()
        compose.waitUntil(10_000) {
            val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow
            fun hasPassword(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
                if (node == null) return false
                if (node.isPassword && node.isEditable) return true
                return (0 until node.childCount).any { hasPassword(node.getChild(it)) }
            }
            hasPassword(root)
        }
        requireNotNull(credential)
        require(credential.all(Char::isDigit)) // no arbitrary shell input; fixture is the emulator's Android PIN
        shell("input text $credential")
        shell("input keyevent 66")
        compose.waitUntil(15_000) { app.security.state.value.status == AppLockStatus.UNLOCKED }
        waitForTag("list-dashboard")
    }

    @Test fun encryptedPortableRestorePreservesAllDeviceLocalSecurityPreferences() {
        fun password() = "senha de teste do backup local".toCharArray()
        val bytes = ByteArrayOutputStream()
        assertTrue(runBlocking { app.backups.export(password(), bytes) } is BackupResult.Success)
        val prepared = runBlocking { app.backups.validate(password(), ByteArrayInputStream(bytes.toByteArray())) }
            as BackupResult.Success
        val devicePolicy = SecuritySettings(true, AutoLockTimeout.AFTER_15_MINUTES, true)
        runBlocking {
            app.preferences.writeSecuritySettings(devicePolicy)
            app.preferences.apply(PortableSettingsV1("DARK", true))
        }
        waitForPolicy(devicePolicy)
        val restored = runBlocking { app.backups.restore(prepared.value, password(), ByteArrayOutputStream()) }
        assertTrue(restored is BackupResult.Success)
        assertEquals(devicePolicy, runBlocking { app.preferences.securitySettings.first() })
        assertEquals(PortableSettingsV1("LIGHT", false), runBlocking { app.preferences.read() })
        assertEquals(AppLockStatus.LOCKED, app.security.state.value.status)
    }
}
