package com.fluxa.personal.data.repository

import androidx.room.withTransaction
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.database.mapper.toBalanceInputs
import com.fluxa.personal.data.database.mapper.toDomain
import com.fluxa.personal.data.database.mapper.toEntity
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.model.TransactionResult
import com.fluxa.personal.domain.rules.BalanceCalculator
import com.fluxa.personal.domain.rules.BalanceSnapshot
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.domain.rules.TransactionRules
import java.time.Clock
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
        dao.observeHistory(
            filter.accountId?.toString(),
            filter.categoryId?.toString(),
            filter.fromInclusive?.toEpochDay(),
            filter.untilExclusive?.toEpochDay(),
            filter.includeCancelled,
        ).map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    fun observeBalances(): Flow<BalanceSnapshot> =
        dao.observeBalanceRows().map { BalanceCalculator.calculate(it.toBalanceInputs()) }.distinctUntilChanged()

    fun observeAccountBalance(id: UUID): Flow<CalculatedBalance?> =
        observeBalances().map { it.accounts[id] }.distinctUntilChanged()

    fun observeAvailableBalance(): Flow<CalculatedBalance> =
        observeBalances().map { it.available }.distinctUntilChanged()

    private fun rules() = TransactionRules(calendar.today(), clock.instant())
}
