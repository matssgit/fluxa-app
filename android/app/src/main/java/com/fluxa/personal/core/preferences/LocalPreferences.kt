package com.fluxa.personal.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fluxa.personal.backup.BackupSettingsStore
import com.fluxa.personal.backup.PortableSettingsV1
import com.fluxa.personal.security.AutoLockTimeout
import com.fluxa.personal.security.SecuritySettings
import com.fluxa.personal.security.SecuritySettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.fluxaPreferences by preferencesDataStore("fluxa_settings")

internal enum class ThemePreference { SYSTEM, LIGHT, DARK }

internal data class LocalSettings(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val hideAmounts: Boolean = false,
)

internal class LocalPreferences(context: Context) : BackupSettingsStore, SecuritySettingsStore {
    private val store = context.applicationContext.fluxaPreferences

    val settings: Flow<LocalSettings> = store.data.map { values ->
        LocalSettings(
            theme = ThemePreference.entries.firstOrNull { it.name == values[THEME] } ?: ThemePreference.SYSTEM,
            hideAmounts = values[HIDE_AMOUNTS] ?: false,
        )
    }

    override val securitySettings: Flow<SecuritySettings> = store.data.map { values ->
        SecuritySettings(
            appLockEnabled = values[APP_LOCK_ENABLED] ?: false,
            autoLockTimeout = AutoLockTimeout.entries.firstOrNull { it.name == values[AUTO_LOCK_TIMEOUT] }
                ?: AutoLockTimeout.IMMEDIATELY,
            protectScreenContent = values[PROTECT_SCREEN_CONTENT] ?: false,
        )
    }

    /** Storage port only: frontend requests authenticated changes through LocalSecurityManager. */
    override suspend fun writeSecuritySettings(settings: SecuritySettings) {
        store.edit {
            it[APP_LOCK_ENABLED] = settings.appLockEnabled
            it[AUTO_LOCK_TIMEOUT] = settings.autoLockTimeout.name
            it[PROTECT_SCREEN_CONTENT] = settings.protectScreenContent
        }
    }

    // Portable v1 deliberately excludes device-local security preferences.
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
        private val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val AUTO_LOCK_TIMEOUT = stringPreferencesKey("auto_lock_timeout")
        private val PROTECT_SCREEN_CONTENT = booleanPreferencesKey("protect_screen_content")
        private val THEME = stringPreferencesKey("theme")
        private val HIDE_AMOUNTS = booleanPreferencesKey("hide_amounts")
    }
}
