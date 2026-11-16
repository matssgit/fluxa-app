package com.fluxa.personal.ui.feature

import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.TransactionDraft
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.model.TransactionResult
import com.fluxa.personal.domain.rules.AccountBalanceInput
import com.fluxa.personal.domain.rules.BalanceCalculator
import com.fluxa.personal.domain.rules.BalanceMovement
import com.fluxa.personal.domain.rules.BalanceSnapshot
import com.fluxa.personal.domain.rules.TransactionRules
import com.fluxa.personal.ui.feature.transactions.TransactionSource
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Fonte de lançamentos em memória para testes JVM. As decisões de negócio vêm das classes do Codex:
 * [TransactionRules] (validação, criação, edição, cancelamento) e [BalanceCalculator] (saldos). Só a
 * persistência é simulada, com a mesma forma de leitura do DAO (lançamentos efetivados por conta, ordem do histórico).
 */
internal class FakeTransactionSource(
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val today: LocalDate,
    private val now: Instant,
) : TransactionSource {
    val rows = MutableStateFlow<Map<UUID, FinancialTransaction>>(emptyMap())
    val createdIds = mutableListOf<UUID>()
    var failWrites = false
    var failReads = false
    /** Grava e depois falha: simula a perda da resposta do primeiro envio. */
    var failAfterWrite = false

    private fun rules() = TransactionRules(today, now)

    override suspend fun create(id: UUID, draft: TransactionDraft): TransactionResult {
        if (failWrites) throw IOException("disk full")
        createdIds += id
        val result = rules().create(id, rows.value[id], draft, accounts.observe(draft.accountId).first(), categories.observe(draft.categoryId).first())
        if (result is TransactionResult.Saved) rows.update { it + (id to result.transaction) }
        if (failAfterWrite) {
            failAfterWrite = false
            throw IOException("connection reset after write")
        }
        return result
    }

    override suspend fun update(id: UUID, draft: TransactionDraft): TransactionResult {
        if (failWrites) throw IOException("disk full")
        val result = rules().edit(rows.value[id], draft, accounts.observe(draft.accountId).first(), categories.observe(draft.categoryId).first())
        if (result is TransactionResult.Saved) rows.update { it + (id to result.transaction) }
        return result
    }

    override suspend fun cancel(id: UUID): TransactionResult {
        if (failWrites) throw IOException("disk full")
        val result = rules().cancel(rows.value[id])
        if (result is TransactionResult.Saved) rows.update { it + (id to result.transaction) }
        return result
    }

    override fun observe(id: UUID): Flow<FinancialTransaction?> = rows.map { it[id] }

    override fun observeHistory(filter: TransactionHistoryFilter): Flow<List<FinancialTransaction>> =
        if (failReads) flow { throw IOException("read") } else rows.map { all ->
            all.values
                .filter { tx ->
                    val d = tx.details
                    (filter.accountId == null || d.accountId == filter.accountId) &&
                        (filter.categoryId == null || d.categoryId == filter.categoryId) &&
                        (filter.fromInclusive == null || d.occurredOn >= filter.fromInclusive) &&
                        (filter.untilExclusive == null || d.occurredOn < filter.untilExclusive) &&
                        (filter.includeCancelled || tx.cancelledAt == null)
                }
                .sortedWith(compareByDescending<FinancialTransaction> { it.details.occurredOn }.thenByDescending { it.id.toString() })
        }

    override fun observeBalances(): Flow<BalanceSnapshot> =
        combine(accounts.observeAll(includeArchived = true), rows) { accountList, txs ->
            BalanceCalculator.calculate(
                accountList.map { account ->
                    AccountBalanceInput(
                        id = account.id,
                        openingBalance = account.details.openingBalance,
                        openingDate = account.details.openingDate,
                        includedAndActive = account.details.includeInAvailable && account.archivedAt == null,
                        movements = txs.values
                            .filter { it.details.accountId == account.id && it.cancelledAt == null }
                            .map { BalanceMovement(it.details.direction, it.details.amount, it.details.occurredOn) },
                    )
                },
            )
        }
}
