package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentSplitterTest {
    private val december = YearMonth.of(2023, 12)
    private val dueDay = BillingDay(31)

    @Test
    fun distributesRemainingCentsAcrossDistinctFinalInstallments() {
        val installments = InstallmentSplitter.split(Money(10001), 3, december, dueDay)

        assertEquals(listOf(3333L, 3334L, 3334L), installments.map { it.amount.cents })
        assertEquals(listOf(1, 2, 3), installments.map { it.number })
        assertTrue(installments.all { it.totalCount == 3 })
        assertEquals(
            listOf(LocalDate.of(2023, 12, 31), LocalDate.of(2024, 1, 31), LocalDate.of(2024, 2, 29)),
            installments.map { it.dueDate },
        )
    }

    @Test
    fun singleInstallmentKeepsTheEntireAmountWithoutOverflow() {
        val installment = InstallmentSplitter.split(Money(Long.MAX_VALUE), 1, december, dueDay).single()

        assertEquals(Money(Long.MAX_VALUE), installment.amount)
        assertEquals(1, installment.number)
        assertEquals(1, installment.totalCount)
        assertEquals(LocalDate.of(2023, 12, 31), installment.dueDate)
    }

    @Test
    fun reconcilesExactlyAndSpreadsAtMostOneCentAcrossManySchedules() {
        for (count in 1..48) {
            for (cents in listOf(count.toLong(), 98765L, Long.MAX_VALUE)) {
                val amounts = InstallmentSplitter.split(Money(cents), count, december, dueDay)
                    .map { it.amount.cents }
                assertEquals(cents, amounts.sum())
                assertTrue(amounts.min() >= 1)
                assertTrue(amounts.max() - amounts.min() <= 1)
            }
        }
    }

    @Test
    fun rejectsInvalidCountAndAmountsThatWouldProduceNonPositiveEntries() {
        for ((cents, count) in listOf(100L to 0, 100L to -1, 0L to 1, -1L to 1, 2L to 3)) {
            assertThrows(IllegalArgumentException::class.java) {
                InstallmentSplitter.split(Money(cents), count, december, dueDay)
            }
        }
    }
}
