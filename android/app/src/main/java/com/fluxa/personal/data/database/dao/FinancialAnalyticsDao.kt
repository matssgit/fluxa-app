package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Query

internal data class AnalyticsMonthRow(
    val bucket: Int, val direction: String, val high: Long, val low: Long, val transactionCount: Long,
)
internal data class AnalyticsCategoryRow(val categoryId: String, val categoryName: String, val high: Long, val low: Long)
internal data class AnalyticsSubscriptionRow(val frequency: String, val high: Long, val low: Long, val activeCount: Long)

@Dao
internal interface FinancialAnalyticsDao {
    // Epoch-day ranges are computed from LocalDate/YearMonth, never SQLite UTC date functions.
    // Split integer sums retain exact cents even when the complete total exceeds SQLite SUM's Long.
    @Query("""
        WITH months(bucket, startDay, endDay) AS (
            VALUES (0,:m0,:m1),(1,:m1,:m2),(2,:m2,:m3),
                   (3,:m3,:m4),(4,:m4,:m5),(5,:m5,:m6)
        )
        SELECT months.bucket, t.direction,
               SUM(t.amountCents / $PERIOD_TOTAL_RADIX) AS high,
               SUM(t.amountCents % $PERIOD_TOTAL_RADIX) AS low,
               COUNT(*) AS transactionCount
        FROM months JOIN financial_transactions t
          ON t.occurredOn >= months.startDay AND t.occurredOn < months.endDay
        WHERE t.cancelledAt IS NULL
        GROUP BY months.bucket,t.direction
    """)
    suspend fun readMonths(m0: Long, m1: Long, m2: Long, m3: Long, m4: Long, m5: Long, m6: Long): List<AnalyticsMonthRow>

    @Query("""
        SELECT t.categoryId, c.name AS categoryName,
               SUM(t.amountCents / $PERIOD_TOTAL_RADIX) AS high,
               SUM(t.amountCents % $PERIOD_TOTAL_RADIX) AS low
        FROM financial_transactions t JOIN categories c ON c.id = t.categoryId
        WHERE t.cancelledAt IS NULL AND t.direction = 'EXPENSE'
          AND t.occurredOn >= :startDay AND t.occurredOn < :endDay
        GROUP BY t.categoryId,c.name
    """)
    suspend fun readCategories(startDay: Long, endDay: Long): List<AnalyticsCategoryRow>

    @Query("""
        SELECT frequency, SUM(amountCents / $PERIOD_TOTAL_RADIX) AS high,
               SUM(amountCents % $PERIOD_TOTAL_RADIX) AS low, COUNT(*) AS activeCount
        FROM subscriptions WHERE status = 'ACTIVE'
        GROUP BY frequency
    """)
    suspend fun readActiveSubscriptions(): List<AnalyticsSubscriptionRow>
}
