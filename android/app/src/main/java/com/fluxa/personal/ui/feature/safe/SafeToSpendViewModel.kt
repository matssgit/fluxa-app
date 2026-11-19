package com.fluxa.personal.ui.feature.safe

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fluxa.personal.R
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.SafeToSpendRepository
import com.fluxa.personal.domain.model.ProjectedCashEvent
import com.fluxa.personal.domain.model.ProjectedCashEventType
import com.fluxa.personal.domain.model.ProjectedCashSource
import com.fluxa.personal.domain.model.SafeToSpendError
import com.fluxa.personal.domain.model.SafeToSpendResult
import com.fluxa.personal.domain.model.SafeToSpendState
import com.fluxa.personal.ui.common.financialDays
import com.fluxa.personal.ui.feature.credit.CreditSource
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionDataSource
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal object SafeRoutes {
    const val DETAIL = "safe-to-spend"
}

// region Fontes

/** Recorte do `SafeToSpendRepository` (para testes JVM). O cálculo inteiro é do backend. */
internal interface SafeToSpendDataSource {
    fun observeSafeToSpend(horizonDays: Int): Flow<SafeToSpendState>
}

internal class RepositorySafeToSpendDataSource(private val repository: SafeToSpendRepository) : SafeToSpendDataSource {
    override fun observeSafeToSpend(horizonDays: Int) = repository.observeSafeToSpend(horizonDays)
}

/** Rótulo de uma parcela (só texto e data original para a explicação; valor e data efetiva vêm do breakdown). */
internal data class InstallmentLabel(val title: String, val number: Int, val count: Int, val dueDate: LocalDate)

/**
 * Nomes legíveis para os eventos do breakdown, buscados **pelos IDs estruturais** que o backend entrega.
 * Não soma, não filtra e não deduplica: só transforma "parcela X" em "Notebook 3/10" e "ocorrência Y" em "Netflix".
 */
internal interface CashEventLabels {
    fun subscriptionName(id: UUID, occurrenceDate: LocalDate): Flow<String?>
    fun installmentLabel(source: ProjectedCashSource.Installment): Flow<InstallmentLabel?>
}

internal class RepositoryCashEventLabels(
    private val subscriptions: SubscriptionDataSource,
    private val credit: CreditSource,
) : CashEventLabels {
    override fun subscriptionName(id: UUID, occurrenceDate: LocalDate) =
        subscriptions.observeOccurrenceDetail(id, occurrenceDate).map { it?.occurrence?.details?.name }

    override fun installmentLabel(source: ProjectedCashSource.Installment) = combine(
        credit.observePurchases(source.cardId).map { list -> list.firstOrNull { it.id == source.purchaseId } },
        credit.observeInstallments(source.purchaseId).map { list -> list.firstOrNull { it.id == source.installmentId } },
    ) { purchase, installment ->
        if (purchase == null || installment == null) null
        else InstallmentLabel(purchase.details.title, installment.number, purchase.details.installmentCount, installment.dueDate)
    }
}

// endregion

// region Estado de apresentação

/** Situação derivada só para escolher textos; nenhuma regra ou limiar financeiro novo. */
internal enum class SafeSituation {
    /** O limite é positivo e obrigações conhecidas reduzem parte do disponível. */
    Reduced,
    /** O limite é igual ao disponível: nenhuma obrigação conhecida o reduz no período. */
    NoObligations,
    /** Disponível positivo, mas as obrigações consomem toda a margem. */
    ZeroByObligations,
    /** O disponível já está negativo; o limite é zero. */
    NegativeAvailable,
}

internal data class SafeEventItem(
    val event: ProjectedCashEvent,
    /** "Netflix" / "Notebook"; nulo enquanto o rótulo não chega (a linha mostra só o tipo). */
    val title: String?,
    /** Parcela n/N quando a fonte é uma parcela. */
    val installmentNumber: Int? = null,
    val installmentCount: Int? = null,
    /** Data original quando a obrigação vencida foi aplicada hoje (breakdown usa a data efetiva). */
    val originalDate: LocalDate? = null,
) {
    val isOverdue: Boolean get() = originalDate != null && originalDate < event.date
    val isIncome: Boolean get() = event.type == ProjectedCashEventType.CONFIRMED_INCOME
}

internal sealed interface SafeToSpendUiState {
    data object Loading : SafeToSpendUiState
    data class Unavailable(@StringRes val message: Int) : SafeToSpendUiState
    data class Content(
        val today: LocalDate,
        val horizonDays: Int,
        val safeAmount: Money,
        val availableNow: Money,
        val lowestProjectedBalance: Money,
        val lowestSlackDate: LocalDate,
        val horizonStart: LocalDate,
        val horizonEndExclusive: LocalDate,
        val situation: SafeSituation,
        /**
         * Quanto do disponível está comprometido até a menor folga — `availableNow - safeAmount`, **só para exibir**
         * (nunca usado em cálculo). Nulo quando não se aplica (disponível negativo ou nada comprometido).
         */
        val committedForDisplay: Money?,
        val breakdown: List<SafeEventItem>,
    ) : SafeToSpendUiState {
        val lowestSlackIsToday: Boolean get() = lowestSlackDate == today
    }
}

// endregion

/**
 * Limite seguro: coleta `observeSafeToSpend(30)` e prepara textos/rótulos. Não recalcula nada. Na virada do dia
 * ([days], o mesmo sinal das Assinaturas) a observação é recriada, como recomenda o handoff 7-A.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SafeToSpendViewModel(
    source: SafeToSpendDataSource,
    private val labels: CashEventLabels,
    days: Flow<LocalDate> = financialDays(),
    private val horizonDays: Int = HORIZON_DAYS,
) : ViewModel() {

    val uiState: StateFlow<SafeToSpendUiState> = days.flatMapLatest { today ->
        source.observeSafeToSpend(horizonDays).flatMapLatest { state ->
            when (state) {
                is SafeToSpendState.Unavailable -> flowOf(
                    SafeToSpendUiState.Unavailable(
                        if (state.error == SafeToSpendError.OVERFLOW) R.string.safe_unavailable_overflow else R.string.safe_unavailable_conflict,
                    ),
                )
                is SafeToSpendState.Ready -> labelled(state.result.breakdown).map { items -> content(today, state.result, items) }
            }
        }
    }
        .catch { emit(SafeToSpendUiState.Unavailable(R.string.error_load)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SafeToSpendUiState.Loading)

    private fun labelled(events: List<ProjectedCashEvent>): Flow<List<SafeEventItem>> {
        if (events.isEmpty()) return flowOf(emptyList())
        return combine(events.map { e -> labelFor(e) }) { it.toList() }
    }

    private fun labelFor(e: ProjectedCashEvent): Flow<SafeEventItem> = when (val s = e.source) {
        is ProjectedCashSource.Subscription ->
            labels.subscriptionName(s.key.subscriptionId, s.key.occurrenceDate)
                .map { SafeEventItem(e, it, originalDate = s.key.occurrenceDate) }
        is ProjectedCashSource.Installment ->
            labels.installmentLabel(s).map { l -> SafeEventItem(e, l?.title, l?.number, l?.count, l?.dueDate) }
        is ProjectedCashSource.ConfirmedIncome -> flowOf(SafeEventItem(e, null))
    }

    private fun content(today: LocalDate, r: SafeToSpendResult, items: List<SafeEventItem>): SafeToSpendUiState.Content {
        val situation = when {
            r.availableNow.cents < 0 -> SafeSituation.NegativeAvailable
            r.safeAmount.cents == 0L -> SafeSituation.ZeroByObligations
            r.safeAmount == r.availableNow -> SafeSituation.NoObligations
            else -> SafeSituation.Reduced
        }
        val committed = if (r.availableNow.cents >= 0 && r.safeAmount < r.availableNow) r.availableNow - r.safeAmount else null
        return SafeToSpendUiState.Content(
            today = today, horizonDays = horizonDays, safeAmount = r.safeAmount, availableNow = r.availableNow,
            lowestProjectedBalance = r.lowestProjectedBalance, lowestSlackDate = r.lowestSlackDate,
            horizonStart = r.horizonStart, horizonEndExclusive = r.horizonEndExclusive,
            situation = situation, committedForDisplay = committed, breakdown = items,
        )
    }

    companion object {
        const val HORIZON_DAYS = 30

        fun factory(source: SafeToSpendDataSource, labels: CashEventLabels): ViewModelProvider.Factory =
            viewModelFactory { initializer { SafeToSpendViewModel(source, labels) } }
    }
}
