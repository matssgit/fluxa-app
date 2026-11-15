package com.fluxa.personal.ui.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Raios do sistema. Componentes não definem raios próprios. */
internal object FluxaRadius {
    val Small = 12.dp
    val Medium = 16.dp
    val Large = 24.dp
    val ExtraLarge = 32.dp
    val LeafTight = 12.dp
}

/**
 * Conjunto fechado de formas.
 *
 * `leaf` é a assinatura orgânica do Fluxa (diagonal assimétrica). Uso restrito a destaques:
 * hero de saldo, cartão em foco, meta em foco. Nunca em listas ou controles.
 */
@Immutable
internal data class FluxaShapes(
    val small: Shape = RoundedCornerShape(FluxaRadius.Small),
    val medium: Shape = RoundedCornerShape(FluxaRadius.Medium),
    val large: Shape = RoundedCornerShape(FluxaRadius.Large),
    val extraLarge: Shape = RoundedCornerShape(FluxaRadius.ExtraLarge),
    val pill: Shape = CircleShape,
    val leaf: Shape = RoundedCornerShape(
        topStart = FluxaRadius.ExtraLarge,
        topEnd = FluxaRadius.LeafTight,
        bottomEnd = FluxaRadius.ExtraLarge,
        bottomStart = FluxaRadius.LeafTight,
    ),
    /** Topo de bottom sheet. */
    val sheet: Shape = RoundedCornerShape(topStart = FluxaRadius.ExtraLarge, topEnd = FluxaRadius.ExtraLarge),
)

internal val MaterialFluxaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(FluxaRadius.Small),
    medium = RoundedCornerShape(FluxaRadius.Medium),
    large = RoundedCornerShape(FluxaRadius.Large),
    extraLarge = RoundedCornerShape(FluxaRadius.ExtraLarge),
)
