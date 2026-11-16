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
import com.fluxa.personal.domain.model.TransactionHistoryFilter
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
    val entryCount: Int = 0,
    @StringRes val errorMessage: Int? = null,
) {
    val hasAccounts: Boolean get() = accounts.isNotEmpty()
}

private data class Catalog(val accounts: List<FinancialAccount>, val categories: List<Category>)
private data class History(val transactions: List<FinancialTransaction>?, val failed: Boolean)
private data class Balances(val snapshot: BalanceSnapshot?, val failed: Boolean)

/**
 * Caixa: saldo disponível e saldos por conta vêm de `observeBalances()` (Codex); o histórico vem de
 * `observeHistory()` com conta e período aplicados na consulta. O filtro receita/despesa é aplicado sobre o
 * resultado do período porque o contrato ainda não filtra por direção (documentado no handoff).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class CashViewModel(
    transactions: TransactionSource,
    accounts: AccountRepository,
    categories: CategoryRepository,
    calendar: FinancialCalendar = FinancialCalendar(),
) : ViewModel() {
    private val today = calendar.today()
    private val filters = MutableStateFlow(CashFilters())

    private val catalog: Flow<Catalog> = combine(
        accounts.observeAll(includeArchived = true),
        categories.observeAll(includeArchived = true),
    ) { a, c -> Catalog(a, c) }

    private val balances: Flow<Balances> = transactions.observeBalances()
        .map { Balances(it, failed = false) }
        .catch { emit(Balances(null, failed = true)) }

    private val history: Flow<History> = filters.flatMapLatest { f ->
        val (from, until) = f.period.range(today)
        transactions.observeHistory(TransactionHistoryFilter(accountId = f.accountId, fromInclusive = from, untilExclusive = until))
            .map { History(it, failed = false) }
            .catch { emit(History(null, failed = true)) }
    }

    val uiState: StateFlow<CashUiState> = combine(catalog, balances, history, filters) { cat, bal, hist, f ->
        val byAccount = cat.accounts.associateBy { it.id }
        val byCategory = cat.categories.associateBy { it.id }
        val active = cat.accounts.filter { it.archivedAt == null }
        val entries = hist.transactions.orEmpty()
            .filter { f.direction.direction == null || it.details.direction == f.direction.direction }
            .map { LedgerEntry(it, byAccount[it.details.accountId], byCategory[it.details.categoryId]) }
        CashUiState(
            isLoading = false,
            available = bal.snapshot?.available,
            accounts = active.map { AccountBalanceItem(it, bal.snapshot?.accounts?.get(it.id)) },
            filters = f,
            historyLoading = false,
            days = entries.groupBy { it.transaction.details.occurredOn }.map { (d, e) -> LedgerDay(d, e) },
            entryCount = entries.size,
            errorMessage = if (bal.failed || hist.failed) R.string.error_load else null,
        )
    }
        .catch { emit(CashUiState(isLoading = false, historyLoading = false, errorMessage = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CashUiState())

    fun setPeriod(value: HistoryPeriod) = filters.update { it.copy(period = value) }
    fun setAccount(value: UUID?) = filters.update { it.copy(accountId = value) }
    fun setDirection(value: DirectionFilter) = filters.update { it.copy(direction = value) }

    companion object {
        fun factory(transactions: TransactionSource, accounts: AccountRepository, categories: CategoryRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { CashViewModel(transactions, accounts, categories) } }
    }
}

// region Início

internal data class DashboardUiState(
    val isLoading: Boolean = true,
    val available: CalculatedBalance? = null,
    val activeAccounts: Int = 0,
    val recent: List<LedgerEntry> = emptyList(),
    @StringRes val errorMessage: Int? = null,
)

/**
 * Início com dados reais suportados pelo backend: saldo disponível e lançamentos recentes (últimos 30 dias,
 * efetivados). Totais de entradas/saídas do período não são calculados na UI: aguardam contrato do Codex.
 */
internal class DashboardViewModel(
    transactions: TransactionSource,
    accounts: AccountRepository,
    categories: CategoryRepository,
    calendar: FinancialCalendar = FinancialCalendar(),
) : ViewModel() {
    private val today = calendar.today()

    val uiState: StateFlow<DashboardUiState> = combine(
        accounts.observeAll(includeArchived = true),
        categories.observeAll(includeArchived = true),
        transactions.observeBalances().map<BalanceSnapshot, BalanceSnapshot?> { it }.catch { emit(null) },
        transactions.observeHistory(
            TransactionHistoryFilter(fromInclusive = today.minusDays(RECENT_DAYS - 1), untilExclusive = today.plusDays(1), includeCancelled = false),
        ).map<List<FinancialTransaction>, List<FinancialTransaction>?> { it }.catch { emit(null) },
    ) { accountList, categoryList, snapshot, recent ->
        val byAccount = accountList.associateBy { it.id }
        val byCategory = categoryList.associateBy { it.id }
        DashboardUiState(
            isLoading = false,
            available = snapshot?.available,
            activeAccounts = accountList.count { it.archivedAt == null },
            recent = recent.orEmpty().take(RECENT_LIMIT).map { LedgerEntry(it, byAccount[it.details.accountId], byCategory[it.details.categoryId]) },
            errorMessage = if (snapshot == null || recent == null) R.string.error_load else null,
        )
    }
        .catch { emit(DashboardUiState(isLoading = false, errorMessage = R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    companion object {
        const val RECENT_DAYS = 30L
        const val RECENT_LIMIT = 5

        fun factory(transactions: TransactionSource, accounts: AccountRepository, categories: CategoryRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { DashboardViewModel(transactions, accounts, categories) } }
    }
}

// endregion
