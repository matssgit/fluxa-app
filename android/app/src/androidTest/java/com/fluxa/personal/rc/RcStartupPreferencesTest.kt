package com.fluxa.personal.rc

import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import com.fluxa.personal.R
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.security.SecuritySettings
import com.fluxa.personal.ui.SecurityGateTestHook
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class RcStartupPreferencesTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun prepare() {
        SecurityGateTestHook.autoPrompt = false
        runBlocking {
            app.preferences.writeSecuritySettings(SecuritySettings())
            app.preferences.apply(PortableSettingsV1("SYSTEM", false))
        }
        compose.waitUntil(10_000) { app.security.state.value.settings == SecuritySettings() }
        app.security.onForeground()
    }
    @After fun cleanup() {
        scenario?.close()
        SecurityGateTestHook.autoPrompt = true
        runBlocking {
            app.preferences.writeSecuritySettings(SecuritySettings())
            app.preferences.apply(PortableSettingsV1("SYSTEM", false))
        }
        app.security.onForeground()
    }
    private fun waitTag(tag: String) = compose.waitUntil(15_000) {
        compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        val clickable = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
            else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        clickable.performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test fun eyePersistsAndThemeChangesDoNotResetTheCurrentRoute() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitTag("tab-more")
        compose.onNodeWithTag("tab-more").performClick()
        waitTag("toggle-amounts")
        tap("toggle-amounts")
        compose.waitUntil(10_000) { runBlocking { app.preferences.settings.first().hideAmounts } }
        // Atomic storage also handles concurrent taps without dropping an inversion.
        runBlocking { coroutineScope { repeat(10) { launch { app.preferences.toggleHideAmounts() } } } }
        assertTrue(runBlocking { app.preferences.settings.first().hideAmounts })
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag("more-security"))
        tap("more-security")
        waitTag("screen-security")
        runBlocking { app.preferences.apply(PortableSettingsV1("DARK", true)) }
        compose.waitForIdle()
        waitTag("screen-security")
        scenario?.recreate()
        waitTag("screen-security")
        assertTrue(runBlocking { app.preferences.settings.first().hideAmounts })
        assertEquals("DARK", runBlocking { app.preferences.read().theme })
    }

    @Test fun storedDarkStartsWithADarkWindowAndNeverComposesFinancialContentWhileLocked() {
        runBlocking {
            app.preferences.apply(PortableSettingsV1("DARK", true))
            app.preferences.writeSecuritySettings(SecuritySettings(appLockEnabled = true))
        }
        compose.waitUntil(10_000) { app.security.state.value.settings?.appLockEnabled == true }
        repeat(5) { attempt ->
            val started = SystemClock.elapsedRealtime()
            scenario = ActivityScenario.launch(MainActivity::class.java)
            waitTag("security-gate")
            compose.onNodeWithTag("security-content").assertDoesNotExist()
            compose.onNodeWithTag("list-dashboard").assertDoesNotExist()
            scenario?.onActivity { activity ->
                val background = activity.window.decorView.background
                assertTrue(background is ColorDrawable)
                assertEquals(activity.getColor(R.color.fluxa_startup_background), (background as ColorDrawable).color)
            }
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            assertNotNull(screenshot)
            val color = requireNotNull(screenshot).getPixel(3, screenshot.height / 2)
            assertTrue("The dark gate background must not be white", android.graphics.Color.red(color) < 64 &&
                android.graphics.Color.green(color) < 64 && android.graphics.Color.blue(color) < 64)
            Log.i("FluxaRcStartup", "activity-start-$attempt ms=" + (SystemClock.elapsedRealtime() - started))
            scenario?.close()
            scenario = null
        }
    }
}
