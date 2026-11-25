package com.fluxa.personal.ui

import android.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.security.SecuritySettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** v1.0.1: o usuário escolhe o tema em Mais → Aparência e a escolha sobrevive a recriar e a fechar o app. */
@RunWith(AndroidJUnit4::class)
class AppearanceFlowTest {
    @get:Rule
    val compose = createEmptyComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private val args get() = InstrumentationRegistry.getArguments()
    private val phase: String? get() = args.getString("themePhase")

    @Before
    fun prepare() = runBlocking {
        if (phase == "verify") return@runBlocking
        app.preferences.writeSecuritySettings(SecuritySettings())
        app.preferences.apply(PortableSettingsV1("SYSTEM", false))
    }

    @After
    fun reset() = runBlocking { if (phase != "seed") app.preferences.apply(PortableSettingsV1("SYSTEM", false)) }

    private fun waitTag(tag: String) = compose.waitUntil(10_000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }

    private fun openMore() {
        waitTag("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        waitTag("screen-more")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("more-appearance"))
    }

    private fun choose(theme: String) {
        // Clique semântico: em 320dp o cartão pode parar sob a navegação flutuante e o toque por coordenada cairia nela.
        compose.onNodeWithTag("theme-$theme").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitUntil(10_000) { runBlocking { app.preferences.read().theme } == theme }
        compose.waitForIdle()
    }

    /** Luminância do fundo na margem da tela. */
    private fun backgroundIsDark(): Boolean {
        Thread.sleep(500)
        val shot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val p = shot.getPixel(3, shot.height / 2)
        return (Color.red(p) + Color.green(p) + Color.blue(p)) / 3 < 96
    }

    @Test
    fun userSwitchesBetweenLightDarkAndAutomatic() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openMore()
            compose.onNodeWithTag("theme-SYSTEM").assertIsOn()
            choose("DARK")
            compose.onNodeWithTag("theme-DARK").assertIsOn()
            compose.onNodeWithTag("theme-LIGHT").assertIsOff()
            assertTrue(backgroundIsDark())
            choose("LIGHT")
            assertTrue(!backgroundIsDark())
            scenario.recreate()
            waitTag("screen-more") // trocar de tema e recriar não tira o usuário do lugar
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("more-appearance"))
            compose.onNodeWithTag("theme-LIGHT").assertIsOn()
            choose("SYSTEM")
        }
    }

    @Test
    fun choosingAThemeChangesNothingElse() {
        val security = SecuritySettings(protectScreenContent = false)
        runBlocking { app.preferences.apply(PortableSettingsV1("SYSTEM", true)) }
        ActivityScenario.launch(MainActivity::class.java).use {
            openMore()
            choose("DARK")
        }
        assertEquals(PortableSettingsV1("DARK", true), runBlocking { app.preferences.read() })
        assertEquals(security, runBlocking { app.preferences.securitySettings.first() })
    }

    /** Protocolo externo: `-e themePhase seed -e themeName LIGHT|DARK`, `am force-stop`, `-e themePhase verify`. */
    @Test
    fun themeSurvivesForceStop() {
        assumeTrue("Run seed and verify in separate processes", phase == "seed" || phase == "verify")
        val theme = requireNotNull(args.getString("themeName"))
        ActivityScenario.launch(MainActivity::class.java).use {
            openMore()
            if (phase == "seed") choose(theme)
            assertEquals(theme, runBlocking { app.preferences.read().theme })
            compose.onNodeWithTag("theme-$theme").assertIsOn()
            assertEquals(theme == "DARK", backgroundIsDark())
        }
    }
}
