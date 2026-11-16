package com.fluxa.personal.data.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.data.repository.TransactionRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult
import com.fluxa.personal.domain.model.Installment
import com.fluxa.personal.domain.model.InstallmentPaymentError
import com.fluxa.personal.domain.model.InstallmentPaymentResult.AlreadyPaid
import com.fluxa.personal.domain.model.InstallmentPaymentResult.Paid
import com.fluxa.personal.domain.model.InstallmentPaymentResult.Rejected
import com.fluxa.personal.domain.model.InstallmentPaymentResult.Undone
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.Purchase
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.PurchaseStatus
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionError
import com.fluxa.personal.domain.model.TransactionHistoryFilter
import com.fluxa.personal.domain.model.TransactionResult
import com.fluxa.personal.domain.rules.CalculatedBalance.Exact
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallmentPaymentTest {
    private val today = LocalDate.of(2026, 11, 16)
    private val clock = Clock.fixed(Instant.parse("2026-11-16T12:00:00Z"), ZoneOffset.UTC)
    private val name = "payments-${UUID.randomUUID()}"
    private lateinit var context: Context
    private lateinit var database: FluxaDatabase
    private lateinit var credit: CreditRepository
    private lateinit var ledger: TransactionRepository
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private lateinit var account: UUID
    private lateinit var category: UUID
    private lateinit var card: UUID

    @Before
    fun setUp() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = FluxaDatabase.create(context, name)
        val calendar = FinancialCalendar(clock) { ZoneOffset.UTC }
        credit = CreditRepository(database, clock, calendar)
        ledger = TransactionRepository(database, clock, calendar)
        accounts = AccountRepository(database.accountDao(), clock)
        categories = CategoryRepository(database.categoryDao(), clock)
        account = accounts.create(AccountDetails("Banco", AccountType.CHECKING, Money(100), today.minusMonths(1)))
        category = categories.create(CategoryDetails("Compras", EXPENSE))
        card = (credit.createCard(CreditCardDetails("Cartão", Money(1000), BillingDay(10), BillingDay(17))) as CreditResult.Saved).value.id
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun paymentUpdatesCashReadModelsAndUndoReopensThenAllowsRepayment() = runBlocking {
        val (purchase, installment) = purchase(700)
        assertEquals(Money(300), credit.observeCard(card).first()!!.availableLimit)
        val payment = (credit.payInstallment(installment.id, account, today) as Paid).transaction
        assertEquals(installment.id, payment.installmentId)
        assertEquals(installment.amount, payment.details.amount)
        assertEquals(category, payment.details.categoryId)
        assertEquals(EXPENSE, payment.details.direction)
        assertEquals(today, payment.details.occurredOn)
        assertEquals(installment.copy(status = InstallmentStatus.PAID), credit.observeInstallments(purchase.id).first().single())
        assertEquals(payment, credit.observeInstallmentPayment(installment.id).first())
        assertEquals(payment, ledger.observeHistory(TransactionHistoryFilter(direction = EXPENSE)).first().single())
        assertEquals(payment, ledger.observeRecentTransactions(5).first().single())
        assertEquals(Exact(Money(700)), ledger.observePeriodSummary(today, today.plusDays(1)).first().expenses)
        assertEquals(Exact(Money(-600)), ledger.observeAccountBalance(account).first())
        assertEquals(Exact(Money(-600)), ledger.observeAvailableBalance().first())
        assertEquals(Money(1000), credit.observeCard(card).first()!!.availableLimit)
        assertEquals(AlreadyPaid(payment.id), credit.payInstallment(installment.id, account, today))
        assertEquals(TransactionResult.Rejected(TransactionError.MANAGED_BY_INSTALLMENT), ledger.cancel(payment.id))
        assertEquals(TransactionResult.Rejected(TransactionError.MANAGED_BY_INSTALLMENT), ledger.update(payment.id, payment.details))
        assertEquals(CreditResult.Rejected(CreditError.HAS_PAID_INSTALLMENT), credit.cancelPurchase(purchase.id))

        assertEquals(Undone(payment.id), credit.undoInstallmentPayment(installment.id, payment.id))
        assertNull(credit.observeInstallmentPayment(installment.id).first())
        assertTrue(ledger.observeHistory().first().isEmpty())
        assertEquals(Exact(Money(100)), ledger.observeAccountBalance(account).first())
        assertEquals(Money(300), credit.observeCard(card).first()!!.availableLimit)
        assertEquals(installment, credit.observeInstallments(purchase.id).first().single())

        val repaid = (credit.payInstallment(installment.id, account, today) as Paid).transaction
        assertTrue(payment.id != repaid.id)
        assertEquals(Rejected(InstallmentPaymentError.PAYMENT_CHANGED), credit.undoInstallmentPayment(installment.id, payment.id))
        assertEquals(repaid, ledger.observeHistory().first().single())
    }

    @Test
    fun concurrentPaymentsShareASingleTransactionAndRestrictDeletion() = runBlocking {
        val (_, installment) = purchase(700)
        val results = List(2) { async { credit.payInstallment(installment.id, account, today) } }.awaitAll()
        val payment = results.filterIsInstance<Paid>().single().transaction
        assertEquals(AlreadyPaid(payment.id), results.filterIsInstance<AlreadyPaid>().single())
        assertEquals(payment, ledger.observeHistory().first().single())

        val entity = database.transactionDao().find(payment.id.toString())!!
        assertTrue(runCatching {
            database.transactionDao().insert(entity.copy(id = UUID.randomUUID().toString()))
        }.exceptionOrNull() is SQLiteConstraintException)
        assertTrue(runCatching {
            execute("DELETE FROM credit_installments WHERE id = ?", installment.id.toString())
        }.exceptionOrNull() is SQLiteConstraintException)
    }

    @Test
    fun failureAfterStateChangesRollsBackPaymentAndUndo() = runBlocking {
        val (purchase, installment) = purchase(700)
        execute(
            """
            CREATE TRIGGER fail_payment BEFORE INSERT ON financial_transactions
            WHEN NEW.installmentId IS NOT NULL BEGIN SELECT RAISE(ABORT, 'test payment failure'); END
            """.trimIndent(),
        )
        assertTrue(runCatching {
            credit.payInstallment(installment.id, account, today)
        }.exceptionOrNull() is SQLiteConstraintException)
        assertEquals(installment, credit.observeInstallments(purchase.id).first().single())
        assertTrue(ledger.observeHistory().first().isEmpty())
        assertEquals(Money(300), credit.observeCard(card).first()!!.availableLimit)

        execute("DROP TRIGGER fail_payment")
        val payment = (credit.payInstallment(installment.id, account, today) as Paid).transaction
        execute(
            """
            CREATE TRIGGER fail_undo BEFORE DELETE ON financial_transactions
            WHEN OLD.installmentId IS NOT NULL BEGIN SELECT RAISE(ABORT, 'test undo failure'); END
            """.trimIndent(),
        )
        assertTrue(runCatching {
            credit.undoInstallmentPayment(installment.id, payment.id)
        }.exceptionOrNull() is SQLiteConstraintException)
        assertEquals(InstallmentStatus.PAID, credit.observeInstallments(purchase.id).first().single().status)
        assertEquals(payment, ledger.observeHistory().first().single())
        assertEquals(Money(1000), credit.observeCard(card).first()!!.availableLimit)
    }

    @Test
    fun paymentAndPurchaseCancellationCannotBothWin() = runBlocking {
        // Cover both submission orders without depending on a particular scheduler.
        repeat(2) { attempt ->
            val (purchase, installment) = purchase(200)
            val pay = suspend { credit.payInstallment(installment.id, account, today) }
            val cancel = suspend { credit.cancelPurchase(purchase.id) }
            val (paymentResult, cancelResult) = if (attempt == 0) {
                val paying = async { pay() }
                val cancelling = async { cancel() }
                paying.await() to cancelling.await()
            } else {
                val cancelling = async { cancel() }
                val paying = async { pay() }
                paying.await() to cancelling.await()
            }
            val final = credit.observeInstallments(purchase.id).first().single()
            val link = credit.observeInstallmentPayment(installment.id).first()
            if (paymentResult is Paid) {
                assertEquals(CreditResult.Rejected(CreditError.HAS_PAID_INSTALLMENT), cancelResult)
                assertEquals(InstallmentStatus.PAID, final.status)
                assertEquals(paymentResult.transaction, link)
                assertEquals(PurchaseStatus.ACTIVE, credit.observePurchases(card).first().single { it.id == purchase.id }.status)
            } else {
                assertTrue(cancelResult is CreditResult.Saved)
                assertEquals(Rejected(InstallmentPaymentError.PURCHASE_CANCELLED), paymentResult)
                assertEquals(InstallmentStatus.CANCELLED, final.status)
                assertNull(link)
                assertEquals(PurchaseStatus.CANCELLED, credit.observePurchases(card).first().single { it.id == purchase.id }.status)
            }
        }
    }

    @Test
    fun undoDoesNotOvercommitCreditAlreadyReusedByAnotherPurchase() = runBlocking {
        val (purchase, installment) = purchase(700)
        val payment = (credit.payInstallment(installment.id, account, today) as Paid).transaction
        purchase(600)
        assertEquals(
            Rejected(InstallmentPaymentError.INSUFFICIENT_LIMIT_TO_UNDO),
            credit.undoInstallmentPayment(installment.id, payment.id),
        )
        assertEquals(InstallmentStatus.PAID, credit.observeInstallments(purchase.id).first().single().status)
        assertEquals(payment, credit.observeInstallmentPayment(installment.id).first())
        assertEquals(Money(400), credit.observeCard(card).first()!!.availableLimit)
    }

    @Test
    fun rejectsArchivedAccountAndUsesOriginalCategoryEvenIfLaterArchived() = runBlocking {
        val (_, installment) = purchase(200)
        val archivedAccount = accounts.create(AccountDetails("Antiga", AccountType.SAVINGS, Money(0), today))
        assertTrue(accounts.archive(archivedAccount))
        assertEquals(Rejected(InstallmentPaymentError.ACCOUNT_ARCHIVED), credit.payInstallment(installment.id, archivedAccount, today))
        assertEquals(Rejected(InstallmentPaymentError.PAYMENT_DATE_IN_FUTURE), credit.payInstallment(installment.id, account, today.plusDays(1)))
        assertTrue(categories.archive(category))
        val paid = (credit.payInstallment(installment.id, account, today.minusDays(1)) as Paid).transaction
        assertEquals(category, paid.details.categoryId)
        assertEquals(today.minusDays(1), paid.details.occurredOn)
    }

    private suspend fun purchase(cents: Long): Pair<Purchase, Installment> {
        val id = UUID.randomUUID()
        val draft = PurchaseDraft(card, category, "Compra", Money(cents), 1, today.minusDays(7))
        val purchase = (credit.createPurchase(id, draft) as CreditResult.Saved).value
        return purchase to credit.observeInstallments(id).first().single()
    }

    private suspend fun execute(sql: String, vararg args: String) = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.execSQL(sql, args)
    }
}
