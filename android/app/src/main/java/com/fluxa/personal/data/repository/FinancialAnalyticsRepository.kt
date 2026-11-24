package com.fluxa.personal.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.invalidationTrackerFlow
import androidx.room.withTransaction
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.database.dao.PERIOD_TOTAL_RADIX
import com.fluxa.personal.domain.model.*
import com.fluxa.personal.domain.rules.FinancialAnalyticsCalculator
import com.fluxa.personal.domain.rules.exactAnalyticsMoney
import java.math.BigInteger
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class FinancialAnalyticsRepository(
    private val database: FluxaDatabase,
    private val safeToSpend: SafeToSpendRepository,
    private val calendar: FinancialCalendar = FinancialCalendar(),
) {
    private val dao = database.analyticsDao()

    /** Null selects the financial current month at each emission. Projection always starts at asOfDate. */
    fun observeFinancialAnalytics(month: YearMonth? = null, horizonDays: Int = 30): Flow<FinancialAnalyticsState> {
        require(horizonDays > 0)
        month?.let(::boundaries)
        return database.invalidationTrackerFlow(
            "financial_transactions", "categories", "accounts", "goals", "goal_movements",
            "credit_cards", "credit_purchases", "credit_installments",
            "subscriptions", "subscription_terms", "subscription_resolutions",
        ).map {
            val today = calendar.today()
            read(month ?: YearMonth.from(today), today, horizonDays)
        }.distinctUntilChanged()
    }

    private fun boundaries(month: YearMonth): List<LocalDate> =
        (0..6).map { month.minusMonths(5).plusMonths(it.toLong()).atDay(1) }

    private fun wide(high: Long, low: Long): BigInteger =
        BigInteger.valueOf(high).multiply(BigInteger.valueOf(PERIOD_TOTAL_RADIX)).add(BigInteger.valueOf(low))
    private fun cents(high: Long, low: Long) = exactAnalyticsMoney(wide(high, low))

    private suspend fun read(month: YearMonth, today: LocalDate, horizonDays: Int): FinancialAnalyticsState =
        database.withTransaction {
            try {
                val days = boundaries(month).map { it.toEpochDay() }
                val first = month.minusMonths(5)
                val totals = dao.readMonths(days[0],days[1],days[2],days[3],days[4],days[5],days[6]).map {
                    AnalyticsLedgerTotal(first.plusMonths(it.bucket.toLong()), TransactionDirection.valueOf(it.direction),
                        cents(it.high,it.low), it.transactionCount)
                }
                val current = FinancialAnalyticsCalculator.summary(month, totals)
                val previous = FinancialAnalyticsCalculator.summary(month.minusMonths(1), totals)
                val categories = FinancialAnalyticsCalculator.categories(dao.readCategories(days[5], days[6]).map {
                    AnalyticsCategoryTotal(UUID.fromString(it.categoryId), it.categoryName, cents(it.high,it.low))
                })
                val budget = dao.readActiveSubscriptions()
                fun total(frequency: String) = budget.filter { it.frequency == frequency }
                    .fold(BigInteger.ZERO) { sum, row -> sum + wide(row.high,row.low) }
                val activeCount = budget.fold(0L) { sum, row -> Math.addExact(sum,row.activeCount) }
                val subscriptions = FinancialAnalyticsCalculator.subscriptions(activeCount,total("MONTHLY"),total("YEARLY"))
                val basis = safeToSpend.read(today, horizonDays)
                val remainingDays = ChronoUnit.DAYS.between(today, YearMonth.from(today).plusMonths(1).atDay(1)).toInt()
                val monthBasis = if (remainingDays == horizonDays) basis else safeToSpend.read(today, remainingDays)
                val unavailable = listOf(basis, monthBasis).filterIsInstance<SafeToSpendState.Unavailable>().firstOrNull()
                if (unavailable != null) return@withTransaction FinancialAnalyticsState.Unavailable(
                    when (unavailable.error) {
                        SafeToSpendError.OVERFLOW -> FinancialAnalyticsError.OVERFLOW
                        SafeToSpendError.CONFLICTING_EVENTS -> FinancialAnalyticsError.CONFLICTING_EVENTS
                    }
                )
                FinancialAnalyticsState.Ready(FinancialAnalyticsSnapshot(today,current,categories,
                    FinancialAnalyticsCalculator.evolution(month,totals),
                    FinancialAnalyticsCalculator.projection((basis as SafeToSpendState.Ready).result),
                    FinancialAnalyticsCalculator.projection((monthBasis as SafeToSpendState.Ready).result),
                    subscriptions, FinancialAnalyticsCalculator.insights(current,previous,categories,today)))
            } catch (_: ArithmeticException) {
                FinancialAnalyticsState.Unavailable(FinancialAnalyticsError.OVERFLOW)
            } catch (error: SQLiteException) {
                if (error.message?.contains("integer overflow", ignoreCase = true) != true) throw error
                FinancialAnalyticsState.Unavailable(FinancialAnalyticsError.OVERFLOW)
            }
        }
}
