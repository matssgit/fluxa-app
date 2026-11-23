package com.fluxa.personal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import com.fluxa.personal.security.AuthenticationLauncher
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fluxa.personal.R
import com.fluxa.personal.security.AppLockState
import com.fluxa.personal.security.AppLockStatus
import com.fluxa.personal.security.AuthenticationCapability
import com.fluxa.personal.security.LocalSecurityManager
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.components.FluxaButton

internal val LocalAuthenticationLauncher = staticCompositionLocalOf<AuthenticationLauncher> {
    error("Authentication host is not installed")
}

/** Minimal functional barrier for 9-A. The final lock/settings presentation belongs to Sprint 9-B. */
@Composable
internal fun LocalSecurityGate(
    manager: LocalSecurityManager,
    onAuthenticate: () -> Unit,
    onRendered: (AppLockState) -> Unit,
    content: @Composable () -> Unit,
) {
    val state by manager.state.collectAsState()
    if (state.canShowContent) {
        Box(Modifier.fillMaxSize().testTag("security-content")) { content() }
    } else {
        FluxaTheme {
            Column(
                Modifier.fillMaxSize().background(FluxaTheme.colors.background).padding(24.dp).testTag("security-gate"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                when (state.status) {
                    AppLockStatus.INITIALIZING -> {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.security_initializing))
                    }
                    AppLockStatus.INITIALIZATION_FAILED -> {
                        Text(stringResource(R.string.security_settings_failed))
                        FluxaButton(text = stringResource(R.string.security_retry), onClick = manager::retryLoadingSettings)
                    }
                    else -> {
                        Text(stringResource(R.string.security_locked))
                        if (state.capability != AuthenticationCapability.AVAILABLE) {
                            Text(stringResource(if (state.capability == AuthenticationCapability.NOT_CONFIGURED)
                                R.string.security_not_configured else R.string.security_unavailable))
                        }
                        FluxaButton(
                            text = stringResource(R.string.security_unlock),
                            onClick = {
                                manager.refreshCapability()
                                onAuthenticate()
                            },
                            enabled = state.authentication == null && !state.isUpdatingSettings,
                            modifier = Modifier.testTag("security-unlock"),
                        )
                    }
                }
            }
        }
    }
    SideEffect { onRendered(state) }
}
