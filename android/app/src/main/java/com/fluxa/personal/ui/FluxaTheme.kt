package com.fluxa.personal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.fluxa.personal.core.preferences.ThemePreference

private val LightColors = lightColorScheme(
    primary = Color(0xFF17554C),
    onPrimary = Color.White,
    secondary = Color(0xFF53654D),
    tertiary = Color(0xFF884631),
    background = Color(0xFFF9F5EA),
    surface = Color(0xFFF9F5EA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA0D4C6),
    secondary = Color(0xFFB8CCB0),
    tertiary = Color(0xFFF1B29A),
    background = Color(0xFF141D19),
    surface = Color(0xFF141D19),
)

@Composable
internal fun FluxaTheme(theme: ThemePreference, content: @Composable () -> Unit) {
    val dark = when (theme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
