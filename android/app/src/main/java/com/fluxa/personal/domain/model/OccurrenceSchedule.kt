package com.fluxa.personal.domain.model

import com.fluxa.personal.core.date.BillingDay
import java.time.LocalDate

enum class OccurrenceFrequency {
    ONCE,
    MONTHLY,
    YEARLY,
}

data class OccurrenceSchedule(
    val sourceId: String,
    val anchorDate: LocalDate,
    val frequency: OccurrenceFrequency,
    val billingDay: BillingDay = BillingDay(anchorDate.dayOfMonth),
) {
    init {
        require(sourceId.isNotBlank()) { "Occurrence source ID must be present." }
        require(frequency != OccurrenceFrequency.ONCE || billingDay.day == anchorDate.dayOfMonth) {
            "A one-time occurrence uses the anchor date."
        }
    }
}

data class OccurrenceId(val sourceId: String, val occurrenceDate: LocalDate)

/** A forecast for the agenda. It has no payment date or cash movement. */
data class ExpectedOccurrence(val id: OccurrenceId) {
    val dueDate: LocalDate get() = id.occurrenceDate
}
