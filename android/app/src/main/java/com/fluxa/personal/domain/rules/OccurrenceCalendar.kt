package com.fluxa.personal.domain.rules

import com.fluxa.personal.domain.model.ExpectedOccurrence
import com.fluxa.personal.domain.model.OccurrenceFrequency
import com.fluxa.personal.domain.model.OccurrenceId
import com.fluxa.personal.domain.model.OccurrenceSchedule
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

object OccurrenceCalendar {
    /**
     * Returns open forecasts in chronological order for [fromInclusive, untilExclusive).
     * An already realized occurrence is matched by its original identity, even when paid
     * on a different day. The calendar never produces a realized cash movement.
     */
    fun between(
        schedule: OccurrenceSchedule,
        fromInclusive: LocalDate,
        untilExclusive: LocalDate,
        completed: Set<OccurrenceId> = emptySet(),
    ): List<ExpectedOccurrence> {
        require(fromInclusive <= untilExclusive) { "Period end is before period start." }
        val start = maxOf(fromInclusive, schedule.anchorDate)
        if (start >= untilExclusive) return emptyList()

        if (schedule.frequency == OccurrenceFrequency.ONCE) {
            val id = OccurrenceId(schedule.sourceId, schedule.anchorDate)
            return if (schedule.anchorDate >= start && id !in completed) {
                listOf(ExpectedOccurrence(id))
            } else {
                emptyList()
            }
        }

        val startMonth = YearMonth.from(start)
        val lastMonth = YearMonth.from(untilExclusive.minusDays(1))
        val yearly = schedule.frequency == OccurrenceFrequency.YEARLY
        val step = if (yearly) 12L else 1L
        var month = if (yearly) YearMonth.of(start.year, schedule.anchorDate.month) else startMonth

        return buildList {
            while (month <= lastMonth) {
                val date = schedule.billingDay.inMonth(month)
                val id = OccurrenceId(schedule.sourceId, date)
                if (date >= start && date < untilExclusive && id !in completed) {
                    add(ExpectedOccurrence(id))
                }
                if (ChronoUnit.MONTHS.between(month, lastMonth) < step) break
                month = month.plusMonths(step)
            }
        }
    }
}
