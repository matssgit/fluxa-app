package com.fluxa.personal.security

import android.view.WindowManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.ui.SecurityGateTestHook
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sprint 9-B: barreira final, tela Segurança e FLAG_SECURE sobre o MainActivity real e o LocalSecurityManager real.
 * O prompt do Android é conduzido pelos callbacks do manager (mesmo caminho do host), exceto nos casos marcados como
 * credencial real, que só rodam quando o AVD tem um bloqueio de tela configurado.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SecurityUiFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private var scenario: ActivityScenario<MainActivity>? = null
    private lateinit var goalId: UUID

    @Before fun seed() {
        SecurityGateTestHook.autoPrompt = false
        runBlocking {
            app.preferences.writeSecuritySettings(SecuritySettings())
            app.preferences.apply(PortableSettingsV1("LIGHT", false))
            app.database.clearAllTables()
            val account = app.accounts.create(AccountDetails("Conta sigilosa", AccountType.CHECKING, Money(987_654), LocalDate.of(2020, 1, 1)))
            goalId = (app.goals.createGoal(GoalDetails("Meta sigilosa", Money(500_000), account)) as com.fluxa.personal.domain.model.GoalResult.Saved).value.id
        }
        waitForPolicy(SecuritySettings())
        app.security.onForeground()
    }

    @After fun reset() {
        if (app.security.state.value.authentication != null) shell("input keyevent 4")
        scenario?.close()
        SecurityGateTestHook.autoPrompt = true
        runBlocking { app.preferences.writeSecuritySettings(SecuritySettings()) }
        waitForPolicy(SecuritySettings())
        app.security.onForeground()
    }

    private fun waitForPolicy(settings: SecuritySettings) = compose.waitUntil(10_000) { app.security.state.value.settings == settings }
    private fun policy(settings: SecuritySettings) {
        runBlocking { app.preferences.writeSecuritySettings(settings) } // fixture; a UI usa só o manager
        waitForPolicy(settings)
    }
    private fun launch() { scenario = ActivityScenario.launch(MainActivity::class.java) }
    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun waitForTag(tag: String, timeout: Long = 15_000) = compose.waitUntil(timeout) { exists(tag) }
    private fun hasAnyText(text: String) =
        compose.onAllNodes(hasText(text, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

    /** Nenhum nó financeiro na árvore semântica (o que o TalkBack leria), não só invisível. */
    private fun assertNoFinancialContent() {
        for (tag in listOf("security-content", "list-dashboard", "safe-to-spend-card", "list-ledger", "list-cards", "list-goals", "screen-goal-detail")) {
            assertFalse("$tag presente atrás da barreira", exists(tag))
        }
        for (text in listOf("Conta sigilosa", "Meta sigilosa", "9.876,54", "R$")) assertFalse("'$text' acessível", hasAnyText(text))
    }

    private fun unlockByCallback() {
        val request = requireNotNull(app.security.requestAuthentication(AuthenticationPurpose.UNLOCK))
        assertTrue(runBlocking { app.security.onAuthenticationSucceeded(request.id) })
        waitForTag("security-content")
    }

    private fun tap(tag: String) {
        compose.waitForIdle()
        val node = compose.onNodeWithTag(tag)
        val target = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
        else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        target.performSemanticsAction(SemanticsActions.OnClick)
    }

    private fun openSecurity() {
        waitForTag("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        waitForTag("more-security")
        tap("more-security")
        waitForTag("screen-security")
    }

    private fun scrollTo(tag: String) {
        compose.waitForIdle()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(tag))
    }

    private fun windowSecure(): Boolean {
        var secure = false
        scenario!!.onActivity { secure = it.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0 }
        return secure
    }

    private fun requireCredential(): String {
        val credential = InstrumentationRegistry.getArguments().getString("deviceCredential")
        assumeTrue("Requer PIN do AVD (-e deviceCredential)", !credential.isNullOrBlank())
        assumeTrue(AndroidAuthenticationCapabilities(app).check() == AuthenticationCapability.AVAILABLE)
        require(credential!!.all(Char::isDigit))
        return credential
    }

    private fun systemOwnsWindow(): Boolean {
        val pkg = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.packageName?.toString()
        return pkg != null && pkg != app.packageName
    }

    private fun enterCredentialInSystemPrompt(credential: String) {
        compose.waitUntil(10_000) {
            fun hasPassword(node: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
                if (node == null) return false
                if (node.isPassword && node.isEditable) return true
                return (0 until node.childCount).any { hasPassword(node.getChild(it)) }
            }
            hasPassword(InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow)
        }
        shell("input text $credential")
        shell("input keyevent 66")
    }

    /** Cancela pelo Back real e espera o Android devolver o callback e fechar a janela do sistema. */
    private fun cancelSystemPrompt() {
        compose.waitUntil(10_000) { systemOwnsWindow() }
        shell("input keyevent 4")
        try {
            compose.waitUntil(2_000) { app.security.state.value.authentication == null }
        } catch (_: androidx.compose.ui.test.ComposeTimeoutException) {
            shell("input keyevent 4") // o primeiro Back pode só fechar o teclado numérico
            compose.waitUntil(10_000) { app.security.state.value.authentication == null }
        }
        waitForSystemPromptGone()
    }

    private fun waitForSystemPromptGone() = compose.waitUntil(10_000) { !systemOwnsWindow() }

    private fun shell(command: String): String = InstrumentationRegistry.getInstrumentation().uiAutomation
        .executeShellCommand(command).let { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        }

    // region Barreira

    @Test fun coldStartShowsTheFluxaBarrierAndNoFinancialSemantics() {
        policy(SecuritySettings(appLockEnabled = true))
        launch()
        waitForTag("security-gate")
        assertTrue(exists("security-wordmark"))
        assertTrue(hasAnyText("Seus dados estão protegidos."))
        assertTrue(exists("security-unlock"))
        assertNoFinancialContent()
    }

    @Test fun unlockShowsTheAppAndCancelKeepsTheBarrierWithAGentleHint() {
        policy(SecuritySettings(appLockEnabled = true))
        launch()
        waitForTag("security-gate")
        val request = requireNotNull(app.security.requestAuthentication(AuthenticationPurpose.UNLOCK))
        app.security.onAuthenticationCancelled(request.id)
        compose.waitUntil(5_000) { hasAnyText("Toque em Desbloquear quando quiser continuar.") }
        assertNoFinancialContent()
        unlockByCallback()
        waitForTag("list-dashboard")
        assertFalse(exists("security-gate"))
    }

    @Test fun authenticationErrorIsFriendlyWithoutAndroidCodes() {
        policy(SecuritySettings(appLockEnabled = true))
        launch()
        waitForTag("security-gate")
        val request = requireNotNull(app.security.requestAuthentication(AuthenticationPurpose.UNLOCK))
        app.security.onAuthenticationError(request.id)
        compose.waitUntil(5_000) { hasAnyText("Não foi possível confirmar sua identidade.") }
        assertNoFinancialContent()
    }

    @Test fun immediatePolicyLocksOnRealBackgroundAndRotationDoesNot() {
        policy(SecuritySettings(appLockEnabled = true))
        launch()
        unlockByCallback()
        scenario?.recreate() // rotação/config change: mesma sessão
        waitForTag("list-dashboard")
        assertEquals(AppLockStatus.UNLOCKED, app.security.state.value.status)
        scenario?.moveToState(Lifecycle.State.CREATED) // STOP real
        scenario?.moveToState(Lifecycle.State.RESUMED)
        waitForTag("security-gate")
        assertNoFinancialContent()
    }

    @Test fun unlockingReturnsToTheSameRoute() {
        policy(SecuritySettings(appLockEnabled = true))
        launch()
        unlockByCallback()
        openSecurity() // rota interna (detalhe fora das abas)
        scenario?.moveToState(Lifecycle.State.CREATED)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        waitForTag("security-gate")
        assertFalse(exists("screen-security"))
        unlockByCallback()
        waitForTag("screen-security")
        assertFalse(exists("list-dashboard"))
    }

    @Test fun continuousForegroundUseNeverShowsTheBarrier() {
        policy(SecuritySettings(appLockEnabled = true, autoLockTimeout = AutoLockTimeout.AFTER_1_MINUTE))
        launch()
        unlockByCallback()
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < 3_000) {
            compose.waitForIdle()
            assertFalse(exists("security-gate"))
            Thread.sleep(250)
        }
        assertEquals(AppLockStatus.UNLOCKED, app.security.state.value.status)
    }

    /** Tempo real (elapsedRealtime), sob demanda: `-e fluxaLongTimeout 1`. A fronteira exata 4m59/5m00 é JVM (9-A). */
    @Test fun oneMinutePolicyBeforeAndAfterTheLimit() {
        assumeTrue("Teste longo sob demanda", InstrumentationRegistry.getArguments().getString("fluxaLongTimeout") == "1")
        policy(SecuritySettings(appLockEnabled = true, autoLockTimeout = AutoLockTimeout.AFTER_1_MINUTE))
        launch()
        unlockByCallback()
        scenario?.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(50_000)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        waitForTag("security-content")
        assertEquals(AppLockStatus.UNLOCKED, app.security.state.value.status)
        scenario?.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(61_000)
        scenario?.moveToState(Lifecycle.State.RESUMED)
        waitForTag("security-gate")
        assertNoFinancialContent()
    }

    // endregion

    // region Configurações

    @Test fun securityScreenShowsCurrentChoicesAndInactiveTimeoutWhileUnlocked() {
        launch()
        waitForTag("list-dashboard")
        openSecurity()
        assertTrue(hasAnyText("Exigir autenticação para abrir o app"))
        scrollTo("security-timeout-IMMEDIATELY")
        compose.onNode(hasTestTag("security-timeout-AFTER_5_MINUTES") and isNotEnabled()).assertExists()
        assertTrue(hasAnyText("Disponível quando a autenticação para abrir o app estiver ativa."))
    }

    @Test fun capabilityWithoutScreenLockDisablesTheToggleWithGuidance() {
        val capability = AndroidAuthenticationCapabilities(app).check()
        assumeTrue("AVD sem bloqueio de tela", capability != AuthenticationCapability.AVAILABLE)
        launch()
        waitForTag("list-dashboard")
        openSecurity()
        compose.onNode(hasTestTag("security-lock-toggle") and isNotEnabled()).assertExists()
        waitForTag("security-capability")
        tap("security-lock-toggle")
        assertNull(app.security.state.value.authentication)
        assertFalse(app.security.state.value.settings!!.appLockEnabled)
    }

    @Test fun enablingAndDisablingAskForTheAndroidAuthentication() {
        val credential = requireCredential()
        launch()
        waitForTag("list-dashboard")
        openSecurity()
        compose.onNode(hasTestTag("security-lock-toggle") and isEnabled()).assertExists()
        tap("security-lock-toggle")
        compose.waitUntil(10_000) { app.security.state.value.authentication?.purpose == AuthenticationPurpose.ENABLE_LOCK }
        assertFalse(app.security.state.value.settings!!.appLockEnabled) // nada salvo antes do sucesso
        enterCredentialInSystemPrompt(credential) // PIN real no prompt do Android
        compose.waitUntil(15_000) { app.security.state.value.settings?.appLockEnabled == true }
        compose.waitUntil(10_000) { hasAnyText("Proteção ativada.") }
        waitForSystemPromptGone()
        tap("security-lock-toggle")
        compose.waitUntil(10_000) { app.security.state.value.authentication?.purpose == AuthenticationPurpose.DISABLE_LOCK }
        cancelSystemPrompt() // Back real: o Android entrega o cancelamento
        assertTrue(app.security.state.value.settings!!.appLockEnabled) // cancelar mantém ligado
        assertEquals(SecurityError.CANCELLED, app.security.state.value.error)
        waitForTag("screen-security")
    }

    @Test fun screenProtectionTogglesFlagSecureImmediatelyAndIndependently() {
        launch()
        waitForTag("list-dashboard")
        assertFalse(windowSecure()) // padrão: desligado
        openSecurity()
        scrollTo("security-screen-toggle")
        tap("security-screen-toggle")
        compose.waitUntil(5_000) { app.security.state.value.settings?.protectScreenContent == true }
        compose.waitUntil(5_000) { windowSecure() }
        assertFalse(app.security.state.value.settings!!.appLockEnabled)
        assertFalse(runBlocking { app.preferences.settings.first() }.hideAmounts)
        scenario?.recreate()
        compose.waitUntil(5_000) { windowSecure() } // reaplicado na nova janela
        waitForTag("screen-security")
        scrollTo("security-screen-toggle")
        tap("security-screen-toggle")
        compose.waitUntil(5_000) { app.security.state.value.settings?.protectScreenContent == false }
        compose.waitUntil(5_000) { !windowSecure() }
    }

    @Test fun autoPromptIsRequestedOnceAndCancelDoesNotLoop() {
        requireCredential()
        SecurityGateTestHook.autoPrompt = true
        policy(SecuritySettings(appLockEnabled = true))
        launch()
        compose.waitUntil(10_000) { app.security.state.value.authentication != null }
        cancelSystemPrompt()
        Thread.sleep(2_000)
        compose.waitForIdle()
        assertNull("não reabre sozinho depois de cancelar", app.security.state.value.authentication)
        waitForTag("security-lock-notice")
        scenario?.recreate() // rotação não reabre o prompt
        Thread.sleep(1_500)
        assertNull(app.security.state.value.authentication)
        assertNotNull(app.security.state.value.error)
        assertNoFinancialContent()
    }

    // endregion
}
