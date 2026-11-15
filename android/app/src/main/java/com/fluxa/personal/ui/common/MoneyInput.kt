package com.fluxa.personal.ui.common

import com.fluxa.personal.core.money.Money
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Entrada monetária em formato brasileiro, convertida para [Money] sem ponto flutuante.
 *
 * O campo recebe o valor absoluto. O sinal vem de um controle separado ("Saldo negativo"),
 * porque o teclado numérico do Android nem sempre oferece o "-".
 * A conversão usa [Money.fromReais] com [RoundingMode.UNNECESSARY], como orienta o domínio:
 * mais de dois decimais é erro de entrada, não arredondamento silencioso.
 */
internal object MoneyInput {

    sealed interface Result {
        data class Valid(val money: Money) : Result
        data object Empty : Result
        data object InvalidFormat : Result
        data object TooManyDecimals : Result
        data object SignNotAllowed : Result
        data object TooLarge : Result
    }

    /** Até 13 dígitos inteiros (trilhões de reais): bem abaixo do limite de Long em centavos. */
    private const val MAX_INTEGER_DIGITS = 13
    private val Grouped = Regex("""^\d{1,3}(\.\d{3})+(,\d+)?$""")
    private val Plain = Regex("""^\d+(,\d+)?$""")

    fun parse(raw: String, negative: Boolean = false): Result {
        val text = raw.replace("R$", "").replace(' ', ' ').trim().replace(" ", "")
        if (text.isEmpty()) return Result.Empty
        if (text.startsWith("-") || text.startsWith("−") || text.startsWith("+")) return Result.SignNotAllowed
        if (!Grouped.matches(text) && !Plain.matches(text)) return Result.InvalidFormat

        val normalized = text.replace(".", "").replace(',', '.')
        val integerPart = normalized.substringBefore('.')
        if (integerPart.trimStart('0').length > MAX_INTEGER_DIGITS) return Result.TooLarge
        val decimals = normalized.substringAfter('.', missingDelimiterValue = "")
        if (decimals.length > 2) return Result.TooManyDecimals

        val reais = BigDecimal(normalized).let { if (negative) it.negate() else it }
        return try {
            Result.Valid(Money.fromReais(reais, RoundingMode.UNNECESSARY))
        } catch (_: ArithmeticException) {
            Result.TooLarge
        }
    }

    /** Valor absoluto pronto para edição: "1.234,56". O sinal é exibido pelo controle separado. */
    fun formatForEditing(money: Money): String {
        val format = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale("pt", "BR")))
        return format.format(money.toReais().abs())
    }

    /** Mantém só dígitos, vírgula e ponto enquanto o usuário digita. */
    fun sanitizeTyping(raw: String): String = raw.filter { it.isDigit() || it == ',' || it == '.' }.take(24)
}
