package com.fluxa.personal.core.money

import java.math.BigDecimal
import java.math.RoundingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {
    @Test
    fun storesSignedBrlCentsAndConvertsExactly() {
        assertEquals("BRL", Money.CURRENCY_CODE)
        for ((decimal, cents) in listOf("1234.56" to 123456L, "-0.01" to -1L, "0.00" to 0L)) {
            val amount = Money.fromReais(BigDecimal(decimal), RoundingMode.UNNECESSARY)
            assertEquals(cents, amount.cents)
            assertEquals(decimal, amount.toReais().toPlainString())
        }
        assertTrue(Money(-1) < Money(0))
    }

    @Test
    fun additionAndSubtractionPreserveSignAndCheckOverflow() {
        assertEquals(Money(0), Money(35) + Money(-35))
        assertEquals(Money(-65), Money(35) - Money(100))
        assertEquals(Money(36), Money(35) - Money(-1))
        assertThrows(ArithmeticException::class.java) { Money(Long.MAX_VALUE) + Money(1) }
        assertThrows(ArithmeticException::class.java) { Money(Long.MIN_VALUE) - Money(1) }
    }

    @Test
    fun conversionUsesTheRequestedRoundingAndRejectsOverflow() {
        val tie = BigDecimal("1.005")
        assertEquals(Money(100), Money.fromReais(tie, RoundingMode.HALF_EVEN))
        assertEquals(Money(101), Money.fromReais(tie, RoundingMode.HALF_UP))
        assertEquals(Money(-101), Money.fromReais(tie.negate(), RoundingMode.HALF_UP))
        assertThrows(ArithmeticException::class.java) {
            Money.fromReais(tie, RoundingMode.UNNECESSARY)
        }
        assertThrows(ArithmeticException::class.java) {
            Money.fromReais(BigDecimal("92233720368547758.08"), RoundingMode.UNNECESSARY)
        }
        assertEquals(Money(Long.MIN_VALUE), Money.fromReais(Money(Long.MIN_VALUE).toReais(), RoundingMode.UNNECESSARY))
    }

    @Test
    fun discountsRoundTheDiscountOnceAndReconcileToTheOriginal() {
        val original = Money(101)
        val rate = BigDecimal("50")
        val rounding = RoundingMode.HALF_UP
        assertEquals(Money(51), original.percentage(rate, rounding))
        assertEquals(Money(50), original.afterDiscount(rate, rounding))
        assertEquals(original, original.percentage(rate, rounding) + original.afterDiscount(rate, rounding))
        assertEquals(original, original.afterDiscount(BigDecimal.ZERO, rounding))
        assertEquals(Money(0), original.afterDiscount(BigDecimal("100"), rounding))
        assertEquals(Money(-51), Money(-101).percentage(rate, rounding))
        assertEquals(Money(13), Money(100).percentage(BigDecimal("12.5"), rounding))
        assertEquals(Money(Long.MAX_VALUE), Money(Long.MAX_VALUE).percentage(BigDecimal("100"), rounding))
        assertThrows(ArithmeticException::class.java) { Money(Long.MAX_VALUE).percentage(BigDecimal("101"), rounding) }
    }

    @Test
    fun rejectsDiscountOutsideItsRange() {
        for (rate in listOf("-0.01", "100.01")) {
            assertThrows(IllegalArgumentException::class.java) {
                Money(100).afterDiscount(BigDecimal(rate), RoundingMode.HALF_EVEN)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            Money(-1).afterDiscount(BigDecimal.TEN, RoundingMode.UNNECESSARY)
        }
    }
}
