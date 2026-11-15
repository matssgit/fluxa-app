package com.fluxa.personal.ui.common

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.ui.common.MoneyInput.Result
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyInputTest {

    private fun valid(cents: Long) = Result.Valid(Money(cents))

    @Test
    fun zeroIsAValidAmount() {
        assertEquals(valid(0), MoneyInput.parse("0"))
        assertEquals(valid(0), MoneyInput.parse("0,00"))
        assertEquals(valid(0), MoneyInput.parse("R$ 0,00"))
        assertEquals(valid(0), MoneyInput.parse("0,00", negative = true))
    }

    @Test
    fun parsesBrazilianDecimalAndGrouping() {
        assertEquals(valid(123_456), MoneyInput.parse("1.234,56"))
        assertEquals(valid(123_456), MoneyInput.parse("1234,56"))
        assertEquals(valid(123_450), MoneyInput.parse("1234,5"))
        assertEquals(valid(100_000_000), MoneyInput.parse("1.000.000"))
        assertEquals(valid(1_000), MoneyInput.parse(" R$ 10,00 "))
    }

    @Test
    fun negativeComesFromTheToggleNotTheText() {
        assertEquals(valid(-105_000), MoneyInput.parse("1.050,00", negative = true))
        assertEquals(Result.SignNotAllowed, MoneyInput.parse("-10,00"))
        assertEquals(Result.SignNotAllowed, MoneyInput.parse("−10,00"))
    }

    @Test
    fun rejectsAmbiguousOrInvalidInputInsteadOfGuessing() {
        assertEquals(Result.Empty, MoneyInput.parse(""))
        assertEquals(Result.Empty, MoneyInput.parse("  R$ "))
        assertEquals(Result.InvalidFormat, MoneyInput.parse("12.34"))
        assertEquals(Result.InvalidFormat, MoneyInput.parse("1,2,3"))
        assertEquals(Result.InvalidFormat, MoneyInput.parse("abc"))
        assertEquals(Result.InvalidFormat, MoneyInput.parse(",50"))
        assertEquals(Result.InvalidFormat, MoneyInput.parse("1.23,00"))
        assertEquals(Result.TooManyDecimals, MoneyInput.parse("1,234"))
        assertEquals(Result.TooLarge, MoneyInput.parse("12345678901234"))
    }

    @Test
    fun formatsForEditingWithoutSign() {
        assertEquals("1.234,56", MoneyInput.formatForEditing(Money(123_456)))
        assertEquals("1.234,56", MoneyInput.formatForEditing(Money(-123_456)))
        assertEquals("0,05", MoneyInput.formatForEditing(Money(5)))
        assertEquals("0,00", MoneyInput.formatForEditing(Money(0)))
    }

    @Test
    fun formattedValueParsesBackToTheSameMoney() {
        listOf(0L, 1L, 99L, 100L, 123_456L, 987_654_321L).forEach { cents ->
            assertEquals(valid(cents), MoneyInput.parse(MoneyInput.formatForEditing(Money(cents))))
        }
    }

    @Test
    fun typingKeepsOnlyDigitsAndSeparators() {
        assertEquals("1.234,5", MoneyInput.sanitizeTyping("R$ -1.234,5a"))
    }
}
