package com.fluxa.personal.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Iconografia própria e mínima: grade 24, traço 1.75 com pontas arredondadas, poucos detalhes.
 * Estado ativo = versão sólida (formas com área) ou traço reforçado (ícones lineares).
 * Evita dependência de material-icons-extended (~MBs).
 */
internal object FluxaIcons {
    val Home: ImageVector by lazy { outline("Home") { house() } }
    val HomeFilled: ImageVector by lazy { filled("HomeFilled") { house() } }

    val Cash: ImageVector by lazy { outline("Cash") { swapArrows() } }
    val CashFilled: ImageVector by lazy { outline("CashFilled", stroke = 2.5f) { swapArrows() } }

    val Card: ImageVector by lazy {
        outline("Card") {
            roundRect(3f, 5.5f, 21f, 18.5f, 2.5f)
            moveTo(3f, 10f); lineTo(21f, 10f)
            moveTo(6.5f, 14.5f); lineTo(9.5f, 14.5f)
        }
    }
    val CardFilled: ImageVector by lazy {
        icon("CardFilled") {
            path(
                fill = SolidColor(Color.Black),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineJoin = StrokeJoin.Round,
                pathFillType = PathFillType.EvenOdd,
            ) {
                roundRect(3f, 5.5f, 21f, 18.5f, 2.5f)
                moveTo(3.9f, 9f); lineTo(20.1f, 9f); lineTo(20.1f, 11f); lineTo(3.9f, 11f); close()
            }
        }
    }

    val More: ImageVector by lazy { outline("More") { fourSquares() } }
    val MoreFilled: ImageVector by lazy { filled("MoreFilled") { fourSquares() } }

    val Plus: ImageVector by lazy {
        outline("Plus", stroke = 2.25f) {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }
    val Eye: ImageVector by lazy { outline("Eye") { eye() } }
    val EyeOff: ImageVector by lazy {
        outline("EyeOff") {
            eye()
            moveTo(4f, 4f); lineTo(20f, 20f)
        }
    }
    val Search: ImageVector by lazy {
        outline("Search") {
            circle(11f, 11f, 6.5f)
            moveTo(16f, 16f); lineTo(20f, 20f)
        }
    }
    val ChevronRight: ImageVector by lazy { outline("ChevronRight") { moveTo(9.5f, 6f); lineTo(15.5f, 12f); lineTo(9.5f, 18f) } }
    val Close: ImageVector by lazy {
        outline("Close") {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }
    val Check: ImageVector by lazy { outline("Check", stroke = 2f) { moveTo(5f, 12.5f); lineTo(10f, 17f); lineTo(19f, 7.5f) } }
    val Alert: ImageVector by lazy {
        outline("Alert") {
            circle(12f, 12f, 8.5f)
            moveTo(12f, 7.75f); lineTo(12f, 12.5f)
            moveTo(12f, 16f); lineTo(12f, 16.01f)
        }
    }
    val Repeat: ImageVector by lazy {
        outline("Repeat") {
            moveTo(17f, 3.5f); lineTo(20f, 6.5f); lineTo(17f, 9.5f)
            moveTo(4f, 11.5f); lineTo(4f, 10f)
            curveTo(4f, 8.07f, 5.57f, 6.5f, 7.5f, 6.5f); lineTo(20f, 6.5f)
            moveTo(7f, 20.5f); lineTo(4f, 17.5f); lineTo(7f, 14.5f)
            moveTo(20f, 12.5f); lineTo(20f, 14f)
            curveTo(20f, 15.93f, 18.43f, 17.5f, 16.5f, 17.5f); lineTo(4f, 17.5f)
        }
    }
    val Target: ImageVector by lazy {
        outline("Target") {
            circle(12f, 12f, 8.5f)
            circle(12f, 12f, 4.5f)
            circle(12f, 12f, 0.75f)
        }
    }
    val Tag: ImageVector by lazy {
        outline("Tag") {
            moveTo(3.5f, 12.6f); lineTo(3.5f, 5f)
            curveTo(3.5f, 4.17f, 4.17f, 3.5f, 5f, 3.5f); lineTo(12.6f, 3.5f)
            lineTo(20.5f, 11.4f)
            curveTo(21.1f, 12f, 21.1f, 12.95f, 20.5f, 13.55f); lineTo(13.55f, 20.5f)
            curveTo(12.95f, 21.1f, 12f, 21.1f, 11.4f, 20.5f); close()
            circle(8f, 8f, 1.25f)
        }
    }
    val Analytics: ImageVector by lazy {
        outline("Analytics") {
            moveTo(5f, 20f); lineTo(5f, 13f)
            moveTo(10f, 20f); lineTo(10f, 8f)
            moveTo(15f, 20f); lineTo(15f, 11f)
            moveTo(20f, 20f); lineTo(20f, 4.5f)
        }
    }

    // region Sprint 2-B: navegação, contas e categorias

    val Back: ImageVector by lazy { outline("Back") { moveTo(14.5f, 6f); lineTo(8.5f, 12f); lineTo(14.5f, 18f) } }
    val Archive: ImageVector by lazy {
        outline("Archive") {
            roundRect(3.5f, 4.5f, 20.5f, 8.5f, 1.5f)
            moveTo(5f, 8.5f); lineTo(5f, 18f)
            curveTo(5f, 18.83f, 5.67f, 19.5f, 6.5f, 19.5f); lineTo(17.5f, 19.5f)
            curveTo(18.33f, 19.5f, 19f, 18.83f, 19f, 18f); lineTo(19f, 8.5f)
            moveTo(10f, 12.5f); lineTo(14f, 12.5f)
        }
    }
    val Restore: ImageVector by lazy {
        outline("Restore") {
            moveTo(4.5f, 12f)
            curveTo(4.5f, 7.86f, 7.86f, 4.5f, 12f, 4.5f)
            curveTo(16.14f, 4.5f, 19.5f, 7.86f, 19.5f, 12f)
            curveTo(19.5f, 16.14f, 16.14f, 19.5f, 12f, 19.5f)
            curveTo(9.6f, 19.5f, 7.46f, 18.37f, 6.09f, 16.62f)
            moveTo(4.5f, 7.5f); lineTo(4.5f, 12f); lineTo(9f, 12f)
        }
    }
    /** Cadeado: arquivo protegido por senha. */
    val Lock: ImageVector by lazy {
        outline("Lock") {
            roundRect(5f, 10.5f, 19f, 20.5f, 2.5f)
            moveTo(8f, 10.5f); lineTo(8f, 7.5f)
            curveTo(8f, 5.29f, 9.79f, 3.5f, 12f, 3.5f)
            curveTo(14.21f, 3.5f, 16f, 5.29f, 16f, 7.5f); lineTo(16f, 10.5f)
            moveTo(12f, 14.5f); lineTo(12f, 16.5f)
        }
    }

    /** Salvar uma cópia (bandeja com seta para baixo). */
    val Download: ImageVector by lazy {
        outline("Download") {
            moveTo(12f, 4f); lineTo(12f, 14.5f)
            moveTo(7.5f, 10f); lineTo(12f, 14.5f); lineTo(16.5f, 10f)
            moveTo(4.5f, 15.5f); lineTo(4.5f, 18f)
            curveTo(4.5f, 18.83f, 5.17f, 19.5f, 6f, 19.5f); lineTo(18f, 19.5f)
            curveTo(18.83f, 19.5f, 19.5f, 18.83f, 19.5f, 18f); lineTo(19.5f, 15.5f)
        }
    }

    val Calendar: ImageVector by lazy {
        outline("Calendar") {
            roundRect(3.5f, 5f, 20.5f, 20f, 2.5f)
            moveTo(3.5f, 9.5f); lineTo(20.5f, 9.5f)
            moveTo(8f, 3f); lineTo(8f, 6.5f)
            moveTo(16f, 3f); lineTo(16f, 6.5f)
        }
    }
    val Bank: ImageVector by lazy {
        outline("Bank") {
            moveTo(3.5f, 9f); lineTo(12f, 4f); lineTo(20.5f, 9f); close()
            moveTo(6f, 11.5f); lineTo(6f, 17f)
            moveTo(10f, 11.5f); lineTo(10f, 17f)
            moveTo(14f, 11.5f); lineTo(14f, 17f)
            moveTo(18f, 11.5f); lineTo(18f, 17f)
            moveTo(3.5f, 20f); lineTo(20.5f, 20f)
        }
    }
    val Wallet: ImageVector by lazy {
        outline("Wallet") {
            roundRect(3.5f, 6f, 20.5f, 19f, 2.5f)
            moveTo(3.5f, 9.5f); lineTo(17f, 9.5f)
            roundRect(14.5f, 12f, 20.5f, 15.5f, 1.5f)
        }
    }
    val Coins: ImageVector by lazy {
        outline("Coins") {
            roundRect(4f, 15.5f, 20f, 19.5f, 2f)
            roundRect(5.5f, 10.5f, 18.5f, 14.5f, 2f)
            roundRect(7f, 5.5f, 17f, 9.5f, 2f)
        }
    }
    val Cart: ImageVector by lazy {
        outline("Cart") {
            moveTo(3.5f, 4.5f); lineTo(5.5f, 4.5f); lineTo(7.5f, 15f); lineTo(18f, 15f); lineTo(20f, 8f); lineTo(6.4f, 8f)
            circle(9f, 19f, 1.25f)
            circle(17f, 19f, 1.25f)
        }
    }
    val Utensils: ImageVector by lazy {
        outline("Utensils") {
            moveTo(7f, 3.5f); lineTo(7f, 20.5f)
            moveTo(4.5f, 3.5f); lineTo(4.5f, 8f)
            curveTo(4.5f, 9.38f, 5.62f, 10.5f, 7f, 10.5f)
            curveTo(8.38f, 10.5f, 9.5f, 9.38f, 9.5f, 8f); lineTo(9.5f, 3.5f)
            moveTo(17f, 20.5f); lineTo(17f, 3.5f)
            curveTo(15f, 4.5f, 14f, 7f, 14f, 10f); lineTo(14f, 13f); lineTo(17f, 13f)
        }
    }
    val Car: ImageVector by lazy {
        outline("Car") {
            moveTo(4f, 16.5f); lineTo(4f, 12.5f); lineTo(6f, 7.5f)
            curveTo(6.3f, 6.9f, 6.9f, 6.5f, 7.6f, 6.5f); lineTo(16.4f, 6.5f)
            curveTo(17.1f, 6.5f, 17.7f, 6.9f, 18f, 7.5f); lineTo(20f, 12.5f); lineTo(20f, 16.5f); close()
            moveTo(4f, 12.5f); lineTo(20f, 12.5f)
            moveTo(6.5f, 16.5f); lineTo(6.5f, 19f)
            moveTo(17.5f, 16.5f); lineTo(17.5f, 19f)
        }
    }
    val Heart: ImageVector by lazy {
        outline("Heart") {
            moveTo(12f, 19.5f)
            curveTo(7f, 16f, 3.5f, 12.8f, 3.5f, 9f)
            curveTo(3.5f, 6.5f, 5.5f, 4.5f, 8f, 4.5f)
            curveTo(9.6f, 4.5f, 11.1f, 5.4f, 12f, 6.8f)
            curveTo(12.9f, 5.4f, 14.4f, 4.5f, 16f, 4.5f)
            curveTo(18.5f, 4.5f, 20.5f, 6.5f, 20.5f, 9f)
            curveTo(20.5f, 12.8f, 17f, 16f, 12f, 19.5f)
            close()
        }
    }
    val Briefcase: ImageVector by lazy {
        outline("Briefcase") {
            roundRect(3.5f, 7.5f, 20.5f, 19.5f, 2.5f)
            moveTo(9f, 7.5f); lineTo(9f, 5.5f)
            curveTo(9f, 4.95f, 9.45f, 4.5f, 10f, 4.5f); lineTo(14f, 4.5f)
            curveTo(14.55f, 4.5f, 15f, 4.95f, 15f, 5.5f); lineTo(15f, 7.5f)
            moveTo(3.5f, 12.5f); lineTo(20.5f, 12.5f)
        }
    }
    val Gift: ImageVector by lazy {
        outline("Gift") {
            roundRect(4f, 9f, 20f, 12.5f, 1.5f)
            moveTo(5.5f, 12.5f); lineTo(5.5f, 19f)
            curveTo(5.5f, 19.55f, 5.95f, 20f, 6.5f, 20f); lineTo(17.5f, 20f)
            curveTo(18.05f, 20f, 18.5f, 19.55f, 18.5f, 19f); lineTo(18.5f, 12.5f)
            moveTo(12f, 9f); lineTo(12f, 20f)
            moveTo(12f, 9f)
            curveTo(10.5f, 5.5f, 7f, 5.5f, 7.5f, 7.5f)
            curveTo(7.8f, 8.7f, 10f, 9f, 12f, 9f)
            curveTo(14f, 9f, 16.2f, 8.7f, 16.5f, 7.5f)
            curveTo(17f, 5.5f, 13.5f, 5.5f, 12f, 9f)
        }
    }
    val Book: ImageVector by lazy {
        outline("Book") {
            moveTo(12f, 6.5f)
            curveTo(10.5f, 5.2f, 8f, 4.5f, 4f, 4.5f); lineTo(4f, 18f)
            curveTo(8f, 18f, 10.5f, 18.7f, 12f, 20f)
            curveTo(13.5f, 18.7f, 16f, 18f, 20f, 18f); lineTo(20f, 4.5f)
            curveTo(16f, 4.5f, 13.5f, 5.2f, 12f, 6.5f)
            close()
            moveTo(12f, 6.5f); lineTo(12f, 20f)
        }
    }
    val Bolt: ImageVector by lazy {
        outline("Bolt") {
            moveTo(13f, 3f); lineTo(5f, 13.5f); lineTo(11.5f, 13.5f); lineTo(10.5f, 21f); lineTo(19f, 10.5f); lineTo(12.5f, 10.5f); close()
        }
    }

    /** Pausar (assinatura pausada). */
    val Pause: ImageVector by lazy {
        outline("Pause", stroke = 2.25f) {
            moveTo(9f, 6f); lineTo(9f, 18f)
            moveTo(15f, 6f); lineTo(15f, 18f)
        }
    }

    /** Retomar. */
    val Play: ImageVector by lazy { outline("Play") { moveTo(8f, 5.5f); lineTo(18f, 12f); lineTo(8f, 18.5f); close() } }

    /** Pular uma cobrança. */
    val Skip: ImageVector by lazy {
        outline("Skip") {
            moveTo(6f, 6f); lineTo(14.5f, 12f); lineTo(6f, 18f); close()
            moveTo(18f, 6f); lineTo(18f, 18f)
        }
    }

    /** Tempo (bloqueio automático). */
    val Clock: ImageVector by lazy {
        outline("Clock") {
            circle(12f, 12f, 8.5f)
            moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f)
        }
    }

    /** Liberar (reduzir a reserva). */
    val Minus: ImageVector by lazy { outline("Minus", stroke = 2.25f) { moveTo(5f, 12f); lineTo(19f, 12f) } }

    // endregion

    // region builders

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun outline(name: String, stroke: Float = 1.75f, shape: PathBuilder.() -> Unit) = icon(name) {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = shape,
        )
    }

    private fun filled(name: String, shape: PathBuilder.() -> Unit) = icon(name) {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.75f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = shape,
        )
    }

    private fun PathBuilder.house() {
        moveTo(4f, 10.2f); lineTo(12f, 4f); lineTo(20f, 10.2f); lineTo(20f, 18.5f)
        curveTo(20f, 19.33f, 19.33f, 20f, 18.5f, 20f); lineTo(5.5f, 20f)
        curveTo(4.67f, 20f, 4f, 19.33f, 4f, 18.5f); close()
    }

    private fun PathBuilder.swapArrows() {
        moveTo(7.5f, 3.75f); lineTo(4f, 7.25f); lineTo(7.5f, 10.75f)
        moveTo(4f, 7.25f); lineTo(17f, 7.25f)
        moveTo(16.5f, 13.25f); lineTo(20f, 16.75f); lineTo(16.5f, 20.25f)
        moveTo(20f, 16.75f); lineTo(7f, 16.75f)
    }

    private fun PathBuilder.fourSquares() {
        roundRect(4f, 4f, 10.5f, 10.5f, 2f)
        roundRect(13.5f, 4f, 20f, 10.5f, 2f)
        roundRect(4f, 13.5f, 10.5f, 20f, 2f)
        roundRect(13.5f, 13.5f, 20f, 20f, 2f)
    }

    private fun PathBuilder.eye() {
        moveTo(2.5f, 12f)
        curveTo(4.5f, 7.5f, 8f, 5.5f, 12f, 5.5f)
        curveTo(16f, 5.5f, 19.5f, 7.5f, 21.5f, 12f)
        curveTo(19.5f, 16.5f, 16f, 18.5f, 12f, 18.5f)
        curveTo(8f, 18.5f, 4.5f, 16.5f, 2.5f, 12f)
        close()
        circle(12f, 12f, 3f)
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        close()
    }

    private fun PathBuilder.roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float) {
        moveTo(l + rad, t); lineTo(r - rad, t)
        arcTo(rad, rad, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = r, y1 = t + rad)
        lineTo(r, b - rad)
        arcTo(rad, rad, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = r - rad, y1 = b)
        lineTo(l + rad, b)
        arcTo(rad, rad, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = l, y1 = b - rad)
        lineTo(l, t + rad)
        arcTo(rad, rad, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = l + rad, y1 = t)
        close()
    }

    // endregion
}
