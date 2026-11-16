package com.fluxa.personal.data.database

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.repository.CategoryKindInUse
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.data.repository.CreditRepository
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.CreditCardDetails
import com.fluxa.personal.domain.model.CreditCardSummary
import com.fluxa.personal.domain.model.CreditError
import com.fluxa.personal.domain.model.CreditResult.Rejected
import com.fluxa.personal.domain.model.CreditResult.Saved
import com.fluxa.personal.domain.model.InstallmentStatus
import com.fluxa.personal.domain.model.PurchaseDraft
import com.fluxa.personal.domain.model.PurchaseStatus
import com.fluxa.personal.domain.model.TransactionDirection.EXPENSE
import com.fluxa.personal.domain.model.TransactionDirection.INCOME
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreditPersistenceTest {
    private val clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC)
    private val day = LocalDate.of(2026, 11, 9)
    private val name = "credit-${UUID.randomUUID()}"
    private lateinit var context: Context
    private lateinit var database: FluxaDatabase
    private lateinit var credit: CreditRepository
    private lateinit var categories: CategoryRepository
    private lateinit var expense: UUID

    @Before
    fun setUp() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = FluxaDatabase.create(context, name)
        credit = CreditRepository(database, clock)
        categories = CategoryRepository(database.categoryDao(), clock)
        expense = categories.create(CategoryDetails("Compras", EXPENSE))
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun createsSingleAndSplitPurchasesAndOnlyNewPurchasesUseUpdatedCalendar() = runBlocking {
        val details = cardDetails(20000)
        val card = (credit.createCard(details) as Saved).value
        assertEquals("Cartão", card.details.name)
        assertEquals(card.id, credit.observeCards().first().single().card.id)
        val one = UUID(0, 1)
        val split = UUID(0, 2)
        val latest = UUID(0, 3)
        val single = (credit.createPurchase(one, draft(card.id, 100, 1)) as Saved).value
        assertEquals(LocalDate.of(2026, 11, 17), single.firstDueDate)
        assertEquals(Money(100), credit.observeInstallments(one).first().single().amount)
        assertTrue(credit.createPurchase(split, draft(card.id, 10001, 3)) is Saved)
        val original = credit.observeInstallments(split).first()
        assertEquals(listOf(3333L, 3334L, 3334L), original.map { it.amount.cents })
        assertEquals(10001L, original.sumOf { it.amount.cents })
        assertEquals(listOf(1, 2, 3), original.map { it.number })
        assertTrue(original.all { it.status == InstallmentStatus.PENDING })
        assertEquals(List(3) { single.firstDueDate.plusMonths(it.toLong()) }, original.map { it.dueDate })

        assertTrue(credit.updateCard(card.id, details.copy(closingDay = BillingDay(8), dueDay = BillingDay(25))) is Saved)
        val newPurchase = (credit.createPurchase(latest, draft(card.id, 99, 1)) as Saved).value
        assertEquals(LocalDate.of(2026, 12, 25), newPurchase.firstDueDate)
        assertEquals(original, credit.observeInstallments(split).first())
        assertEquals(listOf(one, split, latest), credit.observePurchases(card.id, PurchaseStatus.ACTIVE).first().map { it.id })
        assertEquals(Money(10200), credit.observeCard(card.id).first()!!.committedAmount)
        assertEquals(Money(9800), credit.observeCard(card.id).first()!!.availableLimit)
    }

    @Test
    fun rejectsInvalidReferencesAmountsAndArchivedCardsWithoutLosingHistory() = runBlocking {
        val card = (credit.createCard(cardDetails(1000)) as Saved).value
        val valid = draft(card.id, 100, 1)
        val id = UUID.randomUUID()
        val income = categories.create(CategoryDetails("Entrada", INCOME))
        val archived = categories.create(CategoryDetails("Inativa", EXPENSE))
        assertTrue(categories.archive(archived))
        assertEquals(Rejected(CreditError.CATEGORY_NOT_EXPENSE), credit.createPurchase(id, valid.copy(categoryId = income)))
        assertEquals(Rejected(CreditError.CATEGORY_ARCHIVED), credit.createPurchase(id, valid.copy(categoryId = archived)))
        assertEquals(Rejected(CreditError.INVALID_INSTALLMENT_COUNT), credit.createPurchase(id, valid.copy(installmentCount = 101)))
        assertEquals(Rejected(CreditError.INSUFFICIENT_LIMIT), credit.createPurchase(id, valid.copy(total = Money(1001))))
        assertTrue(credit.observePurchases(card.id).first().isEmpty())
        assertTrue(credit.createPurchase(id, valid) is Saved)
        assertEquals(Rejected(CreditError.DUPLICATE_PURCHASE_ID), credit.createPurchase(id, valid))
        assertTrue(credit.archiveCard(card.id))
        assertFalse(credit.archiveCard(card.id))
        assertTrue(categories.archive(expense))
        assertEquals(Rejected(CreditError.CARD_ARCHIVED), credit.createPurchase(UUID.randomUUID(), valid))
        assertTrue(credit.observeCards().first().isEmpty())
        assertEquals(card.id, credit.observeCards(includeArchived = true).first().single().card.id)
        assertEquals(id, credit.observePurchases(card.id).first().single().id)
        assertEquals(Money(900), credit.observeCard(card.id).first()!!.availableLimit)
        val error = runCatching { categories.update(expense, CategoryDetails("Outra", INCOME)) }.exceptionOrNull()
        assertTrue(error is CategoryKindInUse)
        assertNull(credit.observeCard(UUID.randomUUID()).first())
    }

    @Test
    fun concurrentPurchasesUseTheLatestLimitAndCancellationReleasesItReactively() = runBlocking {
        val details = cardDetails(1000)
        val card = (credit.createCard(details) as Saved).value
        val emissions = Channel<CreditCardSummary>(Channel.UNLIMITED)
        val observer = launch { credit.observeCard(card.id).filterNotNull().collect { emissions.send(it) } }
        try {
            withTimeout(15_000) {
                assertEquals(Money(1000), emissions.receive().availableLimit)
                val attempts = List(2) { async { credit.createPurchase(UUID.randomUUID(), draft(card.id, 700, 2)) } }.awaitAll()
                val purchase = (attempts.single { it is Saved } as Saved).value
                assertEquals(1, attempts.count { it == Rejected(CreditError.INSUFFICIENT_LIMIT) })
                assertEquals(Money(300), emissions.receive().availableLimit)
                assertEquals(Rejected(CreditError.LIMIT_BELOW_COMMITTED), credit.updateCard(card.id, details.copy(totalLimit = Money(699))))
                assertTrue(credit.updateCard(card.id, details.copy(totalLimit = Money(700))) is Saved)
                assertEquals(Money(0), emissions.receive().availableLimit)

                assertTrue(credit.cancelPurchase(purchase.id) is Saved)
                assertEquals(Money(700), emissions.receive().availableLimit)
                assertEquals(Rejected(CreditError.PURCHASE_CANCELLED), credit.cancelPurchase(purchase.id))
                assertTrue(credit.observePurchases(card.id, PurchaseStatus.ACTIVE).first().isEmpty())
                assertEquals(purchase.id, credit.observePurchases(card.id, PurchaseStatus.CANCELLED).first().single().id)
                assertTrue(credit.observeInstallments(purchase.id).first().all { it.status == InstallmentStatus.CANCELLED })
            }
        } finally {
            observer.cancel()
            emissions.cancel()
        }
    }

    @Test
    fun failedSecondInstallmentRollsBackPurchaseAndAllowRetryWithSameId() = runBlocking {
        val id = UUID.randomUUID()
        val card = (credit.createCard(cardDetails(1000)) as Saved).value
        execute(
            """
            CREATE TRIGGER fail_second_installment BEFORE INSERT ON credit_installments
            WHEN NEW.number = 2 BEGIN SELECT RAISE(ABORT, 'test installment failure'); END
            """.trimIndent(),
        )
        val attempted = draft(card.id, 1000, 3)
        val error = runCatching { credit.createPurchase(id, attempted) }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)
        assertTrue(credit.observePurchases(card.id).first().isEmpty())
        assertTrue(credit.observeInstallments(id).first().isEmpty())
        assertEquals(Money(1000), credit.observeCard(card.id).first()!!.availableLimit)
        execute("DROP TRIGGER fail_second_installment")
        assertTrue(credit.createPurchase(id, attempted) is Saved)
        assertEquals(Money(0), credit.observeCard(card.id).first()!!.availableLimit)
    }

    @Test
    fun paidInstallmentBlocksCancellationAndForeignKeysProtectHistory() = runBlocking {
        val id = UUID.randomUUID()
        val card = (credit.createCard(cardDetails(1000)) as Saved).value
        assertTrue(credit.createPurchase(id, draft(card.id, 500, 2)) is Saved)
        execute("UPDATE credit_installments SET status = 'PAID' WHERE purchaseId = ? AND number = 1", id.toString())
        assertEquals(Rejected(CreditError.HAS_PAID_INSTALLMENT), credit.cancelPurchase(id))
        assertEquals(Money(250), credit.observeCard(card.id).first()!!.committedAmount)
        val purchase = credit.observePurchases(card.id, PurchaseStatus.ACTIVE).first().single()
        assertEquals(id, purchase.id)
        assertEquals(listOf(InstallmentStatus.PAID, InstallmentStatus.PENDING), credit.observeInstallments(id).first().map { it.status })
        for ((table, key) in listOf("credit_cards" to card.id, "credit_purchases" to id, "categories" to expense)) {
            val error = runCatching { execute("DELETE FROM $table WHERE id = ?", key.toString()) }.exceptionOrNull()
            assertTrue(error is SQLiteConstraintException)
        }
        val error = runCatching {
            execute("UPDATE credit_installments SET number = 1 WHERE purchaseId = ? AND number = 2", id.toString())
        }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)
    }

    private suspend fun execute(sql: String, vararg args: String) = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.execSQL(sql, args)
    }

    private fun cardDetails(cents: Long) = CreditCardDetails("  Cartão  ", Money(cents), BillingDay(10), BillingDay(17))

    private fun draft(cardId: UUID, cents: Long, count: Int) =
        PurchaseDraft(cardId, expense, "Compra", Money(cents), count, day)
}
