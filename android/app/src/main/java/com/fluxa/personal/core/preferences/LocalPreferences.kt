package com.fluxa.personal.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fluxaPreferences by preferencesDataStore("fluxa_settings")

internal enum class ThemePreference { SYSTEM, LIGHT, DARK }

internal data class LocalSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val hideAmounts: Boolean = false,
)

internal class LocalPreferences(context: Context) {
    private val store = context.applicationContext.fluxaPreferences

    val settings: Flow<LocalSettings> = store.data.map { values ->
        LocalSettings(
            theme = ThemePreference.entries.firstOrNull { it.name == values[THEME] }
                ?: ThemePreference.SYSTEM,
            hideAmounts = values[HIDE_AMOUNTS] ?: false,
        )
    }

    companion object {
        private val THEME = stringPreferencesKey("theme")
        private val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
    }
}
