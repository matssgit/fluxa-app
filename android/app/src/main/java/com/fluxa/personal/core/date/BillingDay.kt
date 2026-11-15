package com.fluxa.personal.core.date

import java.time.LocalDate
import java.time.YearMonth

/** A preferred calendar day, preserved even when a short month needs to clamp it. */
@JvmInline
value class BillingDay(val day: Int) {
    init {
        require(day in 1..31) { "Billing day must be within 1 and 31." }
    }

    fun inMonth(month: YearMonth): LocalDate = month.atDay(minOf(day, month.lengthOfMonth()))
}
