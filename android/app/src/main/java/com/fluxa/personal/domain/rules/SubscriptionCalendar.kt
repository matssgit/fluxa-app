package com.fluxa.personal.domain.rules

import com.fluxa.personal.domain.model.OccurrenceFrequency
import com.fluxa.personal.domain.model.OccurrenceSchedule
import com.fluxa.personal.domain.model.Subscription
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionOccurrence
import com.fluxa.personal.domain.model.SubscriptionOccurrenceKey
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionTerms
import java.time.LocalDate

internal object SubscriptionCalendar {
    fun agenda(
        subscription: Subscription,
        terms: List<SubscriptionTerms>,
        resolved: List<SubscriptionOccurrence>,
        fromInclusive: LocalDate,
        untilExclusive: LocalDate,
        today: LocalDate,
    ): List<SubscriptionOccurrence> {
        require(fromInclusive <= untilExclusive)
        val outcomes = resolved.filter { it.key.occurrenceDate in fromInclusive..<untilExclusive }.map {
            if (it.status == SubscriptionOccurrenceStatus.PENDING) it.copy(status = openStatus(it.key.occurrenceDate, today))
            else it
        }
        val resolvedKeys = outcomes.map { it.key }.toSet()
        val pending = terms.sortedBy { it.effectiveOn }.let { ordered ->
            ordered.flatMapIndexed { index, term ->
                val start = maxOf(fromInclusive, term.effectiveOn)
                val end = minOf(untilExclusive, ordered.getOrNull(index + 1)?.effectiveOn ?: untilExclusive)
                if (!term.enabled || start >= end) emptyList()
                else OccurrenceCalendar.between(schedule(subscription), start, end).mapNotNull {
                    val key = SubscriptionOccurrenceKey(subscription.id, it.dueDate)
                    if (key in resolvedKeys) null else SubscriptionOccurrence(
                        key, term.details,
                        openStatus(it.dueDate, today),
                    )
                }
            }
        }
        return (pending + outcomes).sortedBy { it.key.occurrenceDate }
    }

    fun next(
        subscription: Subscription,
        terms: List<SubscriptionTerms>,
        resolved: List<SubscriptionOccurrence>,
        today: LocalDate,
    ): SubscriptionOccurrence? {
        val reopened = resolved.asSequence()
            .filter { it.status == SubscriptionOccurrenceStatus.PENDING && it.key.occurrenceDate >= today }
            .minByOrNull { it.key.occurrenceDate }
        val ordered = terms.sortedBy { it.effectiveOn }
        for ((index, term) in ordered.withIndex()) {
            if (!term.enabled) continue
            var start = maxOf(today, subscription.anchorDate, term.effectiveOn)
            val end = ordered.getOrNull(index + 1)?.effectiveOn ?: LocalDate.MAX
            while (start < end) {
                val windowEnd = if (start.year < LocalDate.MAX.year - 1) minOf(end, start.plusYears(2)) else end
                if (reopened != null && reopened.key.occurrenceDate < start) return reopened
                val pending = agenda(subscription, listOf(term), resolved, start, windowEnd, today)
                    .firstOrNull { it.status == SubscriptionOccurrenceStatus.PENDING }
                if (pending != null) {
                    return reopened?.takeIf { it.key.occurrenceDate < pending.key.occurrenceDate } ?: pending
                }
                start = windowEnd
            }
        }
        return reopened
    }

    fun openStatus(date: LocalDate, today: LocalDate) =
        if (date < today) SubscriptionOccurrenceStatus.OVERDUE else SubscriptionOccurrenceStatus.PENDING

    private fun schedule(subscription: Subscription) = OccurrenceSchedule(
        subscription.id.toString(), subscription.anchorDate,
        when (subscription.frequency) {
            SubscriptionFrequency.MONTHLY -> OccurrenceFrequency.MONTHLY
            SubscriptionFrequency.YEARLY -> OccurrenceFrequency.YEARLY
        }, subscription.billingDay,
    )
}
