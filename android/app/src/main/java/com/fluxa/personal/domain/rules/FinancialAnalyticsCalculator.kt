package com.fluxa.personal.domain.rules

import com.fluxa.personal.core.money.Money
import com.fluxa.personal.domain.model.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/** BigInteger.longValueExact is API31; retain exact conversion on the supported API30. */
internal fun exactAnalyticsMoney(cents: BigInteger): Money {
    if (cents.bitLength() > Long.SIZE_BITS - 1) throw ArithmeticException("Analytics amount exceeds Long cents")
    return Money(cents.toLong())
}

internal object FinancialAnalyticsCalculator {
    private fun Money.wide() = BigInteger.valueOf(cents)
    private fun BigInteger.money() = exactAnalyticsMoney(this)
    private fun ratio(numerator: Money, denominator: Money): BigDecimal =
        BigDecimal.valueOf(numerator.cents).divide(BigDecimal.valueOf(denominator.cents), MathContext.DECIMAL128)

    fun summary(month: YearMonth, totals: List<AnalyticsLedgerTotal>): MonthlyFinancialSummary {
        val rows = totals.filter { it.month == month }
        require(rows.all { it.amount.cents >= 0 && it.count >= 0 })
        fun total(direction: TransactionDirection) = rows.filter { it.direction == direction }
            .fold(BigInteger.ZERO) { sum, row -> sum + row.amount.wide() }.money()
        val income = total(TransactionDirection.INCOME)
        val expense = total(TransactionDirection.EXPENSE)
        val net = income - expense
        val count = rows.fold(0L) { sum, row -> Math.addExact(sum, row.count) }
        return MonthlyFinancialSummary(month, income, expense, net, count,
            if (income.cents > 0) ratio(net, income) else null)
    }

    fun evolution(month: YearMonth, totals: List<AnalyticsLedgerTotal>): List<CashFlowMonth> =
        (5 downTo 0).map { ago ->
            val s = summary(month.minusMonths(ago.toLong()), totals)
            CashFlowMonth(s.month, s.incomeRealized, s.expenseRealized, s.netResult)
        }

    fun categories(totals: List<AnalyticsCategoryTotal>): List<CategorySpending> {
        require(totals.all { it.amount.cents > 0 })
        require(totals.map { it.categoryId }.distinct().size == totals.size)
        if (totals.isEmpty()) return emptyList()
        val ordered = totals.sortedWith(compareByDescending<AnalyticsCategoryTotal> { it.amount }.thenBy { it.categoryId.toString() })
        val sum = ordered.fold(BigInteger.ZERO) { s, r -> s + r.amount.wide() }
        val shares = ordered.map { (it.amount.wide() * BigInteger.valueOf(100)).divideAndRemainder(sum) }
        val missing = 100 - shares.sumOf { it[0].toInt() }
        val winners = shares.indices.sortedWith(compareByDescending<Int> { shares[it][1] }
            .thenBy { ordered[it].categoryId.toString() }).take(missing).toSet()
        return ordered.mapIndexed { i, row ->
            CategorySpending(row.categoryId, row.categoryName, row.amount, shares[i][0].toInt() + if (i in winners) 1 else 0)
        }
    }

    /** No new projection algorithm: minimum, timing, deduplication and horizon belong to SafeToSpend. */
    fun projection(result: SafeToSpendResult): FinancialProjection {
        require(result.safetyMargin.cents == 0L)
        val ending = (result.availableNow.wide() + result.confirmedIncomeTotal.wide() - result.obligationsTotal.wide()).money()
        return FinancialProjection(result.availableNow, result.obligationsTotal, result.confirmedIncomeTotal,
            ending, result.lowestProjectedBalance, result.lowestSlackDate, result.horizonStart, result.horizonEndExclusive)
    }

    fun subscriptions(activeCount: Long, monthlyAmount: BigInteger, yearlyAmount: BigInteger): SubscriptionAnalytics {
        require(activeCount >= 0 && monthlyAmount.signum() >= 0 && yearlyAmount.signum() >= 0)
        val equivalent = BigDecimal(yearlyAmount).divide(BigDecimal.valueOf(12), 0, RoundingMode.HALF_UP).toBigIntegerExact()
        return SubscriptionAnalytics(activeCount, (monthlyAmount + equivalent).money(),
            (monthlyAmount * BigInteger.valueOf(12) + yearlyAmount).money())
    }

    fun insights(current: MonthlyFinancialSummary, previous: MonthlyFinancialSummary,
                 categories: List<CategorySpending>, today: LocalDate): List<FinancialInsight> {
        if (current.transactionCount == 0L) return emptyList()
        val result = mutableListOf<FinancialInsight>()
        if (current.netResult.cents < 0) result += FinancialInsight(FinancialInsightType.NEGATIVE_MONTH, current.netResult)
        // Whole-month comparisons only. A partial current month is not comparable to a completed one.
        if (current.month < YearMonth.from(today) && previous.month == current.month.minusMonths(1) &&
            previous.transactionCount > 0 && previous.expenseRealized.cents > 0 &&
            current.expenseRealized > previous.expenseRealized) {
            result += FinancialInsight(FinancialInsightType.EXPENSES_INCREASED, current.expenseRealized,
                previous.expenseRealized, comparisonPeriod = previous.month,
                changeRate = ratio(current.expenseRealized - previous.expenseRealized, previous.expenseRealized))
        }
        categories.firstOrNull()?.let {
            result += FinancialInsight(FinancialInsightType.TOP_CATEGORY, it.amount, categoryId = it.categoryId)
        }
        if (current.netResult.cents > 0 && current.incomeRealized.cents > 0)
            result += FinancialInsight(FinancialInsightType.POSITIVE_SAVINGS, current.netResult)
        if (current.expenseRealized.cents == 0L)
            result += FinancialInsight(FinancialInsightType.NO_EXPENSES, Money(0))
        return result.take(4)
    }
}
