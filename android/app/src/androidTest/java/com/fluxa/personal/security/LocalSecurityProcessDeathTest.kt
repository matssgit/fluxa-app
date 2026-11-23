package com.fluxa.personal.security

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** External two-phase protocol: seed -> adb force-stop -> verify, on the exclusive offline emulator. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class LocalSecurityProcessDeathTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun newProcessRestoresThePolicyButNeverAnUnlockedSession() {
        val phase = InstrumentationRegistry.getArguments().getString("localSecurityPhase")
        assumeTrue("Run seed and verify in separate processes with force-stop between them", phase == "seed" || phase == "verify")
        val app = ApplicationProvider.getApplicationContext<FluxaApplication>()
        if (phase == "seed") {
            runBlocking { app.preferences.writeSecuritySettings(SecuritySettings(true)) }
            compose.waitUntil(10_000) { app.security.state.value.status == AppLockStatus.LOCKED }
            app.security.onForeground()
            val request = requireNotNull(app.security.requestAuthentication(AuthenticationPurpose.UNLOCK))
            assertTrue(runBlocking { app.security.onAuthenticationSucceeded(request.id) })
            assertEquals(AppLockStatus.UNLOCKED, app.security.state.value.status)
        } else {
            compose.waitUntil(10_000) { app.security.state.value.settings != null }
            assertEquals(AppLockStatus.LOCKED, app.security.state.value.status)
            assertNull(app.security.state.value.authentication)
            ActivityScenario.launch(MainActivity::class.java).use {
                compose.waitUntil(10_000) {
                    compose.onAllNodes(hasTestTag("security-gate")).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("security-content").assertDoesNotExist()
                compose.onNodeWithTag("list-dashboard").assertDoesNotExist()
                assertTrue(app.security.state.value.needsAuthentication)
            }
            runBlocking { app.preferences.writeSecuritySettings(SecuritySettings()) }
        }
    }
}
