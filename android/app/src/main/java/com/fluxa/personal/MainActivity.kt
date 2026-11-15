package com.fluxa.personal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fluxa.personal.core.preferences.LocalSettings
import com.fluxa.personal.ui.FluxaNavigation
import com.fluxa.personal.ui.designsystem.FluxaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val fluxa = application as FluxaApplication
        setContent {
            val settings by fluxa.preferences.settings.collectAsState(LocalSettings())
            LaunchedEffect(fluxa) {
                withContext(Dispatchers.IO) {
                    fluxa.database.openHelper.writableDatabase
                    Unit
                }
            }
            FluxaTheme(themePreference = settings.theme, amountsHidden = settings.hideAmounts) {
                FluxaNavigation()
            }
        }
    }
}
