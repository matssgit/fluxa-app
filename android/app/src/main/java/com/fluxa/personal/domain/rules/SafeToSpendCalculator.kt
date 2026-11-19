package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.ProjectedCashEvent
import com.fluxa.personal.domain.model.ProjectedCashEventType
import com.fluxa.personal.domain.model.ProjectedCashSource
import com.fluxa.personal.domain.model.SafeToSpendError
import com.fluxa.personal.domain.model.SafeToSpendResult
import com.fluxa.personal.domain.model.SafeToSpendState
import java.math.BigInteger
import java.time.LocalDate

internal object SafeToSpendCalculator {
    fun calculate(
        availableNow: Money,
        events: List<ProjectedCashEvent>,
        today: LocalDate,
        horizonDays: Int = 30,
        safetyMargin: Money = Money(0),
    ): SafeToSpendState {
        require(horizonDays > 0) { "The horizon must contain at least one day." }
        require(safetyMargin.cents >= 0) { "The safety margin must not be negative." }
        val end = today.plusDays(horizonDays.toLong())
        val relevant = events.filter {
            it.date < end && (it.type == ProjectedCashEventType.OBLIGATION || it.date >= today)
        }
        val bySource = relevant.groupBy { it.source.stableKey() }
        if (bySource.values.any { versions -> versions.distinct().size > 1 }) {
            return SafeToSpendState.Unavailable(SafeToSpendError.CONFLICTING_EVENTS)
        }
        val ordered = relevant.distinctBy { it.source.stableKey() }
            .map { it.copy(date = maxOf(it.date, today)) }
            .sortedWith(compareBy({ it.date }, { it.type.ordinal }, { it.source.stableKey() }))
        var projected = availableNow.wide() - safetyMargin.wide()
        var minimum = projected
        var lowestDate = today
        var minimumEventCount = 0
        var obligations = BigInteger.ZERO
        var income = BigInteger.ZERO
        for ((index, event) in ordered.withIndex()) {
            when (event.type) {
                ProjectedCashEventType.OBLIGATION -> {
                    obligations += event.amount.wide()
                    projected -= event.amount.wide()
                }
                ProjectedCashEventType.CONFIRMED_INCOME -> {
                    income += event.amount.wide()
                    projected += event.amount.wide()
                }
            }
            if (projected < minimum) {
                minimum = projected
                lowestDate = event.date
                minimumEventCount = index + 1
            }
        }
        if (listOf(minimum, obligations, income).any { it.bitLength() > Long.SIZE_BITS - 1 }) {
            return SafeToSpendState.Unavailable(SafeToSpendError.OVERFLOW)
        }
        return SafeToSpendState.Ready(SafeToSpendResult(
            Money(minimum.max(BigInteger.ZERO).toLong()), availableNow, safetyMargin, Money(minimum.toLong()),
            lowestDate, today, end, Money(obligations.toLong()), Money(income.toLong()),
            ordered.take(minimumEventCount),
        ))
    }

    private fun ProjectedCashSource.stableKey(): String = when (this) {
        is ProjectedCashSource.Installment -> "installment:$installmentId"
        is ProjectedCashSource.Subscription -> "subscription:${key.subscriptionId}:${key.occurrenceDate}"
        is ProjectedCashSource.ConfirmedIncome -> "income:$id"
    }
}
