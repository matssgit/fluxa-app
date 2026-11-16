package com.fluxa.personal.ui.feature.cash

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.FinancialAccount
import com.fluxa.personal.domain.model.FinancialTransaction
import com.fluxa.personal.domain.model.TransactionDirection
import com.fluxa.personal.domain.model.PeriodSummary
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.model.TransactionHistoryPage
import com.fluxa.personal.domain.rules.BalanceSnapshot
import com.fluxa.personal.domain.rules.CalculatedBalance
import com.fluxa.personal.ui.feature.transactions.TransactionSource
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Períodos de consulta. Sempre limitados, para não carregar um histórico ilimitado na memória. */
internal enum class HistoryPeriod(@StringRes val label: Int) {
    THIS_MONTH(R.string.period_this_month),
    LAST_MONTH(R.string.period_last_month),
    LAST_90_DAYS(R.string.period_last_90_days),
    THIS_YEAR(R.string.period_this_year);

    /** Início inclusivo e fim exclusivo, como o `TransactionHistoryFilter` do contrato. */
    fun range(today: LocalDate): Pair<LocalDate, LocalDate> = when (this) {
        THIS_MONTH -> today.withDayOfMonth(1).let { it to it.plusMonths(1) }
        LAST_MONTH -> today.withDayOfMonth(1).minusMonths(1).let { it to it.plusMonths(1) }
        LAST_90_DAYS -> today.minusDays(89) to today.plusDays(1)
        THIS_YEAR -> today.withDayOfYear(1).let { it to it.plusYears(1) }
    }
}

internal enum class DirectionFilter(@StringRes val label: Int, val direction: TransactionDirection?) {
    ALL(R.string.filter_all, null),
    INCOME(R.string.filter_income, TransactionDirection.INCOME),
    EXPENSE(R.string.filter_expense, TransactionDirection.EXPENSE),
}

internal data class CashFilters(
    val period: HistoryPeriod = HistoryPeriod.THIS_MONTH,
    val accountId: UUID? = null,
    val direction: DirectionFilter = DirectionFilter.ALL,
)

internal data class AccountBalanceItem(val account: FinancialAccount, val balance: CalculatedBalance?)

/** Lançamento com os nomes já resolvidos para exibição. Nenhum valor é recalculado. */
internal data class LedgerEntry(
    val transaction: FinancialTransaction,
    val account: FinancialAccount?,
    val category: Category?,
)

internal data class LedgerDay(val date: LocalDate, val entries: List<LedgerEntry>)

internal data class CashUiState(
    val isLoading: Boolean = true,
    val available: CalculatedBalance? = null,
    val accounts: List<AccountBalanceItem> = emptyList(),
    val filters: CashFilters = CashFilters(),
    val historyLoading: Boolean = true,
    val days: List<LedgerDay> = emptyList(),
    /** Quantidade exibida (páginas carregadas). */
    val entryCount: Int = 0,
    /** Há mais lançamentos além das páginas carregadas para os filtros atuais. */
    val hasMore: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    val hasAccounts: Boolean get() = accounts.isNotEmpty()
}

private data class Catalog(val accounts: List<FinancialAccount>, val categories: List<Category>)
private data class History(val transactions: List<FinancialTransaction>?, val hasMore: Boolean, val failed: Boolean)
private data class Balances(val snapshot: BalanceSnapshot?, val failed: Boolean)

/** Parâmetros da consulta do histórico: filtros + quantidade de páginas carregadas a partir do offset zero. */
private data class HistoryQuery(val filters: CashFilters = CashFilters(), val pages: Int = 1)

/**
 * Caixa. Toda a parte financeira vem do repositório (Sprint 3-C):
 * - saldo disponível e saldos por conta: `observeBalances()`;
 * - histórico: `observeHistoryPage()` com período, conta e direção na própria consulta SQL.
 *
 * O ViewModel só monta os parâmetros. "Carregar mais" aumenta o `limit` a partir do offset zero (como orienta o
 * handoff, para que inserções/edições não desalinhem páginas observadas separadamente); trocar qualquer filtro volta
 * para a primeira página. Pede-se `limit + 1` só para saber se existe próxima página.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CashViewModel(
    transactions: TransactionSource,
    accounts: AccountRepository,
    categories: CategoryRepository,
    calendar: FinancialCalendar = FinancialCalendar(),
) : ViewModel() {
    private val today = calendar.today()
    private val query = MutableStateFlow(HistoryQuery())

    private val catalog: Flow<Catalog> = combine(
        accounts.observeAll(includeArchived = true),
        categories.observeAll(includeArchived = true),
    ) { a, c -> Catalog(a, c) }

    private val balances: Flow<Balances> = transactions.observeBalances()
        .map { Balances(it, failed = false) }
        .catch { emit(Balances(null, failed = true)) }

    private val history: Flow<History> = query.flatMapLatest { q ->
        val limit = PAGE_SIZE * q.pages
        transactions.observeHistoryPage(q.filters.toHistoryFilter(today), TransactionHistoryPage(limit = limit + 1))
            .map { rows -> History(rows.take(limit), hasMore = rows.size > limit, failed = false) }
            .catch { emit(History(null, hasMore = false, failed = true)) }
    }

    val uiState: StateFlow<CashUiState> = combine(catalog, balances, history, query) { cat, bal, hist, q ->
        val byAccount = cat.accounts.associateBy { it.id }
        val byCategory = cat.categories.associateBy { it.id }
        val entries = hist.transactions.orEmpty()
            .map { LedgerEntry(it, byAccount[it.details.accountId], byCategory[it.details.categoryId]) }
        CashUiState(
            isLoading = false,
            available = bal.snapshot?.available,
            accounts = cat.accounts.filter { it.archivedAt == null }.map { AccountBalanceItem(it, bal.snapshot?.accounts?.get(it.id)) },
            filters = q.filters,
            historyLoading = false,
            // Agrupamento por dia é só apresentação; a ordem é a do repositório (occurredOn DESC, id ASC).
            days = entries.groupBy { it.transaction.details.occurredOn }.map { (d, e) -> LedgerDay(d, e) },
            entryCount = entries.size,
            hasMore = hist.hasMore,
            errorMessage = if (bal.failed || hist.failed) R.string.error_load else null,
        )
    }
        .catch { emit(CashUiState(isLoading = false, historyLoading = false, errorMessage = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashUiState())

    fun setPeriod(value: HistoryPeriod) = updateFilters { it.copy(period = value) }
    fun setAccount(value: UUID?) = updateFilters { it.copy(accountId = value) }
    fun setDirection(value: DirectionFilter) = updateFilters { it.copy(direction = value) }
    fun loadMore() = query.update { if (uiState.value.hasMore) it.copy(pages = it.pages + 1) else it }

    private fun updateFilters(change: (CashFilters) -> CashFilters) =
        query.update { current -> HistoryQuery(filters = change(current.filters), pages = 1) }

    companion object {
        const val PAGE_SIZE = 50

        fun factory(transactions: TransactionSource, accounts: AccountRepository, categories: CategoryRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { CashViewModel(transactions, accounts, categories) } }
    }
}

/** Converte os filtros da tela no filtro do contrato (início inclusivo, fim exclusivo, direção opcional). */
internal fun CashFilters.toHistoryFilter(today: LocalDate): TransactionHistoryFilter {
    val (from, until) = period.range(today)
    return TransactionHistoryFilter(accountId = accountId, fromInclusive = from, untilExclusive = until, direction = direction.direction)
}

// region Início

internal data class DashboardUiState(
    val isLoading: Boolean = true,
    val available: CalculatedBalance? = null,
    val activeAccounts: Int = 0,
    /** Mês corrente exibido: [monthStart, monthEnd). */
    val monthStart: LocalDate? = null,
    val summary: PeriodSummary? = null,
    val recent: List<LedgerEntry> = emptyList(),
    @StringRes val errorMessage: Int? = null,
)

private data class DashboardData(val snapshot: BalanceSnapshot?, val summary: PeriodSummary?, val recent: List<FinancialTransaction>?)

/**
 * Início com contratos reais: saldo disponível (`observeBalances`), resumo do mês corrente
 * (`observePeriodSummary` em [primeiro dia do mês, primeiro dia do mês seguinte)) e recentes
 * (`observeRecentTransactions(5)`: até 5 efetivados, na ordem do repositório). Nenhuma soma ou recorte no ViewModel.
 */
internal class DashboardViewModel(
    transactions: TransactionSource,
    accounts: AccountRepository,
    categories: CategoryRepository,
    calendar: FinancialCalendar = FinancialCalendar(),
) : ViewModel() {
    private val monthStart: LocalDate = calendar.today().withDayOfMonth(1)
    private val monthEnd: LocalDate = monthStart.plusMonths(1)

    private val data: Flow<DashboardData> = combine(
        transactions.observeBalances().map<BalanceSnapshot, BalanceSnapshot?> { it }.catch { emit(null) },
        transactions.observePeriodSummary(monthStart, monthEnd).map<PeriodSummary, PeriodSummary?> { it }.catch { emit(null) },
        transactions.observeRecentTransactions(RECENT_LIMIT).map<List<FinancialTransaction>, List<FinancialTransaction>?> { it }.catch { emit(null) },
    ) { snapshot, summary, recent -> DashboardData(snapshot, summary, recent) }

    val uiState: StateFlow<DashboardUiState> = combine(
        accounts.observeAll(includeArchived = true),
        categories.observeAll(includeArchived = true),
        data,
    ) { accountList, categoryList, d ->
        val byAccount = accountList.associateBy { it.id }
        val byCategory = categoryList.associateBy { it.id }
        DashboardUiState(
            isLoading = false,
            available = d.snapshot?.available,
            activeAccounts = accountList.count { it.archivedAt == null },
            monthStart = monthStart,
            summary = d.summary,
            recent = d.recent.orEmpty().map { LedgerEntry(it, byAccount[it.details.accountId], byCategory[it.details.categoryId]) },
            errorMessage = if (d.snapshot == null || d.summary == null || d.recent == null) R.string.error_load else null,
        )
    }
        .catch { emit(DashboardUiState(isLoading = false, errorMessage = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    companion object {
        const val RECENT_LIMIT = 5

        fun factory(transactions: TransactionSource, accounts: AccountRepository, categories: CategoryRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { DashboardViewModel(transactions, accounts, categories) } }
    }
}

// endregion
