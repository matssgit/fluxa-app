package com.fluxa.personal.ui.feature.catalog

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fluxa.personal.R
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.ui.designsystem.FluxaIcons

/**
 * Destino visual dos campos opcionais `icon` e `color` de categoria (o contrato guarda apenas texto).
 *
 * - `icon` guarda uma chave estável ("cart", "home"...). Chave desconhecida cai no ícone padrão.
 * - `color` guarda hexadecimal "#RRGGBB", portátil para backup e web. Valor inválido cai na cor padrão.
 *
 * A cor é decorativa: o texto nunca é desenhado sobre ela e a categoria sempre tem nome e ícone.
 */
internal data class CategoryIconOption(val key: String, val icon: ImageVector, @StringRes val label: Int)

internal data class CategoryColorOption(val hex: String, @StringRes val label: Int) {
    val color: Color get() = parseHexColor(hex) ?: DefaultCategoryColor
}

internal val CategoryIcons: List<CategoryIconOption> by lazy {
    listOf(
        CategoryIconOption("tag", FluxaIcons.Tag, R.string.icon_tag),
        CategoryIconOption("home", FluxaIcons.Home, R.string.icon_home),
        CategoryIconOption("cart", FluxaIcons.Cart, R.string.icon_cart),
        CategoryIconOption("food", FluxaIcons.Utensils, R.string.icon_food),
        CategoryIconOption("transport", FluxaIcons.Car, R.string.icon_transport),
        CategoryIconOption("health", FluxaIcons.Heart, R.string.icon_health),
        CategoryIconOption("work", FluxaIcons.Briefcase, R.string.icon_work),
        CategoryIconOption("gift", FluxaIcons.Gift, R.string.icon_gift),
        CategoryIconOption("education", FluxaIcons.Book, R.string.icon_education),
        CategoryIconOption("utilities", FluxaIcons.Bolt, R.string.icon_utilities),
        CategoryIconOption("subscription", FluxaIcons.Repeat, R.string.icon_subscription),
        CategoryIconOption("savings", FluxaIcons.Coins, R.string.icon_savings),
    )
}

internal val CategoryColors: List<CategoryColorOption> = listOf(
    CategoryColorOption("#35695A", R.string.color_pine),
    CategoryColorOption("#829B88", R.string.color_sage),
    CategoryColorOption("#B9684F", R.string.color_terracotta),
    CategoryColorOption("#C9A66B", R.string.color_sand),
    CategoryColorOption("#4E8C86", R.string.color_teal),
    CategoryColorOption("#8A6A84", R.string.color_plum),
    CategoryColorOption("#6F7D79", R.string.color_slate),
    CategoryColorOption("#A07A64", R.string.color_clay),
)

internal val DefaultCategoryColor = Color(0xFF829B88)

internal fun categoryIconFor(key: String?): ImageVector =
    CategoryIcons.firstOrNull { it.key == key }?.icon ?: FluxaIcons.Tag

internal fun categoryColorFor(hex: String?): Color = hex?.let(::parseHexColor) ?: DefaultCategoryColor

internal fun parseHexColor(hex: String): Color? {
    val digits = hex.removePrefix("#")
    if (digits.length != 6 || !digits.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return Color(0xFF000000 or digits.toLong(16))
}

internal data class AccountTypeVisual(@StringRes val label: Int, val icon: ImageVector)

internal fun AccountType.visual(): AccountTypeVisual = when (this) {
    AccountType.CHECKING -> AccountTypeVisual(R.string.account_type_checking, FluxaIcons.Bank)
    AccountType.WALLET -> AccountTypeVisual(R.string.account_type_wallet, FluxaIcons.Wallet)
    AccountType.SAVINGS -> AccountTypeVisual(R.string.account_type_savings, FluxaIcons.Coins)
}
