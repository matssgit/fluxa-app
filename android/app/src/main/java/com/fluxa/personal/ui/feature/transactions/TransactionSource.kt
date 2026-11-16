package com.fluxa.personal.ui.feature.transactions

import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.PeriodSummary
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.model.TransactionHistoryPage
import com.fluxa.personal.domain.model.TransactionResult
import com.fluxa.personal.domain.rules.BalanceSnapshot
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Recorte do [TransactionRepository] usado pela UI. Existe só para os ViewModels poderem ser testados na JVM
 * (o repositório real depende de `RoomDatabase.withTransaction`). Não acrescenta regra: em produção cada
 * método delega diretamente ao repositório do Codex.
 */
internal interface TransactionSource {
    suspend fun create(id: UUID, draft: TransactionDraft): TransactionResult
    suspend fun update(id: UUID, draft: TransactionDraft): TransactionResult
    suspend fun cancel(id: UUID): TransactionResult
    fun observe(id: UUID): Flow<FinancialTransaction?>
    fun observeHistory(filter: TransactionHistoryFilter): Flow<List<FinancialTransaction>>
    fun observeHistoryPage(filter: TransactionHistoryFilter, page: TransactionHistoryPage): Flow<List<FinancialTransaction>>
    fun observeRecentTransactions(limit: Int): Flow<List<FinancialTransaction>>
    fun observePeriodSummary(startInclusive: LocalDate, endExclusive: LocalDate): Flow<PeriodSummary>
    fun observeBalances(): Flow<BalanceSnapshot>
}

internal class RepositoryTransactionSource(private val repository: TransactionRepository) : TransactionSource {
    override suspend fun create(id: UUID, draft: TransactionDraft) = repository.create(id, draft)
    override suspend fun update(id: UUID, draft: TransactionDraft) = repository.update(id, draft)
    override suspend fun cancel(id: UUID) = repository.cancel(id)
    override fun observe(id: UUID) = repository.observe(id)
    override fun observeHistory(filter: TransactionHistoryFilter) = repository.observeHistory(filter)
    override fun observeHistoryPage(filter: TransactionHistoryFilter, page: TransactionHistoryPage) =
        repository.observeHistoryPage(filter, page)
    override fun observeRecentTransactions(limit: Int) = repository.observeRecentTransactions(limit)
    override fun observePeriodSummary(startInclusive: LocalDate, endExclusive: LocalDate) =
        repository.observePeriodSummary(startInclusive, endExclusive)
    override fun observeBalances() = repository.observeBalances()
}
