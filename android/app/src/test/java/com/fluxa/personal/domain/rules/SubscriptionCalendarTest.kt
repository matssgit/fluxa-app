package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubscriptionCalendarTest {
    private val id = UUID(0, 1)
    private val details = SubscriptionDetails("Original", Money(60000), UUID(0, 2), SubscriptionSource.Account(UUID(0, 3)))

    @Test
    fun monthlyClampsShortMonthsAndRestoresThePreferredDay() {
        val subscription = subscription("2026-01-31", SubscriptionFrequency.MONTHLY)
        val rows = agenda(subscription, listOf(term("2026-01-31")), from = "2026-01-01", until = "2026-04-01")
        assertEquals(listOf("2026-01-31", "2026-02-28", "2026-03-31").map(::date), rows.map { it.key.occurrenceDate })
        assertEquals(SubscriptionOccurrenceStatus.OVERDUE, rows.first().status)
        assertEquals(SubscriptionOccurrenceStatus.PENDING, rows.last().status)
    }

    @Test
    fun yearlyOccursOnceAtFullAmountAndRestoresLeapDay() {
        val subscription = subscription("2024-02-29", SubscriptionFrequency.YEARLY)
        val rows = agenda(subscription, listOf(term("2024-02-29")), from = "2024-01-01", until = "2029-01-01")
        assertEquals(
            listOf("2024-02-29", "2025-02-28", "2026-02-28", "2027-02-28", "2028-02-29").map(::date),
            rows.map { it.key.occurrenceDate },
        )
        assertEquals(List(5) { Money(60000) }, rows.map { it.details.amount })
    }

    @Test
    fun preservesTermsBeforeChangesAndOmitsDatesDuringPauseAndAfterCancellation() {
        val subscription = subscription("2026-01-15", SubscriptionFrequency.MONTHLY)
        val revised = details.copy(amount = Money(1200), source = SubscriptionSource.Card(UUID(0, 4)))
        val terms = listOf(
            term("2026-01-15"),
            SubscriptionTerms(date("2026-02-01"), revised, true),
            SubscriptionTerms(date("2026-03-01"), revised, false),
            SubscriptionTerms(date("2026-04-16"), revised, true),
            SubscriptionTerms(date("2026-06-01"), revised, false),
        )
        val rows = agenda(subscription.copy(status = SubscriptionStatus.CANCELLED), terms, from = "2026-01-01", until = "2026-07-01")
        assertEquals(listOf("2026-01-15", "2026-02-15", "2026-05-15").map(::date), rows.map { it.key.occurrenceDate })
        assertEquals(listOf(details, revised, revised), rows.map { it.details })
        assertNull(SubscriptionCalendar.next(subscription, terms, emptyList(), date("2026-06-01")))
    }

    @Test
    fun skippedAndResolvedSnapshotsRemainVisibleAndDoNotAppearAsNext() {
        val subscription = subscription("2026-01-15", SubscriptionFrequency.MONTHLY)
        val terms = listOf(term("2026-01-15"))
        val skip = SubscriptionOccurrence(
            SubscriptionOccurrenceKey(id, date("2026-02-15")), details, SubscriptionOccurrenceStatus.SKIPPED,
        )
        val rows = agenda(subscription, terms, listOf(skip), "2026-01-01", "2026-04-01")
        assertEquals(skip, rows[1])
        assertEquals(date("2026-03-15"), SubscriptionCalendar.next(subscription, terms, listOf(skip), date("2026-02-15"))!!.key.occurrenceDate)
    }

    private fun agenda(
        subscription: Subscription, terms: List<SubscriptionTerms>, resolved: List<SubscriptionOccurrence> = emptyList(),
        from: String, until: String,
    ) = SubscriptionCalendar.agenda(subscription, terms, resolved, date(from), date(until), date("2026-03-01"))

    private fun subscription(anchor: String, frequency: SubscriptionFrequency): Subscription {
        val date = date(anchor)
        return Subscription(id, details, frequency, date, BillingDay(date.dayOfMonth), SubscriptionStatus.ACTIVE, Instant.EPOCH, Instant.EPOCH)
    }

    private fun term(day: String) = SubscriptionTerms(date(day), details, true)
    private fun date(value: String) = LocalDate.parse(value)
}
