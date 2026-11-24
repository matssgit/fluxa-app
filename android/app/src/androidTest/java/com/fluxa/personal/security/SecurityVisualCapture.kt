package com.fluxa.personal.security

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
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
import com.fluxa.personal.ui.SecurityGateTestHook
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Capturas da Sprint 9-B. Só roda com `-e fluxaCapture <sufixo>` (largura/tema/fonte configurados via adb).
 * A proteção de tela ligada não pode ser capturada: o próprio FLAG_SECURE bloqueia a captura.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SecurityVisualCapture {
    @get:Rule val compose = createEmptyComposeRule()
    private val suffix: String? = InstrumentationRegistry.getArguments().getString("fluxaCapture")
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private var scenario: ActivityScenario<MainActivity>? = null

    @After fun reset() {
        scenario?.close()
        SecurityGateTestHook.autoPrompt = true
        runBlocking { app.preferences.writeSecuritySettings(SecuritySettings()) }
    }

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    private fun wait(tag: String) = compose.waitUntil(15_000) { exists(tag) }
    private fun tap(tag: String) {
        compose.waitForIdle()
        val node = compose.onNodeWithTag(tag)
        val target = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
        else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        target.performSemanticsAction(SemanticsActions.OnClick)
    }
    private fun shot(name: String) {
        compose.waitForIdle()
        Thread.sleep(700)
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val dir = File(app.getExternalFilesDir(null), "captures").apply { mkdirs() }
        File(dir, "$name-$suffix.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun policy(settings: SecuritySettings) {
        runBlocking { app.preferences.writeSecuritySettings(settings) }
        compose.waitUntil(10_000) { app.security.state.value.settings == settings }
    }

    @Test fun captureLockAndSecuritySettings() {
        assumeTrue("Captura visual só roda sob demanda", suffix != null)
        SecurityGateTestHook.autoPrompt = false
        runBlocking {
            app.preferences.apply(PortableSettingsV1("SYSTEM", false))
            app.database.clearAllTables()
            app.accounts.create(AccountDetails("Conta principal", AccountType.CHECKING, Money(245_090), LocalDate.of(2026, 11, 1)))
        }
        policy(SecuritySettings(appLockEnabled = true))
        app.security.onForeground()
        scenario = ActivityScenario.launch(MainActivity::class.java)
        wait("security-unlock")
        shot("9b-bloqueio")
        val cancelled = requireNotNull(app.security.requestAuthentication(AuthenticationPurpose.UNLOCK))
        app.security.onAuthenticationCancelled(cancelled.id)
        wait("security-lock-notice")
        shot("9b-bloqueio-cancelado")
        val request = requireNotNull(app.security.requestAuthentication(AuthenticationPurpose.UNLOCK))
        runBlocking { app.security.onAuthenticationSucceeded(request.id) }
        wait("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        wait("more-security")
        shot("9b-mais")
        tap("more-security")
        wait("screen-security")
        shot("9b-seguranca-ligado")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("security-screen-card"))
        shot("9b-seguranca-ligado-fim")

        // Bloqueio desligado: tempo inativo, toggle livre.
        compose.onNode(hasScrollToNodeAction()).performScrollToIndex(0)
        policy(SecuritySettings())
        wait("security-lock-toggle")
        shot("9b-seguranca-desligado")
    }
}
