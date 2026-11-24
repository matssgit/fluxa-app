package com.fluxa.personal.ui.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fluxa.personal.ui.designsystem.FluxaIcons
import com.fluxa.personal.ui.designsystem.FluxaTheme
import com.fluxa.personal.ui.designsystem.fluxaInnerShade

/**
 * Campo de texto. O rótulo fica sempre visível acima do campo e faz parte do nó de acessibilidade.
 * Foco: anel de 2dp na cor da marca. Erro: borda, ícone e texto (não depende só de cor).
 */
@Composable
internal fun FluxaInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = FluxaTheme.colors
    val type = FluxaTheme.typography
    val shape = FluxaTheme.shapes.medium
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val isError = errorText != null

    val borderWidth = if (focused || isError) 2.dp else 1.dp
    val borderColor = when {
        isError -> colors.expense
        focused -> colors.brand
        else -> colors.borderStrong
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (errorText != null) error(errorText) },
        enabled = enabled,
        singleLine = singleLine,
        textStyle = type.body.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.brand),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        interactionSource = interaction,
        decorationBox = { innerField ->
            Column(
                modifier = Modifier.alpha(if (enabled) 1f else 0.5f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(label, style = type.label, color = colors.textPrimary)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(colors.surfaceSunken, shape)
                        .fluxaInnerShade(shape)
                        .border(borderWidth, borderColor, shape)
                        // Com ícone clicável (48dp) à direita o campo continua com 56dp, igual aos demais.
                        .padding(horizontal = 16.dp, vertical = if (trailing != null) 4.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = if (focused) colors.brand else colors.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = type.body, color = colors.textMuted)
                        }
                        innerField()
                    }
                    trailing?.invoke()
                }
                when {
                    errorText != null -> Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(FluxaIcons.Alert, contentDescription = null, tint = colors.expense, modifier = Modifier.size(16.dp))
                        Text(errorText, style = type.caption, color = colors.expense)
                    }
                    supportingText != null -> Text(supportingText, style = type.caption, color = colors.textMuted)
                }
            }
        },
    )
}
