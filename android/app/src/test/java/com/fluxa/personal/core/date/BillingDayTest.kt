package com.fluxa.personal.core.date

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BillingDayTest {
    @Test
    fun clampsDays29To31InFebruaryAndRestoresTheRequestedDayInMarch() {
        for (day in 29..31) {
            val billingDay = BillingDay(day)
            assertEquals(LocalDate.of(2024, 2, 29), billingDay.inMonth(YearMonth.of(2024, 2)))
            assertEquals(LocalDate.of(2025, 2, 28), billingDay.inMonth(YearMonth.of(2025, 2)))
            assertEquals(LocalDate.of(2025, 3, day), billingDay.inMonth(YearMonth.of(2025, 3)))
        }
        assertEquals(LocalDate.of(2025, 4, 30), BillingDay(31).inMonth(YearMonth.of(2025, 4)))
    }

    @Test
    fun rejectsDaysOutsideCalendarRange() {
        assertThrows(IllegalArgumentException::class.java) { BillingDay(0) }
        assertThrows(IllegalArgumentException::class.java) { BillingDay(32) }
    }
}
