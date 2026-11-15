package com.fluxa.personal.core.money

import java.math.BigDecimal
import java.math.RoundingMode

/** An amount in BRL. Balances and arithmetic results may be signed. */
@JvmInline
value class Money(val cents: Long) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(Math.addExact(cents, other.cents))

    operator fun minus(other: Money): Money = Money(Math.subtractExact(cents, other.cents))

    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    fun toReais(): BigDecimal = BigDecimal.valueOf(cents, 2)

    /** A percentage of 10 means 10%. Round once to whole cents. */
    fun percentage(percent: BigDecimal, rounding: RoundingMode): Money {
        val result = BigDecimal.valueOf(cents).multiply(percent).movePointLeft(2)
        return Money(result.setScale(0, rounding).longValueExact())
    }

    /** Subtracts the rounded discount so the discount and payable amount reconcile. */
    fun afterDiscount(percent: BigDecimal, rounding: RoundingMode): Money {
        require(cents >= 0) { "A discount requires a non-negative amount." }
        require(percent >= BigDecimal.ZERO && percent <= HUNDRED) {
            "Discount percentage must be within 0 and 100."
        }
        return this - percentage(percent, rounding)
    }

    companion object {
        const val CURRENCY_CODE = "BRL"
        private val HUNDRED = BigDecimal.valueOf(100)

        fun fromReais(amount: BigDecimal, rounding: RoundingMode): Money =
            Money(amount.movePointRight(2).setScale(0, rounding).longValueExact())
    }
}
