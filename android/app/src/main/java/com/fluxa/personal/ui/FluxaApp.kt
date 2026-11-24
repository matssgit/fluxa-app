package com.fluxa.personal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.fluxa.personal.FluxaApplication
import com.fluxa.personal.core.preferences.LocalSettings
import com.fluxa.personal.ui.designsystem.FluxaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Raiz do app. Tema e privacidade vêm do DataStore e mudam sem recriar a Activity (inclusive após uma restauração).
 * [shellGeneration] recria o shell de navegação inteiro depois de um restore: NavController, back stack,
 * ViewModels das telas começam do zero; tema e privacidade refletem as preferências restauradas.
 */
@Composable
internal fun FluxaApp(fluxa: FluxaApplication, initialSettings: LocalSettings) {
    val settings by fluxa.preferences.settings.collectAsState(initialSettings)
    LaunchedEffect(fluxa) {
        withContext(Dispatchers.IO) {
            fluxa.database.openHelper.writableDatabase
            Unit
        }
    }
    var shellGeneration by rememberSaveable { mutableIntStateOf(0) }
    FluxaTheme(themePreference = settings.theme, amountsHidden = settings.hideAmounts) {
        key(shellGeneration) {
            FluxaNavigation(theme = settings.theme, onRestartShell = { shellGeneration++ })
        }
    }
}
