package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.ProjectedCashEvent
import com.fluxa.personal.domain.model.ProjectedCashEventType
import com.fluxa.personal.domain.model.ProjectedCashSource
import com.fluxa.personal.domain.model.SafeToSpendError
import com.fluxa.personal.domain.model.SafeToSpendResult
import com.fluxa.personal.domain.model.SafeToSpendState
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class SafeToSpendCalculatorTest {
    private val today = LocalDate.of(2026, 11, 19)

    @Test
    fun noEventsUsesAvailableAlreadyNetOfReserves() {
        val result = ready(350_000)
        assertEquals(Money(350_000), result.safeAmount)
        assertEquals(Money(350_000), result.availableNow)
        assertEquals(Money(0), result.obligationsTotal)
        assertEquals(today, result.lowestSlackDate)
        assertTrue(result.breakdown.isEmpty())
    }

    @Test
    fun minimumBeforeLaterIncomeDeterminesWhatCanBeSpentToday() {
        val events = listOf(obligation(1, 50_000), obligation(5, 90_000), income(10, 100_000))
        val result = ready(200_000, events)
        assertEquals(Money(60_000), result.safeAmount)
        assertEquals(Money(60_000), result.lowestProjectedBalance)
        assertEquals(today.plusDays(5), result.lowestSlackDate)
        assertEquals(events.take(2), result.breakdown)
        assertEquals(Money(140_000), result.obligationsTotal)
        assertEquals(Money(100_000), result.confirmedIncomeTotal)
    }

    @Test
    fun laterIncomeCannotFinanceEarlierObligation() {
        val result = ready(50_000, listOf(obligation(1, 40_000), income(2, 100_000)))
        assertEquals(Money(10_000), result.safeAmount)
        assertEquals(today.plusDays(1), result.lowestSlackDate)
        assertEquals(1, result.breakdown.size)
    }

    @Test
    fun sameDayObligationsPrecedeIncomeAndBreakdownStopsAtTheMinimum() {
        val expense = obligation(0, 40_000)
        val result = ready(50_000, listOf(income(0, 100_000), expense))
        assertEquals(Money(10_000), result.safeAmount)
        assertEquals(today, result.lowestSlackDate)
        assertEquals(listOf(expense), result.breakdown)
    }

    @Test
    fun earlierConfirmedIncomeCanFundLaterObligationsButCannotIncreaseTodaysBase() {
        val result = ready(50_000, listOf(income(1, 100_000), obligation(2, 90_000)))
        assertEquals(Money(50_000), result.safeAmount)
        assertEquals(today, result.lowestSlackDate)
        assertTrue(result.breakdown.isEmpty())
        val lower = ready(50_000, listOf(income(1, 100_000), obligation(2, 120_000)))
        assertEquals(Money(30_000), lower.safeAmount)
        assertEquals(2, lower.breakdown.size)
    }

    @Test
    fun overdueObligationsApplyTodayWhilePastIncomeIsAlreadyInCash() {
        val result = ready(100_000, listOf(obligation(-100, 30_000), income(-1, 900_000)))
        assertEquals(Money(70_000), result.safeAmount)
        assertEquals(today, result.lowestSlackDate)
        assertEquals(today, result.breakdown.single().date)
        assertEquals(Money(0), result.confirmedIncomeTotal)
    }

    @Test
    fun negativeAvailabilityAndInsolvencyAreValidResultsClampedToZero() {
        val negative = ready(-50_000)
        assertEquals(Money(0), negative.safeAmount)
        assertEquals(Money(-50_000), negative.availableNow)
        assertEquals(Money(-50_000), negative.lowestProjectedBalance)
        val insolvent = ready(50_000, listOf(obligation(1, 70_000)))
        assertEquals(Money(0), insolvent.safeAmount)
        assertEquals(Money(-20_000), insolvent.lowestProjectedBalance)
        assertEquals(Money(0), ready(0).safeAmount)
    }

    @Test
    fun horizonIncludesLastDayAndExcludesEndBoundary() {
        val result = ready(100_000, listOf(obligation(29, 10_000), obligation(30, 999_999)))
        assertEquals(Money(90_000), result.safeAmount)
        assertEquals(today.plusDays(30), result.horizonEndExclusive)
        assertEquals(Money(10_000), result.obligationsTotal)
        val singleDay = SafeToSpendCalculator.calculate(Money(100_000), listOf(obligation(1, 10_000)), today, horizonDays = 1)
        assertEquals(Money(100_000), (singleDay as SafeToSpendState.Ready).result.safeAmount)
    }

    @Test
    fun multipleEventsAreStableRegardlessOfInputOrdering() {
        val events = listOf(obligation(1, 10_000), income(1, 50_000), obligation(1, 30_000))
        assertEquals(ready(100_000, events), ready(100_000, events.reversed()))
        assertEquals(Money(60_000), ready(100_000, events).safeAmount)
    }

    @Test
    fun identicalEventIdentityIsCountedOnceAndConflictingVersionsAreRejected() {
        val event = obligation(1, 30_000)
        assertEquals(Money(70_000), ready(100_000, listOf(event, event)).safeAmount)
        assertEquals(SafeToSpendState.Unavailable(SafeToSpendError.CONFLICTING_EVENTS),
            SafeToSpendCalculator.calculate(Money(100_000), listOf(event, event.copy(amount = Money(1))), today))
    }

    @Test
    fun safetyMarginIsAppliedOnceBeforeTemporalProjection() {
        val result = SafeToSpendCalculator.calculate(
            Money(100_000), listOf(obligation(1, 30_000)), today, safetyMargin = Money(20_000),
        ) as SafeToSpendState.Ready
        assertEquals(Money(50_000), result.result.safeAmount)
        assertEquals(Money(20_000), result.result.safetyMargin)
        assertEquals(Money(100_000), result.result.availableNow)
    }

    @Test
    fun overflowIsExplicitAndWideIntermediateArithmeticCanRecover() {
        val events = listOf(obligation(0, Long.MAX_VALUE), obligation(0, Long.MAX_VALUE))
        assertEquals(SafeToSpendState.Unavailable(SafeToSpendError.OVERFLOW),
            SafeToSpendCalculator.calculate(Money(0), events, today))
        assertEquals(SafeToSpendState.Unavailable(SafeToSpendError.OVERFLOW),
            SafeToSpendCalculator.calculate(Money(Long.MIN_VALUE), listOf(obligation(1, 1)), today))
        val recovered = ready(Long.MAX_VALUE, listOf(income(1, 1), obligation(2, Long.MAX_VALUE)))
        assertEquals(Money(1), recovered.safeAmount)
    }

    @Test
    fun equalMinimumKeepsTheEarliestDateAndExcludesLaterEventsFromExplanation() {
        val first = obligation(1, 10_000)
        val result = ready(50_000, listOf(first, income(2, 10_000), obligation(3, 10_000)))
        assertEquals(today.plusDays(1), result.lowestSlackDate)
        assertEquals(listOf(first), result.breakdown)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidHorizonIsRejected() {
        SafeToSpendCalculator.calculate(Money(0), emptyList(), today, horizonDays = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeSafetyMarginIsRejected() {
        SafeToSpendCalculator.calculate(Money(0), emptyList(), today, safetyMargin = Money(-1))
    }

    private fun ready(cents: Long, events: List<ProjectedCashEvent> = emptyList()): SafeToSpendResult =
        (SafeToSpendCalculator.calculate(Money(cents), events, today) as SafeToSpendState.Ready).result

    private fun obligation(days: Long, amount: Long) = ProjectedCashEvent(
        today.plusDays(days), ProjectedCashEventType.OBLIGATION, Money(amount),
        ProjectedCashSource.Installment(UUID.randomUUID(), UUID(0, 1), UUID(0, 2)),
    )

    private fun income(days: Long, amount: Long) = ProjectedCashEvent(
        today.plusDays(days), ProjectedCashEventType.CONFIRMED_INCOME, Money(amount),
        ProjectedCashSource.ConfirmedIncome(UUID.randomUUID()),
    )
}
