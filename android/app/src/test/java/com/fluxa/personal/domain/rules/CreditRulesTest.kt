package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCard
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult.Rejected
import com.fluxa.personal.domain.model.CreditResult.Saved
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CreditRulesTest {
    @Test
    fun closingDayStartsNextCycle() {
        assertEquals(date("2026-11-17"), due("2026-11-09", 10, 17))
        assertEquals(date("2026-12-17"), due("2026-11-10", 10, 17))
        assertEquals(date("2026-12-17"), due("2026-11-11", 10, 17))
        assertEquals(date("2027-01-17"), due("2026-12-10", 10, 17))
    }

    @Test
    fun dueDateMustFollowClosingEvenIfTheDueDayClampsToIt() {
        assertEquals(date("2026-12-05"), due("2026-11-01", 10, 5))
        assertEquals(date("2026-12-10"), due("2026-11-01", 10, 10))
        assertEquals(date("2025-03-29"), due("2025-02-01", 28, 29))
        assertEquals(date("2025-04-30"), due("2025-02-28", 31, 31))
    }

    @Test
    fun clampsPreferredDays28To31InFebruaryAndHandlesLeapYears() {
        for (day in 28..31) {
            assertEquals(date("2025-02-28"), due("2025-02-01", 10, day))
            assertEquals(LocalDate.of(2024, 2, minOf(day, 29)), due("2024-02-01", 10, day))
        }
    }

    @Test
    fun scheduleUsesTheExistingSplitterAndRestoresPreferredDueDay() {
        val now = Instant.EPOCH
        val card = CreditCard(
            UUID(0, 1), CreditCardDetails("Cartão", Money(10001), BillingDay(10), BillingDay(31)), null, now, now,
        )
        val category = Category(UUID(0, 2), CategoryDetails("Despesas", EXPENSE), null)
        val draft = PurchaseDraft(card.id, category.id, "Compra", Money(10001), 3, date("2024-02-01"))
        val summary = CreditCardSummary(card, Money(0))
        val installments = (CreditPurchaseRules.schedule(draft, summary, category) as Saved).value

        assertEquals(listOf(3333L, 3334L, 3334L), installments.map { it.amount.cents })
        assertEquals(draft.total.cents, installments.sumOf { it.amount.cents })
        assertEquals(listOf("2024-02-29", "2024-03-31", "2024-04-30").map(::date), installments.map { it.dueDate })
        assertEquals(
            Rejected(CreditError.INVALID_INSTALLMENT_COUNT),
            CreditPurchaseRules.schedule(draft.copy(total = Money(2)), summary, category),
        )
        assertEquals(
            Rejected(CreditError.INVALID_SCHEDULE_DATE),
            CreditPurchaseRules.schedule(draft.copy(purchaseDate = LocalDate.MAX), summary, category),
        )
    }

    @Test
    fun limitHandlesSufficientExactAndInsufficientCredit() {
        val limit = CreditLimit(Money(1000), Money(300))
        assertEquals(Money(700), limit.available)
        assertTrue(limit.canCommit(Money(699)))
        assertTrue(limit.canCommit(Money(700)))
        assertFalse(limit.canCommit(Money(701)))
        assertFalse(limit.canCommit(Money(0)))
        assertTrue(limit.canSetTotal(Money(300)))
        assertTrue(limit.canSetTotal(Money(301)))
        assertFalse(limit.canSetTotal(Money(299)))
        assertThrows(IllegalArgumentException::class.java) { CreditLimit(Money(299), Money(300)) }
    }

    private fun due(purchase: String, closing: Int, dueDay: Int) =
        CreditBillingCalendar.firstDueDate(date(purchase), BillingDay(closing), BillingDay(dueDay))

    private fun date(value: String): LocalDate = LocalDate.parse(value)
}
