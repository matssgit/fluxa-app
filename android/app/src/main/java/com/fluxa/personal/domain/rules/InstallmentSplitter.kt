package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.ScheduledInstallment
import java.time.YearMonth

object InstallmentSplitter {
    /**
     * The caller supplies the first billing month. The closing-date policy belongs to cards.
     * The final [count] entries are numbered from 1, and each remaining cent goes to a
     * distinct installment at the end of the schedule.
     */
    fun split(
        total: Money,
        count: Int,
        firstDueMonth: YearMonth,
        dueDay: BillingDay,
    ): List<ScheduledInstallment> {
        require(count > 0) { "Installment count must be positive." }
        require(total.cents >= count.toLong()) {
            "The total must allow at least one cent per installment."
        }

        val base = total.cents / count
        val remainder = (total.cents % count).toInt()
        return buildList(count) {
            repeat(count) { index ->
                val cents = if (index >= count - remainder) base + 1 else base
                add(
                    ScheduledInstallment(
                        number = index + 1,
                        totalCount = count,
                        amount = Money(cents),
                        dueDate = dueDay.inMonth(firstDueMonth.plusMonths(index.toLong())),
                    ),
                )
            }
        }
    }
}
