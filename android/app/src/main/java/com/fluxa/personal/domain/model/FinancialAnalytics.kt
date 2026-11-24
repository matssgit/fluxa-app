package com.fluxa.personal.domain.model

import com.fluxa.personal.core.money.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal data class MonthlyFinancialSummary(
    val month: YearMonth,
    val incomeRealized: Money,
    val expenseRealized: Money,
    val netResult: Money,
    val transactionCount: Long,
    /** Signed DECIMAL128 ratio: 0.25 means 25%; null when income is zero. Never quantize tiny losses to zero. */
    val savingsRate: BigDecimal?,
)
internal data class CategorySpending(
    val categoryId: UUID, val categoryName: String, val amount: Money,
    /** Integer percentage; largest remainders reconcile the complete list to 100. */
    val percentage: Int,
)
internal data class CashFlowMonth(val month: YearMonth, val income: Money, val expense: Money, val net: Money)
internal data class FinancialProjection(
    val availableNow: Money, val obligationsTotal: Money, val confirmedIncomeTotal: Money,
    val projectedEndingBalance: Money, val lowestProjectedBalance: Money, val lowestBalanceDate: LocalDate,
    val horizonStart: LocalDate, val horizonEndExclusive: LocalDate,
)
internal data class SubscriptionAnalytics(
    val activeCount: Long,
    /** Budget equivalent, not a cash event. Annual totals are divided once, HALF_UP to cents. */
    val monthlyEquivalentCost: Money,
    val annualProjectedCost: Money,
)
internal enum class FinancialInsightType { NEGATIVE_MONTH, EXPENSES_INCREASED, TOP_CATEGORY, POSITIVE_SAVINGS, NO_EXPENSES }
internal data class FinancialInsight(
    val type: FinancialInsightType,
    val primaryValue: Money? = null,
    val secondaryValue: Money? = null,
    val categoryId: UUID? = null,
    val comparisonPeriod: YearMonth? = null,
    /** Ratio, not a monetary value. */
    val changeRate: BigDecimal? = null,
)
internal data class FinancialAnalyticsSnapshot(
    val asOfDate: LocalDate,
    val monthSummary: MonthlyFinancialSummary,
    val categorySpending: List<CategorySpending>,
    val sixMonthEvolution: List<CashFlowMonth>,
    val projection: FinancialProjection,
    val remainingMonthProjection: FinancialProjection,
    val subscriptionAnalytics: SubscriptionAnalytics,
    val insights: List<FinancialInsight>,
) {
    /** Percentages remain shares of ALL spending; the top five are not renormalized. */
    val topCategories: List<CategorySpending> get() = categorySpending.take(5)
}
internal enum class FinancialAnalyticsError { OVERFLOW, CONFLICTING_EVENTS }
internal sealed interface FinancialAnalyticsState {
    data class Ready(val snapshot: FinancialAnalyticsSnapshot) : FinancialAnalyticsState
    data class Unavailable(val error: FinancialAnalyticsError) : FinancialAnalyticsState
}

/** Pure input contracts; Room aggregates before mapping into these types. */
internal data class AnalyticsLedgerTotal(val month: YearMonth, val direction: TransactionDirection, val amount: Money, val count: Long)
internal data class AnalyticsCategoryTotal(val categoryId: UUID, val categoryName: String, val amount: Money)
