package com.fluxa.personal.ui.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fluxa.personal.R
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.LocalAmountsHidden
import com.fluxa.personal.ui.designsystem.components.FluxaCard
import com.fluxa.personal.ui.designsystem.components.FluxaIconButton
import com.fluxa.personal.ui.designsystem.components.FluxaNavigationBarDefaults
import com.fluxa.personal.ui.designsystem.components.FluxaTopBar

/*
 * Superfícies mínimas para validar o design system no shell. Não são telas definitivas
 * e não usam dados reais: os valores exibidos são fictícios.
 */

@Composable
private fun PlaceholderScreen(
    route: String,
    title: String,
    onToggleAmounts: () -> Unit,
    overline: String? = null,
    content: LazyListScope.() -> Unit,
) {
    val hidden = LocalAmountsHidden.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FluxaTheme.colors.background)
            .testTag("screen-$route"),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = FluxaNavigationBarDefaults.ContentClearance),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                FluxaTopBar(title = title, overline = overline) {
                    FluxaIconButton(
                        icon = if (hidden) FluxaIcons.EyeOff else FluxaIcons.Eye,
                        contentDescription = stringResource(if (hidden) R.string.action_show_amounts else R.string.action_hide_amounts),
                        onClick = onToggleAmounts,
                        modifier = Modifier.testTag("toggle-amounts"),
                    )
                }
            }
            content()
        }
    }
}

private val Gutter = Modifier.fillMaxWidth().padding(horizontal = 16.dp)

private data class MoreDestination(val label: Int, val icon: ImageVector, val route: String? = null)

@Composable
internal fun MorePlaceholder(
    onToggleAmounts: () -> Unit,
    onOpenCategories: () -> Unit = {},
    onOpenSubscriptions: () -> Unit = {},
    onOpenGoals: () -> Unit = {},
    onOpenBackup: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
) {
    val destinations = listOf(
        MoreDestination(R.string.more_subscriptions, FluxaIcons.Repeat, route = "subscriptions"),
        MoreDestination(R.string.more_goals, FluxaIcons.Target, route = "goals"),
        MoreDestination(R.string.more_categories, FluxaIcons.Tag, route = "categories"),
        MoreDestination(R.string.more_backup, FluxaIcons.Download, route = "backup"),
        MoreDestination(R.string.more_security, FluxaIcons.Lock, route = "security"),
        MoreDestination(R.string.more_analytics, FluxaIcons.Analytics),
        MoreDestination(R.string.more_settings, FluxaIcons.Sliders),
    )
    PlaceholderScreen(
        route = "more",
        title = stringResource(R.string.more),
        overline = stringResource(R.string.placeholder_more_overline),
        onToggleAmounts = onToggleAmounts,
    ) {
        item {
            FluxaCard(modifier = Gutter, contentPadding = PaddingValues(vertical = 4.dp)) {
                destinations.forEachIndexed { index, destination ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(start = 72.dp)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(FluxaTheme.colors.border),
                        )
                    }
                    MoreRow(
                        label = stringResource(destination.label),
                        icon = destination.icon,
                        onClick = when (destination.route) {
                            "categories" -> onOpenCategories
                            "subscriptions" -> onOpenSubscriptions
                            "goals" -> onOpenGoals
                            "backup" -> onOpenBackup
                            "security" -> onOpenSecurity
                            else -> null
                        },
                        modifier = if (destination.route != null) Modifier.testTag("more-${destination.route}") else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreRow(label: String, icon: ImageVector, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = FluxaTheme.colors
    val soon = stringResource(R.string.coming_soon)
    val narrow = com.fluxa.personal.ui.common.isNarrowForText()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(colors.brandTonal, FluxaTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.onBrandTonal, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = FluxaTheme.typography.bodyStrong, color = colors.textPrimary)
            // Com fonte ampliada o selo vai para baixo do rótulo (ao lado, "Configurações" quebrava no meio).
            if (onClick == null && narrow) SoonTag(soon, Modifier.padding(top = 4.dp))
        }
        if (onClick != null) {
            Icon(FluxaIcons.ChevronRight, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
        } else if (!narrow) {
            SoonTag(soon)
        }
    }
}

@Composable
private fun SoonTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = FluxaTheme.typography.caption,
        color = FluxaTheme.colors.textMuted,
        modifier = modifier
            .background(FluxaTheme.colors.surfaceSunken, FluxaTheme.shapes.pill)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
