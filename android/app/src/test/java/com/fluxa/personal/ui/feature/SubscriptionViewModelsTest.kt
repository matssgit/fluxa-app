package com.fluxa.personal.ui.feature

import com.fluxa.personal.R
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.InstallmentPaymentResult
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.SubscriptionDetails
import com.fluxa.personal.domain.model.SubscriptionFrequency
import com.fluxa.personal.domain.model.SubscriptionFrequency.MONTHLY
import com.fluxa.personal.domain.model.SubscriptionFrequency.YEARLY
import com.fluxa.personal.domain.model.SubscriptionOccurrenceStatus
import com.fluxa.personal.domain.model.SubscriptionResolutionDetail
import com.fluxa.personal.domain.model.SubscriptionResult
import com.fluxa.personal.domain.model.SubscriptionSource
import com.fluxa.personal.domain.model.SubscriptionStatus
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.ui.feature.credit.CommandStatus
import com.fluxa.personal.ui.feature.subscriptions.OccurrenceOutcome
import com.fluxa.personal.ui.feature.subscriptions.SourceKind
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionAgendaViewModel
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionCompletion
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionDetailViewModel
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionFormViewModel
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionOccurrenceViewModel
import com.fluxa.personal.ui.feature.subscriptions.SubscriptionsViewModel
import com.fluxa.personal.ui.feature.transactions.TransactionFormViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Sprint 5-B: a UI de assinaturas só repassa comandos e lê os read models. O fake aplica o `SubscriptionCalendar`
 * do domínio; os testes provam que nenhuma data, valor ou estado é inventado nos ViewModels.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val now = Instant.parse("2026-11-16T12:00:00Z")
    private val today = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val calendar = FinancialCalendar(clock) { ZoneOffset.UTC }
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private lateinit var ledger: FakeTransactionSource
    private lateinit var credit: FakeCreditSource
    private lateinit var subs: FakeSubscriptionDataSource
    private lateinit var days: MutableStateFlow<LocalDate>
    private lateinit var streaming: UUID
    private lateinit var main: UUID
    private lateinit var card: UUID

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(dispatcher)
        accounts = AccountRepository(FakeAccountDao(), clock)
        categories = CategoryRepository(FakeCategoryDao(), clock)
        ledger = FakeTransactionSource(accounts, categories, today, now)
        credit = FakeCreditSource(categories, now, accounts, ledger, today)
        subs = FakeSubscriptionDataSource(accounts, categories, credit, ledger, now, today)
        days = MutableStateFlow(today)
        streaming = categories.create(CategoryDetails("Streaming", EXPENSE))
        main = accounts.create(AccountDetails("Conta Principal", AccountType.CHECKING, Money(100_000), LocalDate.of(2026, 1, 1)))
        card = (credit.createCard(CreditCardDetails("Cartão Roxo", Money(50_000), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.collect(flow: Flow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect {} }
    }

    private suspend fun create(
        name: String, cents: Long, frequency: SubscriptionFrequency = MONTHLY, anchor: LocalDate = today,
        source: SubscriptionSource = SubscriptionSource.Account(main), day: Int = anchor.dayOfMonth,
    ): UUID = (subs.create(SubscriptionDetails(name, Money(cents), streaming, source), frequency, anchor, BillingDay(day)) as SubscriptionResult.Saved).value.id

    private fun occurrenceVm(id: UUID, date: LocalDate) =
        SubscriptionOccurrenceViewModel(subs, accounts, credit, id, date, calendar, days)

    // region Lista

    @Test
    fun listSplitsActivePausedAndCancelledWithNextFromContract() = runTest(dispatcher) {
        val a = create("Música", 2_190)
        val p = create("Jornal", 3_000)
        val c = create("Nuvem", 1_000)
        subs.pause(p)
        subs.cancel(c)
        create("Antiga", 1_500, anchor = today.minusMonths(1)) // gera uma vencida
        val vm = SubscriptionsViewModel(subs, accounts, credit, days)
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(listOf("Antiga", "Música"), active.map { it.subscription.details.name })
            assertEquals(today, active.single { it.subscription.id == a }.next!!.key.occurrenceDate)
            assertEquals("Conta Principal", active.first().source.name)
            assertEquals(listOf(p), paused.map { it.subscription.id })
            assertNull(paused.single().next) // pausada: sem próxima
            assertEquals(listOf(c), cancelled.map { it.subscription.id })
            assertNull(cancelled.single().next)
            assertEquals(1, overdueCount)
        }
    }

    // endregion

    // region Formulário

    private fun form(id: UUID? = null) = SubscriptionFormViewModel(subs, categories, accounts, credit, id, calendar)

    @Test
    fun monthlyAccountSubscriptionIsCreatedWithChosenDay() = runTest(dispatcher) {
        val vm = form()
        advanceUntilIdle()
        vm.onNameChange("  Música  ")
        vm.onAmountChange("21,90")
        vm.onCategoryChange(streaming)
        vm.onBillingDayChange("31")
        vm.save()
        advanceUntilIdle()
        val created = vm.uiState.value.completion as SubscriptionCompletion.Created
        val s = subs.observeSubscription(created.id).first()!!
        assertEquals("Música", s.details.name)
        assertEquals(Money(2_190), s.details.amount)
        assertEquals(MONTHLY, s.frequency)
        assertEquals(31, s.billingDay.day)
        assertEquals(SubscriptionSource.Account(main), s.details.source) // conta única escolhida automaticamente
    }

    @Test
    fun yearlyCardSubscriptionUsesAnchorDayAndOnlyTheCard() = runTest(dispatcher) {
        val vm = form()
        advanceUntilIdle()
        vm.onNameChange("Antivírus")
        vm.onAmountChange("600,00")
        vm.onCategoryChange(streaming)
        vm.onFrequencyChange(YEARLY)
        vm.onAnchorChange(LocalDate.of(2026, 12, 5))
        vm.onBillingDayChange("20") // ignorado no anual: o contrato exige o dia da âncora
        vm.onSourceKindChange(SourceKind.CARD)
        vm.onCardChange(card)
        vm.save()
        advanceUntilIdle()
        val s = subs.observeSubscription((vm.uiState.value.completion as SubscriptionCompletion.Created).id).first()!!
        assertEquals(YEARLY, s.frequency)
        assertEquals(5, s.billingDay.day)
        assertEquals(SubscriptionSource.Card(card), s.details.source) // conta OU cartão, nunca os dois
        assertEquals(LocalDate.of(2026, 12, 5), subs.observeNextOccurrence(s.id).first()!!.key.occurrenceDate)
    }

    @Test
    fun invalidFieldsAreReportedWithoutCallingTheBackend() = runTest(dispatcher) {
        accounts.create(AccountDetails("Carteira", AccountType.WALLET, Money(0), LocalDate.of(2026, 1, 1))) // 2 contas: sem auto-seleção
        val vm = form()
        advanceUntilIdle()
        vm.onAmountChange("0")
        vm.onBillingDayChange("32")
        vm.save()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(R.string.error_name_required, nameError)
            assertEquals(R.string.error_amount_positive, amountError)
            assertEquals(R.string.error_subscription_category_required, categoryError)
            assertEquals(R.string.error_billing_day, billingDayError)
            assertEquals(R.string.error_subscription_account_required, sourceError)
            assertNull(completion)
        }
        assertTrue(subs.observeSubscriptions().first().isEmpty())
    }

    @Test
    fun editKeepsScheduleAndUpdatesDetails() = runTest(dispatcher) {
        val id = create("Música", 2_190)
        val vm = form(id)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertTrue(scheduleLocked)
            assertEquals("21,90", amountText)
        }
        vm.onFrequencyChange(YEARLY) // bloqueado
        vm.onAmountChange("25,90")
        vm.save()
        advanceUntilIdle()
        assertEquals(SubscriptionCompletion.Updated, vm.uiState.value.completion)
        val s = subs.observeSubscription(id).first()!!
        assertEquals(MONTHLY, s.frequency)
        assertEquals(Money(2_590), s.details.amount)
        // A cobrança de hoje mantém o termo anterior; a mudança vale a partir de amanhã.
        assertEquals(Money(2_190), subs.observeOccurrence(id, today).first()!!.details.amount)
    }

    // endregion

    // region Agenda

    @Test
    fun agendaGroupsOverdueUpcomingAndResolvedFromTheContract() = runTest(dispatcher) {
        val a = create("Música", 2_190, anchor = LocalDate.of(2026, 10, 20))
        val b = create("Nuvem", 1_000, anchor = LocalDate.of(2026, 11, 1), source = SubscriptionSource.Card(card))
        val c = create("Antivírus", 60_000, frequency = YEARLY, anchor = LocalDate.of(2026, 11, 10))
        val vm = SubscriptionAgendaViewModel(subs, accounts, credit, days)
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(listOf(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10)), overdue.map { it.date })
            assertTrue(overdue.flatMap { it.items }.all { it.status == SubscriptionOccurrenceStatus.OVERDUE })
            assertEquals(listOf(LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 1)), upcoming.map { it.date })
            assertTrue(upcoming.flatMap { it.items }.all { it.status == SubscriptionOccurrenceStatus.PENDING })
            assertEquals(Money(60_000), overdue.last().items.single().details.amount) // anual: valor cheio, sem equivalente mensal
        }
        subs.skip(a, LocalDate.of(2026, 10, 20))
        subs.pay(b, LocalDate.of(2026, 11, 1), today)
        subs.pay(c, LocalDate.of(2026, 11, 10), today)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertTrue(overdue.isEmpty())
            val statuses = resolved.flatMap { it.items }.associate { it.key.subscriptionId to it.status }
            assertEquals(SubscriptionOccurrenceStatus.SKIPPED, statuses[a])
            assertEquals(SubscriptionOccurrenceStatus.CHARGED, statuses[b])
            assertEquals(SubscriptionOccurrenceStatus.PAID, statuses[c])
            assertEquals(resolved.map { it.date }.sortedDescending(), resolved.map { it.date }) // mais recentes primeiro
        }
    }

    @Test
    fun crossingMidnightRecreatesTheObservation() = runTest(dispatcher) {
        create("Música", 2_190) // vence hoje
        val vm = SubscriptionAgendaViewModel(subs, accounts, credit, days)
        collect(vm.uiState)
        advanceUntilIdle()
        assertEquals(SubscriptionOccurrenceStatus.PENDING, vm.uiState.value.upcoming.first().items.single().status)
        subs.today = today.plusDays(1) // o relógio passou da meia-noite
        days.value = today.plusDays(1)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(SubscriptionOccurrenceStatus.OVERDUE, overdue.single().items.single().status)
            assertEquals(LocalDate.of(2026, 11, 17), this.today)
        }
    }

    // endregion

    // region Ações sobre ocorrências

    @Test
    fun payingInAccountCreatesOneLedgerExpenseWithOccurrenceTerms() = runTest(dispatcher) {
        val id = create("Antivírus", 60_000, frequency = YEARLY, anchor = LocalDate.of(2026, 11, 10))
        val vm = occurrenceVm(id, LocalDate.of(2026, 11, 10))
        collect(vm.uiState)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canSettle)
        assertFalse(vm.uiState.value.isCard)
        vm.onPaidOnChange(today.minusDays(1))
        vm.settle()
        vm.settle() // toque duplo
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Success, status)
            assertEquals(OccurrenceOutcome.Paid, outcome)
            assertEquals(SubscriptionOccurrenceStatus.PAID, occurrence!!.status)
        }
        assertEquals(1, subs.payCalls.size)
        val tx = ledger.rows.value.values.single()
        assertEquals(Money(60_000), tx.details.amount)
        assertEquals(today.minusDays(1), tx.details.occurredOn)
        assertEquals(id, tx.subscriptionOccurrence!!.subscriptionId)
    }

    @Test
    fun chargingOnCardCreatesPurchaseAndCommitsLimitWithoutLedgerEntry() = runTest(dispatcher) {
        val id = create("Nuvem", 1_000, anchor = LocalDate.of(2026, 11, 1), source = SubscriptionSource.Card(card))
        val vm = occurrenceVm(id, LocalDate.of(2026, 11, 1))
        collect(vm.uiState)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.isCard)
        vm.settle()
        advanceUntilIdle()
        assertEquals(OccurrenceOutcome.Charged, vm.uiState.value.outcome)
        assertEquals(SubscriptionOccurrenceStatus.CHARGED, vm.uiState.value.occurrence!!.status)
        assertEquals(Money(1_000), credit.observeCard(card).first()!!.committedAmount)
        assertEquals("Nuvem", credit.observePurchases(card).first().single().details.title)
        assertTrue(ledger.rows.value.isEmpty()) // só entra no Caixa quando a parcela for paga
    }

    @Test
    fun insufficientCardLimitKeepsOccurrenceOpen() = runTest(dispatcher) {
        val id = create("Curso", 80_000, anchor = LocalDate.of(2026, 11, 1), source = SubscriptionSource.Card(card)) // limite 500
        val vm = occurrenceVm(id, LocalDate.of(2026, 11, 1))
        collect(vm.uiState)
        advanceUntilIdle()
        vm.settle()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Error, status)
            assertEquals(R.string.error_subscription_card_limit, error)
            assertEquals(SubscriptionOccurrenceStatus.OVERDUE, occurrence!!.status)
            assertTrue(canSettle) // pode tentar de novo
        }
        assertTrue(credit.observePurchases(card).first().isEmpty())
    }

    @Test
    fun skippingResolvesWithoutMoneyAndStaysInHistory() = runTest(dispatcher) {
        val id = create("Música", 2_190, anchor = LocalDate.of(2026, 10, 20))
        val vm = occurrenceVm(id, LocalDate.of(2026, 10, 20))
        collect(vm.uiState)
        advanceUntilIdle()
        vm.skip()
        advanceUntilIdle()
        assertEquals(OccurrenceOutcome.Skipped, vm.uiState.value.outcome)
        assertEquals(SubscriptionOccurrenceStatus.SKIPPED, vm.uiState.value.occurrence!!.status)
        assertTrue(subs.observeOverdueOccurrences().first().isEmpty())
        assertTrue(ledger.rows.value.isEmpty())
        val detail = SubscriptionDetailViewModel(subs, accounts, credit, categories, id, days)
        collect(detail.uiState)
        advanceUntilIdle()
        assertEquals(SubscriptionOccurrenceStatus.SKIPPED, detail.uiState.value.history.single().status)
    }

    @Test
    fun alreadyResolvedIsShownAsUpdatedState() = runTest(dispatcher) {
        val id = create("Música", 2_190)
        val vm = occurrenceVm(id, today)
        collect(vm.uiState)
        advanceUntilIdle()
        subs.pay(id, today, today) // outra tentativa resolveu antes
        vm.settle()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(CommandStatus.Success, status)
            assertEquals(OccurrenceOutcome.AlreadyResolved, outcome)
            assertNull(error)
        }
        assertEquals(1, ledger.rows.value.size)
    }

    @Test
    fun futureOccurrenceCanOnlyBeSkipped() = runTest(dispatcher) {
        val id = create("Música", 2_190, anchor = today.plusDays(5))
        val vm = occurrenceVm(id, today.plusDays(5))
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertTrue(isFuture)
            assertFalse(canSettle)
            assertTrue(canSkip)
        }
    }

    // endregion

    // region Pausa, retomada e cancelamento

    @Test
    fun pauseResumeAndCancelUseTheContractAndRefreshState() = runTest(dispatcher) {
        val id = create("Música", 2_190, anchor = today.plusDays(1))
        val vm = SubscriptionDetailViewModel(subs, accounts, credit, categories, id, days)
        collect(vm.uiState)
        advanceUntilIdle()
        assertEquals(today.plusDays(1), vm.uiState.value.next!!.key.occurrenceDate)
        vm.pause()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(SubscriptionStatus.PAUSED, status)
            assertNull(next)
            assertTrue(canResume)
            assertEquals(1, actionCount)
        }
        vm.resume()
        advanceUntilIdle()
        assertEquals(SubscriptionStatus.ACTIVE, vm.uiState.value.status)
        vm.cancel()
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertEquals(SubscriptionStatus.CANCELLED, status)
            assertNull(next)
            assertFalse(canCancel)
            assertFalse(canEdit)
            assertFalse(canResume) // cancelamento é terminal
        }
    }

    @Test
    fun pausedIntervalGeneratesNoOccurrencesAfterResume() = runTest(dispatcher) {
        val id = create("Música", 2_190, anchor = LocalDate.of(2026, 11, 20), day = 20)
        subs.pause(id)
        subs.today = LocalDate.of(2027, 1, 25) // dois meses pausada
        days.value = subs.today
        subs.resume(id)
        val vm = SubscriptionDetailViewModel(subs, accounts, credit, categories, id, days)
        collect(vm.uiState)
        advanceUntilIdle()
        val dates = subs.observeOccurrences(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 3, 1)).first()
            .filter { it.key.subscriptionId == id }.map { it.key.occurrenceDate }
        assertEquals(listOf(LocalDate.of(2027, 2, 20)), dates) // 20/11, 20/12 e 20/01 não são recriadas
        assertEquals(LocalDate.of(2027, 2, 20), vm.uiState.value.next!!.key.occurrenceDate)
    }

    @Test
    fun pausedSubscriptionBlocksPaymentButAllowsSkip() = runTest(dispatcher) {
        val id = create("Música", 2_190, anchor = LocalDate.of(2026, 10, 20))
        subs.pause(id)
        val vm = occurrenceVm(id, LocalDate.of(2026, 10, 20))
        collect(vm.uiState)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertFalse(canSettle)
            assertTrue(canSkip)
        }
        assertEquals(SubscriptionResult.Rejected(com.fluxa.personal.domain.model.SubscriptionError.PAUSED), subs.pay(id, LocalDate.of(2026, 10, 20), today))
    }

    // endregion

    // region Caixa

    @Test
    fun subscriptionExpenseOpensReadOnlyWithSubscriptionOrigin() = runTest(dispatcher) {
        val id = create("Música", 2_190)
        val paid = subs.pay(id, today, today) as SubscriptionResult.Saved
        val vm = TransactionFormViewModel(ledger, accounts, categories, paid.value.resultId!!, calendar = calendar)
        advanceUntilIdle()
        with(vm.uiState.value) {
            assertTrue(isManagedBySubscription)
            assertTrue(isReadOnly)
            assertEquals(id, subscriptionId)
            assertEquals(Money(2_190), managedAmount)
        }
        vm.cancelTransaction()
        advanceUntilIdle()
        assertNull(ledger.rows.value.values.single().cancelledAt)
    }

    @Test
    fun installmentPaymentExposesOriginForNavigation() = runTest(dispatcher) {
        val purchase = UUID.randomUUID()
        credit.createPurchase(purchase, PurchaseDraft(card, streaming, "Fone", Money(30_000), 3, today))
        val installment = credit.observeInstallments(purchase).first().first().id
        val paid = credit.payInstallment(installment, main, today) as InstallmentPaymentResult.Paid
        val vm = TransactionFormViewModel(ledger, accounts, categories, paid.transaction.id, calendar = calendar, installmentOrigins = credit::observeInstallmentOrigin)
        advanceUntilIdle()
        with(vm.uiState.value.installmentOrigin) {
            assertNotNull(this)
            assertEquals(card, this!!.cardId)
            assertEquals(purchase, purchaseId)
            assertEquals(installment, installmentId)
        }
    }

    // endregion
    @Test
    fun subscriptionDetailUsesScopedHistoryInDescendingOrder() = runTest(dispatcher) {
        val previous = today.minusMonths(1)
        val id = create("Música", 2_190, anchor = previous)
        val other = create("Nuvem", 1_000)
        subs.skip(other, today)
        subs.skip(id, previous)
        subs.pay(id, today, today)
        val vm = SubscriptionDetailViewModel(subs, accounts, credit, categories, id, days)
        collect(vm.uiState)
        advanceUntilIdle()

        assertEquals(0, subs.globalOccurrenceQueries)
        assertEquals(listOf(today, previous), vm.uiState.value.history.map { it.key.occurrenceDate })
        assertTrue(vm.uiState.value.history.all { it.key.subscriptionId == id })
    }

    @Test
    fun accountUndoRequiresConfirmationAndSubmitsOnce() = runTest(dispatcher) {
        val past = today.minusMonths(1)
        val id = create("Música", 2_190, anchor = past)
        subs.pay(id, past, today)
        val vm = occurrenceVm(id, past)
        collect(vm.uiState)
        advanceUntilIdle()
        val target = vm.uiState.value.undoTarget!!
        assertEquals(today, vm.uiState.value.resolution!!.occurredOn)

        vm.confirmUndo()
        vm.requestUndo(target)
        advanceUntilIdle()
        assertEquals(target, vm.uiState.value.undoConfirmation)
        assertTrue(subs.undoCalls.isEmpty())
        vm.confirmUndo()
        vm.confirmUndo()
        advanceUntilIdle()

        assertEquals(listOf(target.resultId), subs.undoCalls)
        assertEquals(OccurrenceOutcome.Reopened, vm.uiState.value.outcome)
        assertEquals(SubscriptionOccurrenceStatus.OVERDUE, vm.uiState.value.occurrence!!.status)
        assertNull(vm.uiState.value.undoTarget)
        assertNull(ledger.observe(target.resultId).first())
        assertTrue(vm.uiState.value.canSettle)
    }

    @Test
    fun cardUndoBlocksPaidInstallmentAndKeepsItsStructuralId() = runTest(dispatcher) {
        val id = create("Nuvem", 1_000, source = SubscriptionSource.Card(card))
        subs.pay(id, today, today)
        val vm = occurrenceVm(id, today)
        collect(vm.uiState)
        advanceUntilIdle()
        val resolution = vm.uiState.value.resolution as SubscriptionResolutionDetail.Card
        credit.payInstallment(resolution.installmentId, main, today)
        val target = vm.uiState.value.undoTarget!!
        vm.requestUndo(target)
        vm.confirmUndo()
        advanceUntilIdle()

        assertEquals(R.string.error_occurrence_undo_installment_paid, vm.uiState.value.error)
        assertEquals(resolution, vm.uiState.value.resolution)
        val payment = credit.observeInstallmentPayment(resolution.installmentId).first()!!
        credit.undoInstallmentPayment(resolution.installmentId, payment.id)
        vm.requestUndo(target)
        vm.confirmUndo()
        advanceUntilIdle()

        assertEquals(SubscriptionOccurrenceStatus.PENDING, vm.uiState.value.occurrence!!.status)
        assertEquals(OccurrenceOutcome.Reopened, vm.uiState.value.outcome)
        assertEquals(Money(0), credit.observeCard(card).first()!!.committedAmount)
    }

    @Test
    fun staleConfirmationCannotUndoReplacementPayment() = runTest(dispatcher) {
        val id = create("Música", 2_190)
        subs.pay(id, today, today)
        val vm = occurrenceVm(id, today)
        collect(vm.uiState)
        advanceUntilIdle()
        val original = vm.uiState.value.undoTarget!!
        vm.requestUndo(original)
        subs.undoOccurrence(id, today, original.resultId)
        subs.pay(id, today, today)
        advanceUntilIdle()
        val replacement = vm.uiState.value.undoTarget!!
        assertTrue(replacement != original)
        assertEquals(original, vm.uiState.value.undoConfirmation)

        vm.confirmUndo()
        advanceUntilIdle()

        assertEquals(R.string.error_occurrence_undo_conflict, vm.uiState.value.error)
        assertEquals(replacement, vm.uiState.value.undoTarget)
        assertEquals(SubscriptionOccurrenceStatus.PAID, vm.uiState.value.occurrence!!.status)
        assertEquals(original.resultId, subs.undoCalls.last())
    }

    @Test
    fun historicalUndoPreservesPausedAndCancelledSubscriptions() = runTest(dispatcher) {
        val past = today.minusMonths(1)
        for (status in listOf(SubscriptionStatus.PAUSED, SubscriptionStatus.CANCELLED)) {
            val id = create("Música", 2_190, anchor = past)
            subs.pay(id, past, today)
            if (status == SubscriptionStatus.PAUSED) subs.pause(id) else subs.cancel(id)
            val vm = occurrenceVm(id, past)
            collect(vm.uiState)
            advanceUntilIdle()

            vm.requestUndo(vm.uiState.value.undoTarget!!)
            vm.confirmUndo()
            advanceUntilIdle()

            assertEquals(status, vm.uiState.value.subscription!!.status)
            assertEquals(SubscriptionOccurrenceStatus.OVERDUE, vm.uiState.value.occurrence!!.status)
            assertFalse(vm.uiState.value.canSettle)
        }
    }

}
