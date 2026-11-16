package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.date.BillingDay
import java.time.LocalDate
import java.time.YearMonth

internal object CreditBillingCalendar {
    fun firstDueDate(purchaseDate: LocalDate, closingDay: BillingDay, dueDay: BillingDay): LocalDate {
        val purchaseMonth = YearMonth.from(purchaseDate)
        val closingMonth = if (purchaseDate < closingDay.inMonth(purchaseMonth)) {
            purchaseMonth
        } else {
            purchaseMonth.plusMonths(1)
        }
        val sameMonthDue = dueDay.inMonth(closingMonth)
        return if (sameMonthDue > closingDay.inMonth(closingMonth)) {
            sameMonthDue
        } else {
            dueDay.inMonth(closingMonth.plusMonths(1))
        }
    }
}
