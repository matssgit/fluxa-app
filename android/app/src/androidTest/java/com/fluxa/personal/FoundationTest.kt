package com.fluxa.personal

import android.content.pm.ApplicationInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fluxa.personal.core.preferences.LocalSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesAndNavigatesThroughAllPlaceholders() {
        compose.onNodeWithTag("screen-dashboard").assertIsDisplayed()
        for (route in listOf("cash", "cards", "more", "dashboard")) {
            compose.onNodeWithTag("tab-$route").performClick()
            compose.onNodeWithTag("screen-$route").assertIsDisplayed()
        }
    }

    @Test
    fun opensAndDismissesTheCentralActionSheetWithoutChangingDestination() {
        compose.onNodeWithTag("tab-cash").performClick()
        compose.onNodeWithTag("screen-cash").assertIsDisplayed()

        val description = compose.activity.getString(R.string.placeholder_sheet_body)
        val close = compose.activity.getString(R.string.action_close)
        compose.onNodeWithTag("action-new-entry").performClick()
        compose.onNodeWithText(description).assertIsDisplayed()

        compose.onNodeWithText(close).performClick()
        compose.onNodeWithText(description).assertDoesNotExist()
        compose.onNodeWithTag("screen-cash").assertIsDisplayed()
    }


    @Test
    fun opensRoomWithForeignKeysAndReadsPreferenceDefaults() = runBlocking(Dispatchers.IO) {
        val application = compose.activity.application as FluxaApplication
        val database = application.database.openHelper.writableDatabase

        assertEquals(2, database.version)
        database.query("PRAGMA foreign_keys").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        assertEquals(LocalSettings(), application.preferences.settings.first())
        assertEquals(0, application.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}
