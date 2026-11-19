package com.fluxa.personal.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import com.fluxa.personal.backup.BackupSettingsStore
import com.fluxa.personal.backup.PortableSettingsV1
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

internal class LocalPreferences(context: Context) : BackupSettingsStore {
    private val store = context.applicationContext.fluxaPreferences

    val settings: Flow<LocalSettings> = store.data.map { values ->
        LocalSettings(
            theme = ThemePreference.entries.firstOrNull { it.name == values[THEME] }
                ?: ThemePreference.SYSTEM,
            hideAmounts = values[HIDE_AMOUNTS] ?: false,
        )
    }

    override suspend fun read(): PortableSettingsV1 {
        val current = settings.first()
        return PortableSettingsV1(current.theme.name, current.hideAmounts)
    }

    override suspend fun apply(settings: PortableSettingsV1) {
        val theme = ThemePreference.valueOf(settings.theme)
        store.edit {
            it[THEME] = theme.name
            it[HIDE_AMOUNTS] = settings.hideAmounts
        }
    }

    companion object {
        private val THEME = stringPreferencesKey("theme")
        private val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
    }
}
