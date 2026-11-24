package com.fluxa.personal.rc

import android.os.Process
import android.os.SystemClock
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
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.GoalDetails
import com.fluxa.personal.domain.model.GoalResult
import com.fluxa.personal.domain.model.SafeToSpendState
import com.fluxa.personal.security.AppLockStatus
import com.fluxa.personal.security.AuthenticationPurpose
import com.fluxa.personal.security.SecuritySettings
import com.fluxa.personal.ui.SecurityGateTestHook
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * External protocol. Seed stays on the requested screen until the host force-stops the real PID.
 * Its termination is intentional; verification is a separate, normally finishing instrumentation run.
 */
@OptIn(ExperimentalTestApi::class)
class RcProcessDeathTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val app get() = ApplicationProvider.getApplicationContext<FluxaApplication>()
    private fun waitTag(tag: String) = compose.waitUntil(15_000) {
        compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }
    private fun tap(tag: String) {
        val node = compose.onNodeWithTag(tag)
        val target = if (SemanticsActions.OnClick in node.fetchSemanticsNode().config) node
            else compose.onNode(hasClickAction() and hasParent(hasTestTag(tag)))
        target.performSemanticsAction(SemanticsActions.OnClick)
    }
    private fun goTo(route: String) {
        waitTag("tab-more")
        when (route) {
            "dashboard", "lock" -> waitTag("list-dashboard")
            "cash" -> { compose.onNodeWithTag("tab-cash").performClick(); waitTag("list-cash") }
            "cards" -> { compose.onNodeWithTag("tab-cards").performClick(); waitTag("list-cards") }
            "safe" -> {
                waitTag("list-dashboard")
                compose.waitUntil(15_000) {
                    runCatching {
                        compose.onNodeWithTag("list-dashboard").performScrollToNode(hasTestTag("action-safe-details"))
                    }.isSuccess
                }
                tap("action-safe-details")
                waitTag("screen-safe-to-spend")
            }
            else -> {
                compose.onNodeWithTag("tab-more").performClick()
                val target = when (route) {
                    "subscriptions" -> "more-subscriptions"
                    "goals" -> "more-goals"
                    "backup" -> "more-backup"
                    else -> error("Unsupported fixture route")
                }
                compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(target))
                tap(target)
                waitTag(when (route) {
                    "subscriptions" -> "screen-subscriptions"
                    "goals" -> "screen-goals"
                    else -> "screen-backup"
                })
            }
        }
    }

    @Test fun processDeathFromEachExistingDestinationKeepsDataAndNeverRestoresAnUnlock() {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("rcPhase")
        val route = args.getString("rcRoute")
        assumeTrue("External seed -> force-stop -> verify protocol", phase == "seed" || phase == "verify")
        require(route in listOf("dashboard", "cash", "cards", "subscriptions", "goals", "safe", "backup", "lock"))
        SecurityGateTestHook.autoPrompt = false
        if (phase == "seed") {
            runBlocking {
                app.preferences.writeSecuritySettings(SecuritySettings())
                app.preferences.apply(PortableSettingsV1("DARK", true))
                app.database.clearAllTables()
                val account = app.accounts.create(AccountDetails(
                    "Conta RC", AccountType.CHECKING, Money(500_000), LocalDate.of(2020, 1, 1),
                ))
                val goal = (app.goals.createGoal(GoalDetails("Reserva RC", Money(150_000), account)) as GoalResult.Saved).value
                assertTrue(app.goals.allocateToGoal(goal.id, Money(150_000)) is GoalResult.Saved)
                val safe = (app.safeToSpend.observeSafeToSpend().first() as SafeToSpendState.Ready).result
                assertEquals(Money(350_000), safe.availableNow)
                assertEquals(Money(350_000), safe.safeAmount)
                if (route == "lock") app.preferences.writeSecuritySettings(SecuritySettings(true))
            }
            val expected = SecuritySettings(route == "lock")
            compose.waitUntil(10_000) { app.security.state.value.settings == expected }
            app.security.onForeground()
            if (route == "lock") {
                val auth = requireNotNull(app.security.requestAuthentication(AuthenticationPurpose.UNLOCK))
                assertTrue(runBlocking { app.security.onAuthenticationSucceeded(auth.id) })
            }
            val scenario = ActivityScenario.launch(MainActivity::class.java)
            try {
                goTo(requireNotNull(route))
                File(app.filesDir, "rc-process-ready").writeText(Process.myPid().toString() + "|" + route)
                // The external harness must kill this PID while the destination is still open.
                val deadline = SystemClock.elapsedRealtime() + 60_000
                while (SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
                fail("The external harness did not force-stop the waiting seed process")
            } finally { scenario.close() }
        } else {
            try {
                val settings = runBlocking { app.preferences.read() }
                assertEquals(PortableSettingsV1("DARK", true), settings)
                assertEquals("Conta RC", runBlocking { app.accounts.observeAll().first().single().details.name })
                val safe = runBlocking { (app.safeToSpend.observeSafeToSpend().first() as SafeToSpendState.Ready).result }
                assertEquals(Money(350_000), safe.availableNow)
                assertEquals(Money(350_000), safe.safeAmount)
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    if (route == "lock") {
                        compose.waitUntil(10_000) { app.security.state.value.status == AppLockStatus.LOCKED }
                        waitTag("security-gate")
                        assertEquals(AppLockStatus.LOCKED, app.security.state.value.status)
                        compose.onNodeWithTag("security-content").assertDoesNotExist()
                        compose.onNodeWithTag("list-dashboard").assertDoesNotExist()
                        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
                        assertFalse(app.security.state.value.canShowContent)
                    } else {
                        goTo(requireNotNull(route))
                    }
                }
                File(app.filesDir, "rc-process-verified").writeText(Process.myPid().toString() + "|" + route)
            } finally {
                SecurityGateTestHook.autoPrompt = true
                runBlocking { app.preferences.writeSecuritySettings(SecuritySettings()) }
            }
        }
    }
}
