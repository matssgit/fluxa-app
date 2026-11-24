package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test

class FinancialAnalyticsCalculatorTest {
    private val c = FinancialAnalyticsCalculator
    private val month = YearMonth.of(2024, 1)
    private fun row(direction: TransactionDirection, cents: Long, date: YearMonth = month, count: Long = 1) =
        AnalyticsLedgerTotal(date, direction, Money(cents), count)
    private fun category(id: Int, cents: Long) = AnalyticsCategoryTotal(UUID(0, id.toLong()), "Categoria$id", Money(cents))
    private fun summary(income: Long, expense: Long, date: YearMonth = month) =
        c.summary(date, listOf(row(TransactionDirection.INCOME,income,date), row(TransactionDirection.EXPENSE,expense,date)))

    @Test fun summaryKeepsNegativeNetAndSignedSavingsRate() {
        val s = summary(10000,15000)
        assertEquals(Money(-5000),s.netResult)
        assertEquals(BigDecimal("-0.5"),s.savingsRate)
        assertEquals(2L,s.transactionCount)
    }
    @Test fun noIncomeHasNoSavingsRateAndEmptyMonthsAreZero() {
        val s = c.summary(month,listOf(row(TransactionDirection.EXPENSE,500)))
        assertNull(s.savingsRate)
        assertEquals(Money(-500),s.netResult)
        assertEquals(Money(0),c.summary(month,emptyList()).incomeRealized)
        assertEquals(0L,c.summary(month,emptyList()).transactionCount)
    }
    @Test fun savingsRoundingIsExplicitAndNeverClamped() {
        assertEquals(BigDecimal("0.6666666666666666666666666666666667"),summary(3,1).savingsRate)
        assertEquals(BigDecimal("1"),summary(100,0).savingsRate)
    }
    @Test fun tinyNegativeSavingsAndTinyExpenseIncreaseKeepTheirSigns() {
        val s=summary(Long.MAX_VALUE-1,Long.MAX_VALUE)
        assertEquals(Money(-1),s.netResult)
        assertEquals(-1,requireNotNull(s.savingsRate).signum())
        val previous=summary(1,Long.MAX_VALUE-1,month.minusMonths(1))
        val growth=c.insights(s,previous,emptyList(),LocalDate.of(2024,2,1)).single { it.type==FinancialInsightType.EXPENSES_INCREASED }
        assertEquals(1,requireNotNull(growth.changeRate).signum())
    }
    @Test fun categoriesUseLargestRemainderStableIdsAndPreserveEveryCent() {
        val inputs=listOf(category(3,1),category(1,1),category(2,1))
        val result=c.categories(inputs)
        assertEquals(listOf(34,33,33),result.map { it.percentage })
        assertEquals(listOf(UUID(0,1),UUID(0,2),UUID(0,3)),result.map { it.categoryId })
        assertEquals(3L,result.sumOf { it.amount.cents })
        assertEquals(result,c.categories(inputs.reversed()))
    }
    @Test fun smallCategoriesCanHaveZeroShareButTotalIs100() {
        val result=c.categories((1..103).map { category(it,1) })
        assertEquals(100,result.sumOf { it.percentage })
        assertEquals(3,result.count { it.percentage == 0 })
        assertTrue(c.categories(emptyList()).isEmpty())
    }
    @Test fun categoryDistributionDoesNotOverflowMultiplication() {
        assertEquals(listOf(50,50),c.categories(listOf(category(1,Long.MAX_VALUE),category(2,Long.MAX_VALUE))).map { it.percentage })
    }
    @Test fun evolutionIsChronologicalContinuousAcrossYearBoundary() {
        val series=c.evolution(month,listOf(row(TransactionDirection.EXPENSE,100),
            row(TransactionDirection.INCOME,300,YearMonth.of(2023,12))))
        assertEquals(6,series.size)
        assertEquals(YearMonth.of(2023,8),series.first().month)
        assertEquals(Money(0),series.first().net)
        assertEquals(Money(300),series[4].net)
        assertEquals(Money(-100),series.last().net)
    }
    private val today = LocalDate.of(2024,2,29)
    private fun event(id: Int, date: LocalDate, type: ProjectedCashEventType, cents: Long) =
        ProjectedCashEvent(date,type,Money(cents),if (type == ProjectedCashEventType.CONFIRMED_INCOME) ProjectedCashSource.ConfirmedIncome(UUID(0,id.toLong()))
            else ProjectedCashSource.Installment(UUID(0,id.toLong()),UUID(0,100),UUID(0,200)))
    private fun project(events: List<ProjectedCashEvent>, days: Int = 30): FinancialProjection {
        val basis=SafeToSpendCalculator.calculate(Money(350000),events,today,days) as SafeToSpendState.Ready
        return c.projection(basis.result)
    }
    @Test fun projectionStartsWithAvailableAndDoesNotDeductReserveAgain() {
        val p=project(emptyList())
        assertEquals(Money(350000),p.availableNow)
        assertEquals(Money(350000),p.projectedEndingBalance)
        assertEquals(Money(350000),p.lowestProjectedBalance)
        assertEquals(today,p.lowestBalanceDate)
    }
    @Test fun overdueIsTodayLaterIncomeDoesNotRepairEarlierMinimumAndHorizonIsExclusive() {
        val p=project(listOf(
            event(1,today.minusDays(20),ProjectedCashEventType.OBLIGATION,400000),
            event(2,today.plusDays(2),ProjectedCashEventType.CONFIRMED_INCOME,200000),
            event(3,today.plusDays(30),ProjectedCashEventType.OBLIGATION,123),
            event(4,today.minusDays(1),ProjectedCashEventType.CONFIRMED_INCOME,999)))
        assertEquals(Money(150000),p.projectedEndingBalance)
        assertEquals(Money(-50000),p.lowestProjectedBalance)
        assertEquals(today,p.lowestBalanceDate)
        assertEquals(Money(400000),p.obligationsTotal)
        assertEquals(Money(200000),p.confirmedIncomeTotal)
        assertEquals(LocalDate.of(2024,3,30),p.horizonEndExclusive)
    }
    @Test fun sameDayObligationsPrecedeIncomeAndDuplicatesUseSharedRules() {
        val expense=event(1,today,ProjectedCashEventType.OBLIGATION,400000)
        val p=project(listOf(event(2,today,ProjectedCashEventType.CONFIRMED_INCOME,400000),expense,expense))
        assertEquals(Money(350000),p.projectedEndingBalance)
        assertEquals(Money(-50000),p.lowestProjectedBalance)
        assertEquals(Money(400000),p.obligationsTotal)
    }
    @Test fun monthEndUsesSameProjectionPipelineInLeapFebruary() {
        val p=project(listOf(event(1,today.plusDays(1),ProjectedCashEventType.OBLIGATION,100)),1)
        assertEquals(LocalDate.of(2024,3,1),p.horizonEndExclusive)
        assertEquals(Money(0),p.obligationsTotal)
    }
    @Test fun annualBudgetRoundsAggregateOnceAndAnnualProjectionRemainsExact() {
        val b=c.subscriptions(3,BigInteger.valueOf(1000),BigInteger.valueOf(1207))
        assertEquals(Money(1101),b.monthlyEquivalentCost)
        assertEquals(Money(13207),b.annualProjectedCost)
        assertEquals(3L,b.activeCount)
    }
    @Test(expected=ArithmeticException::class) fun budgetOverflowIsNeverWrapped() {
        c.subscriptions(1,BigInteger.valueOf(Long.MAX_VALUE),BigInteger.ZERO)
    }
    @Test(expected=ArithmeticException::class) fun projectionEndingOverflowIsDetectedEvenWhenMinimumFits() {
        val basis=SafeToSpendCalculator.calculate(Money(Long.MAX_VALUE),
            listOf(event(1,today,ProjectedCashEventType.CONFIRMED_INCOME,1)),today) as SafeToSpendState.Ready
        c.projection(basis.result)
    }
    @Test fun negativeAndTopCategoryAreFactsWithoutTextOrScore() {
        val facts=c.insights(summary(100,200),summary(100,100,month.minusMonths(1)),
            c.categories(listOf(category(1,200))),LocalDate.of(2024,2,1))
        assertEquals(listOf(FinancialInsightType.NEGATIVE_MONTH,FinancialInsightType.EXPENSES_INCREASED,
            FinancialInsightType.TOP_CATEGORY),facts.map { it.type })
        assertEquals(BigDecimal("1"),facts[1].changeRate)
        assertEquals(month.minusMonths(1),facts[1].comparisonPeriod)
    }
    @Test fun zeroPreviousBasePartialMonthAndNoDataNeverInventGrowth() {
        assertFalse(c.insights(summary(100,200),summary(0,0,month.minusMonths(1)),emptyList(),
            LocalDate.of(2024,2,1)).any { it.type == FinancialInsightType.EXPENSES_INCREASED })
        assertFalse(c.insights(summary(100,200),summary(100,100,month.minusMonths(1)),emptyList(),
            month.atDay(20)).any { it.type == FinancialInsightType.EXPENSES_INCREASED })
        assertTrue(c.insights(c.summary(month,emptyList()),summary(1,1),emptyList(),today).isEmpty())
    }
    @Test fun positiveIncomeOnlyHasSavingsAndNoExpenseFacts() {
        val facts=c.insights(summary(100,0),c.summary(month.minusMonths(1),emptyList()),emptyList(),today)
        assertEquals(listOf(FinancialInsightType.POSITIVE_SAVINGS,FinancialInsightType.NO_EXPENSES),facts.map { it.type })
    }
}
