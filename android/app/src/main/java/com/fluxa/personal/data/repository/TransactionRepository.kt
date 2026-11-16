package com.fluxa.personal.data.repository

import android.database.sqlite.SQLiteException
import androidx.room.invalidationTrackerFlow
import androidx.room.withTransaction
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.database.dao.PERIOD_TOTAL_RADIX
import com.fluxa.personal.data.database.mapper.toBalanceInputs
import com.fluxa.personal.data.database.mapper.toDomain
import com.fluxa.personal.data.database.mapper.toEntity
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.PeriodSummary
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.model.TransactionHistoryPage
import com.fluxa.personal.domain.model.TransactionResult
import com.fluxa.personal.domain.rules.BalanceCalculator
import com.fluxa.personal.domain.rules.BalanceSnapshot
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.domain.rules.TransactionRules
import java.math.BigInteger
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class TransactionRepository(
    private val database: FluxaDatabase,
    private val clock: Clock = Clock.systemUTC(),
    private val calendar: FinancialCalendar = FinancialCalendar(clock),
) {
    private val dao = database.transactionDao()

    // The caller retains the same ID for retries of one submission.
    suspend fun create(id: UUID, draft: TransactionDraft): TransactionResult = database.withTransaction {
        val result = rules().create(
            id, dao.find(id.toString())?.toDomain(), draft,
            dao.findAccount(draft.accountId.toString())?.toDomain(),
            dao.findCategory(draft.categoryId.toString())?.toDomain(),
        )
        if (result is TransactionResult.Saved) dao.insert(result.transaction.toEntity())
        result
    }

    suspend fun update(id: UUID, draft: TransactionDraft): TransactionResult = database.withTransaction {
        val result = rules().edit(
            dao.find(id.toString())?.toDomain(), draft,
            dao.findAccount(draft.accountId.toString())?.toDomain(),
            dao.findCategory(draft.categoryId.toString())?.toDomain(),
        )
        if (result is TransactionResult.Saved) dao.update(result.transaction.toEntity())
        result
    }

    suspend fun cancel(id: UUID): TransactionResult = database.withTransaction {
        val result = rules().cancel(dao.find(id.toString())?.toDomain())
        if (result is TransactionResult.Saved) dao.update(result.transaction.toEntity())
        result
    }

    fun observe(id: UUID): Flow<FinancialTransaction?> =
        dao.observe(id.toString()).map { it?.toDomain() }.distinctUntilChanged()

    fun observeHistory(filter: TransactionHistoryFilter = TransactionHistoryFilter()): Flow<List<FinancialTransaction>> =
        history(filter, limit = -1, offset = 0)

    fun observeHistoryPage(
        filter: TransactionHistoryFilter,
        page: TransactionHistoryPage,
    ): Flow<List<FinancialTransaction>> = history(filter, page.limit, page.offset)

    private fun history(filter: TransactionHistoryFilter, limit: Int, offset: Int): Flow<List<FinancialTransaction>> =
        dao.observeHistory(
            accountId = filter.accountId?.toString(),
            categoryId = filter.categoryId?.toString(),
            fromInclusive = filter.fromInclusive?.toEpochDay() ?: Long.MIN_VALUE,
            untilExclusive = filter.untilExclusive?.toEpochDay() ?: Long.MAX_VALUE,
            includeCancelled = filter.includeCancelled,
            direction = filter.direction?.name,
            limit = limit,
            offset = offset,
        ).map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    fun observeRecentTransactions(limit: Int): Flow<List<FinancialTransaction>> {
        require(limit > 0) { "Recent transactions limit must be positive." }
        return dao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()
    }

    fun observePeriodSummary(startInclusive: LocalDate, endExclusive: LocalDate): Flow<PeriodSummary> {
        require(startInclusive <= endExclusive) { "The end of the period must not precede the start." }
        val start = startInclusive.toEpochDay()
        val end = endExclusive.toEpochDay()
        return database.invalidationTrackerFlow("financial_transactions")
            .map { readPeriodSummary(start, end) }
            .distinctUntilChanged()
    }

    private suspend fun readPeriodSummary(start: Long, end: Long): PeriodSummary {
        val totals = try {
            dao.readPeriodTotals(start, end).associate { it.direction to BigInteger.valueOf(it.cents) }
        } catch (exception: SQLiteException) {
            if (!exception.isIntegerOverflow()) throw exception
            try {
                val radix = BigInteger.valueOf(PERIOD_TOTAL_RADIX)
                dao.readWidePeriodTotals(start, end).associate {
                    it.direction to BigInteger.valueOf(it.high).multiply(radix).add(BigInteger.valueOf(it.low))
                }
            } catch (wideException: SQLiteException) {
                if (!wideException.isIntegerOverflow()) throw wideException
                return PeriodSummary.Overflow
            }
        }
        return PeriodSummary.fromCents(
            income = totals[TransactionDirection.INCOME.name] ?: BigInteger.ZERO,
            expenses = totals[TransactionDirection.EXPENSE.name] ?: BigInteger.ZERO,
        )
    }

    private fun SQLiteException.isIntegerOverflow() = message?.contains("integer overflow", ignoreCase = true) == true

    fun observeBalances(): Flow<BalanceSnapshot> =
        dao.observeBalanceRows().map { BalanceCalculator.calculate(it.toBalanceInputs()) }.distinctUntilChanged()

    fun observeAccountBalance(id: UUID): Flow<CalculatedBalance?> =
        observeBalances().map { it.accounts[id] }.distinctUntilChanged()

    fun observeAvailableBalance(): Flow<CalculatedBalance> =
        observeBalances().map { it.available }.distinctUntilChanged()

    private fun rules() = TransactionRules(calendar.today(), clock.instant())
}
