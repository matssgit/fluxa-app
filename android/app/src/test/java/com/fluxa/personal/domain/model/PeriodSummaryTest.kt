package com.fluxa.personal.domain.model

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import com.fluxa.personal.domain.rules.CalculatedBalance.Overflow
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PeriodSummaryTest {
    @Test
    fun calculatesEmptyAndSingleDirectionPeriods() {
        assertEquals(PeriodSummary(Exact(Money(0)), Exact(Money(0)), Exact(Money(0))), summary(0, 0))
        assertEquals(PeriodSummary(Exact(Money(40)), Exact(Money(0)), Exact(Money(40))), summary(40, 0))
        assertEquals(PeriodSummary(Exact(Money(0)), Exact(Money(40)), Exact(Money(-40))), summary(0, 40))
    }

    @Test
    fun calculatesPositiveAndNegativeNetFromBothDirections() {
        assertEquals(Exact(Money(15)), summary(35, 20).net)
        assertEquals(Exact(Money(-15)), summary(20, 35).net)
    }

    @Test
    fun reportsOverflowIndependentlyAndKeepsExactNet() {
        val aboveMax = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
        val summary = PeriodSummary.fromCents(aboveMax, aboveMax.add(BigInteger.TEN))
        assertEquals(Overflow, summary.income)
        assertEquals(Overflow, summary.expenses)
        assertEquals(Exact(Money(-10)), summary.net)
    }

    @Test
    fun historyPageRequiresPositiveLimitAndNonNegativeOffset() {
        assertEquals(0, TransactionHistoryPage(25).offset)
        assertThrows(IllegalArgumentException::class.java) { TransactionHistoryPage(0) }
        assertThrows(IllegalArgumentException::class.java) { TransactionHistoryPage(10, -1) }
    }

    private fun summary(income: Long, expenses: Long) =
        PeriodSummary.fromCents(BigInteger.valueOf(income), BigInteger.valueOf(expenses))
}
