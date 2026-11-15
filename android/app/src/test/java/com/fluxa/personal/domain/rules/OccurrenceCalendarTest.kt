package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.domain.model.OccurrenceFrequency
import com.fluxa.personal.domain.model.OccurrenceId
import com.fluxa.personal.domain.model.OccurrenceSchedule
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OccurrenceCalendarTest {
    @Test
    fun treatsStartAsInclusiveAndEndAsExclusiveForOneTimeOccurrences() {
        val due = date("2025-04-30")
        val schedule = OccurrenceSchedule("bill", due, OccurrenceFrequency.ONCE)

        assertEquals(due, OccurrenceCalendar.between(schedule, due, due.plusDays(1)).single().dueDate)
        assertTrue(OccurrenceCalendar.between(schedule, due.minusDays(1), due).isEmpty())
        assertTrue(OccurrenceCalendar.between(schedule, due.plusDays(1), due.plusDays(2)).isEmpty())
        assertTrue(OccurrenceCalendar.between(schedule, due, due).isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            OccurrenceCalendar.between(schedule, due, due.minusDays(1))
        }
    }

    @Test
    fun monthlyScheduleCrossesYearsAndRecoversThePreferredDayAfterFebruary() {
        val schedule = OccurrenceSchedule("monthly", date("2023-12-31"), OccurrenceFrequency.MONTHLY)

        assertEquals(
            listOf("2023-12-31", "2024-01-31", "2024-02-29", "2024-03-31").map(::date),
            OccurrenceCalendar.between(schedule, date("2023-12-01"), date("2024-04-01"))
                .map { it.dueDate },
        )
    }

    @Test
    fun yearlyScheduleStaysInItsAnchorMonthAndReturnsToLeapDay() {
        val schedule = OccurrenceSchedule("annual", date("2024-02-29"), OccurrenceFrequency.YEARLY)

        assertEquals(
            listOf("2025-02-28", "2026-02-28", "2027-02-28", "2028-02-29").map(::date),
            OccurrenceCalendar.between(schedule, date("2024-03-01"), date("2029-02-01"))
                .map { it.dueDate },
        )
    }

    @Test
    fun startsAtOrAfterTheAnchorAndKeepsAnExplicitPreferredDay() {
        val anchor = date("2025-02-28")
        val until = date("2025-04-01")
        val schedule = OccurrenceSchedule("monthly", anchor, OccurrenceFrequency.MONTHLY, BillingDay(31))

        assertEquals(
            listOf(anchor, date("2025-03-31")),
            OccurrenceCalendar.between(schedule, date("2025-01-01"), until).map { it.dueDate },
        )
        assertEquals(
            listOf(date("2025-03-15")),
            OccurrenceCalendar.between(schedule.copy(billingDay = BillingDay(15)), anchor, until)
                .map { it.dueDate },
        )
    }

    @Test
    fun stableOccurrenceIdsPreventDuplicatesAcrossAdjacentWindowsAndSuppressRealizedEntries() {
        val start = date("2025-01-01")
        val boundary = date("2025-02-01")
        val end = date("2025-04-01")
        val schedule = OccurrenceSchedule("subscription", start, OccurrenceFrequency.MONTHLY)
        val expected = OccurrenceCalendar.between(schedule, start, end)
        val adjacent = OccurrenceCalendar.between(schedule, start, boundary) +
            OccurrenceCalendar.between(schedule, boundary, end)
        val overlapping = OccurrenceCalendar.between(schedule, boundary, end)

        assertEquals(expected, adjacent)
        assertEquals(expected.drop(1), overlapping)
        assertEquals(expected.size, expected.map { it.id }.toSet().size)
        assertEquals(
            expected.drop(1),
            OccurrenceCalendar.between(
                schedule,
                start,
                end,
                completed = setOf(OccurrenceId("subscription", start), OccurrenceId("other", boundary)),
            ),
        )
    }

    @Test
    fun handlesTheLastRepresentableMonthWithoutSteppingBeyondIt() {
        val anchor = LocalDate.MAX.minusDays(1)
        val schedule = OccurrenceSchedule("last", anchor, OccurrenceFrequency.MONTHLY)

        assertEquals(anchor, OccurrenceCalendar.between(schedule, anchor, LocalDate.MAX).single().dueDate)
    }

    private fun date(value: String): LocalDate = LocalDate.parse(value)
}
