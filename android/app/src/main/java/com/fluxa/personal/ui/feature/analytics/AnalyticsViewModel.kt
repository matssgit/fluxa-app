package com.fluxa.personal.ui.feature.analytics

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.FinancialAnalyticsRepository
import com.fluxa.personal.domain.model.CashFlowMonth
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CategorySpending
import com.fluxa.personal.domain.model.FinancialAnalyticsError
import com.fluxa.personal.domain.model.FinancialAnalyticsSnapshot
import com.fluxa.personal.domain.model.FinancialAnalyticsState
import com.fluxa.personal.domain.model.FinancialInsight
import com.fluxa.personal.domain.model.FinancialInsightType
import com.fluxa.personal.domain.model.FinancialProjection
import com.fluxa.personal.domain.model.MonthlyFinancialSummary
import com.fluxa.personal.domain.model.SubscriptionAnalytics
import com.fluxa.personal.ui.common.financialDays
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

internal object AnalyticsRoutes {
    const val HOME = "analytics"
}

// region Fontes

/** Recorte do `FinancialAnalyticsRepository` (para testes JVM). Todo o cálculo é do backend. */
internal interface AnalyticsDataSource {
    /** `null` = mês financeiro atual. */
    fun observe(month: YearMonth?): Flow<FinancialAnalyticsState>
    /** Só para cor e ícone de cada categoria; nome, valor e percentual vêm do snapshot. */
    fun observeCategories(): Flow<List<Category>>
}

internal class RepositoryAnalyticsDataSource(
    private val analytics: FinancialAnalyticsRepository,
    private val categories: CategoryRepository,
) : AnalyticsDataSource {
    override fun observe(month: YearMonth?) = analytics.observeFinancialAnalytics(month)
    override fun observeCategories() = categories.observeAll(includeArchived = true)
}

// endregion

// region Estado de apresentação

/** Categoria do top 5 do contrato com a aparência do cadastro. */
internal data class CategoryItem(val spending: CategorySpending, val icon: String?, val color: String?)

internal sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Error(@StringRes val message: Int) : AnalyticsUiState
    data class Content(
        val today: LocalDate,
        val month: YearMonth,
        val isCurrentMonth: Boolean,
        val summary: MonthlyFinancialSummary,
        /** Top 5 na ordem do contrato; percentuais são do total, não do top 5. */
        val categories: List<CategoryItem>,
        /** Quantas categorias ficaram fora do top 5. Contagem, nunca um valor "Outras" inventado. */
        val otherCategories: Int,
        val evolution: List<CashFlowMonth>,
        val projection: FinancialProjection,
        val remainingMonth: FinancialProjection,
        val subscriptions: SubscriptionAnalytics,
        val insights: List<FinancialInsight>,
        /** Nada registrado em lugar nenhum: a tela vira um convite, sem gráficos vazios. */
        val isEmpty: Boolean,
    ) : AnalyticsUiState {
        val hasMovements: Boolean get() = summary.transactionCount > 0
        val hasExpenses: Boolean get() = categories.isNotEmpty()
        val hasEvolution: Boolean get() = evolution.any { it.income.cents != 0L || it.expense.cents != 0L }
        /** Escala do gráfico: maior barra dos seis meses (só desenho). */
        val evolutionPeak: Money get() = evolution.flatMap { listOf(it.income, it.expense) }.maxOrNull() ?: Money(0)
    }
}

// endregion

/**
 * Análises: escolhe o mês, coleta o snapshot do backend e prepara a apresentação. Não soma, não filtra e não projeta.
 * Na virada do dia ([days], o mesmo sinal das outras telas) a observação é recriada e "mês atual" volta a ser o mês de hoje.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class AnalyticsViewModel(
    source: AnalyticsDataSource,
    days: Flow<LocalDate> = financialDays(),
) : ViewModel() {
    /** `null` acompanha o mês atual, inclusive na virada do mês. */
    private val selected = MutableStateFlow<YearMonth?>(null)

    val uiState: StateFlow<AnalyticsUiState> = combine(days, selected) { today, month ->
        val current = YearMonth.from(today)
        // Um relógio atrasado não pode deixar a tela num mês futuro.
        today to month?.takeIf { it < current }
    }.flatMapLatest { (today, month) ->
        combine(source.observe(month), source.observeCategories()) { state, categories ->
            when (state) {
                is FinancialAnalyticsState.Unavailable -> AnalyticsUiState.Error(
                    if (state.error == FinancialAnalyticsError.OVERFLOW) R.string.analytics_unavailable_overflow
                    else R.string.analytics_unavailable_conflict,
                )
                is FinancialAnalyticsState.Ready -> content(today, state.snapshot, categories)
            }
        }
    }
        .catch { emit(AnalyticsUiState.Error(R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsUiState.Loading)

    fun previousMonth() {
        val shown = uiState.value as? AnalyticsUiState.Content ?: return
        selected.value = shown.month.minusMonths(1)
    }

    /** Nunca passa do mês atual: o realizado não existe no futuro. */
    fun nextMonth() {
        val shown = uiState.value as? AnalyticsUiState.Content ?: return
        if (shown.isCurrentMonth) return
        val next = shown.month.plusMonths(1)
        selected.value = next.takeIf { it < YearMonth.from(shown.today) }
    }

    private fun content(today: LocalDate, s: FinancialAnalyticsSnapshot, categories: List<Category>): AnalyticsUiState.Content {
        val byId = categories.associateBy { it.id }
        val top = s.topCategories
        return AnalyticsUiState.Content(
            today = today,
            month = s.monthSummary.month,
            isCurrentMonth = s.monthSummary.month == YearMonth.from(today),
            summary = s.monthSummary,
            categories = top.map { CategoryItem(it, byId[it.categoryId]?.details?.icon, byId[it.categoryId]?.details?.color) },
            otherCategories = s.categorySpending.size - top.size,
            evolution = s.sixMonthEvolution,
            projection = s.projection,
            remainingMonth = s.remainingMonthProjection,
            subscriptions = s.subscriptionAnalytics,
            insights = s.insights,
            // Só um banco sem nada (em mês atual) vira estado vazio; um mês passado vazio continua navegável.
            isEmpty = s.monthSummary.month == YearMonth.from(today) && s.monthSummary.transactionCount == 0L &&
                s.sixMonthEvolution.all { it.income.cents == 0L && it.expense.cents == 0L } &&
                s.subscriptionAnalytics.activeCount == 0L && s.projection.availableNow.cents == 0L && s.projection.obligationsTotal.cents == 0L,
        )
    }

    companion object {
        fun factory(source: AnalyticsDataSource): ViewModelProvider.Factory =
            viewModelFactory { initializer { AnalyticsViewModel(source) } }
    }
}

/** Fatos que denunciam sinal ou variação do resultado ficam de fora quando os valores estão ocultos. */
internal fun visibleInsights(insights: List<FinancialInsight>, hidden: Boolean): List<FinancialInsight> =
    if (!hidden) insights
    else insights.filter { it.type == FinancialInsightType.TOP_CATEGORY || it.type == FinancialInsightType.NO_EXPENSES }

/** Formatação de razões do contrato em porcentagem (0.3387 → "33,9%"). Só apresentação. */
internal object PercentFormat {
    private val Hundred = BigDecimal(100)

    fun ratio(value: BigDecimal, signed: Boolean = false): String {
        val percent = value.multiply(Hundred).setScale(1, RoundingMode.HALF_UP)
        // Uma perda ou ganho minúsculo não pode aparecer como um zero exato.
        if (percent.signum() == 0 && value.signum() != 0) return if (value.signum() < 0) ">−0,1%" else "<0,1%"
        val text = percent.abs().stripTrailingZeros().toPlainString().replace('.', ',') + "%"
        return when {
            percent.signum() < 0 -> "−$text"
            signed && percent.signum() > 0 -> "+$text"
            else -> text
        }
    }

    /** Percentual inteiro da categoria; uma fatia pequena mas real não é "0%". */
    fun share(spending: CategorySpending): String =
        if (spending.percentage == 0 && spending.amount.cents > 0) "<1%" else "${spending.percentage}%"
}
