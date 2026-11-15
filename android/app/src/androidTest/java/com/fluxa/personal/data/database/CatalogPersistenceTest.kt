package com.fluxa.personal.data.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.data.database.entity.CategoryEntity
import com.fluxa.personal.data.repository.AccountRepository
import com.fluxa.personal.data.repository.CategoryRepository
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.AccountType
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogPersistenceTest {
    private lateinit var database: FluxaDatabase
    private lateinit var accounts: AccountRepository
    private lateinit var categories: CategoryRepository
    private val now = Instant.parse("2026-11-15T14:00:00.123456789Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val openingDate = LocalDate.of(2024, 2, 29)

    @Before
    fun openDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, FluxaDatabase::class.java).build()
        accounts = AccountRepository(database.accountDao(), clock)
        categories = CategoryRepository(database.categoryDao(), clock)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun accountCrudKeepsItsIdentityAndArchivedRow() = runBlocking {
        val details = AccountDetails("  Reserva  ", AccountType.CHECKING, Money(-11234), openingDate)
        withChanges(accounts.observeAll()) { changes ->
            assertTrue(changes.receive().isEmpty())
            val id = accounts.create(details)
            val created = changes.receive().single()
            assertEquals(id, created.id)
            assertEquals(details.copy(name = "Reserva"), created.details)
            assertEquals(now, created.createdAt)

            val updated = details.copy(
                name = "Carteira",
                type = AccountType.WALLET,
                openingBalance = Money(0),
                openingDate = openingDate.plusDays(1),
                includeInAvailable = false,
            )
            assertTrue(accounts.update(id, updated))
            assertEquals(updated, changes.receive().single().details)

            assertTrue(accounts.archive(id))
            assertTrue(changes.receive().isEmpty())
            assertFalse(accounts.archive(id))
            val archived = accounts.observe(id).first()!!
            assertEquals(id, archived.id)
            assertEquals(updated, archived.details)
            assertEquals(now, archived.archivedAt)
            assertEquals(listOf(archived), accounts.observeAll(includeArchived = true).first())
        }
    }

    @Test
    fun accountNamesCanRepeatAndOnlyOneConcurrentArchiveSucceeds() = runBlocking {
        val details = AccountDetails("Banco", AccountType.SAVINGS, Money(100), openingDate)
        val firstId = accounts.create(details)
        val secondId = accounts.create(details)
        assertNotEquals(firstId, secondId)
        assertEquals(2, accounts.observeAll().first().size)

        val results = listOf(async { accounts.archive(firstId) }, async { accounts.archive(firstId) }).awaitAll()
        assertEquals(1, results.count { it })
        assertEquals(secondId, accounts.observeAll().first().single().id)

        val missing = UUID.randomUUID()
        assertNull(accounts.observe(missing).first())
        assertFalse(accounts.update(missing, details))
        assertFalse(accounts.archive(missing))
    }

    @Test
    fun categoryCrudEmitsEditsArchiveAndReactivationWithoutReplacingId() = runBlocking {
        withChanges(categories.observeAll(kind = TransactionDirection.EXPENSE)) { changes ->
            assertTrue(changes.receive().isEmpty())
            val original = CategoryDetails("  Mercado  ", TransactionDirection.EXPENSE)
            val id = categories.create(original)
            assertEquals(original.copy(name = "Mercado"), changes.receive().single().details)

            val updated = original.copy(name = "Alimentação", icon = "tag", color = "#53654D")
            assertTrue(categories.update(id, updated))
            assertEquals(updated, changes.receive().single().details)

            assertTrue(categories.archive(id))
            assertTrue(changes.receive().isEmpty())
            assertFalse(categories.archive(id))
            val archived = categories.observe(id).first()!!
            assertEquals(now, archived.archivedAt)
            assertEquals(archived, categories.observeAll(includeArchived = true).first().single())

            assertTrue(categories.reactivate(id))
            assertFalse(categories.reactivate(id))
            val restored = changes.receive().single()
            assertEquals(id, restored.id)
            assertEquals(updated, restored.details)
            assertNull(restored.archivedAt)

            assertTrue(categories.update(id, updated.copy(kind = TransactionDirection.INCOME)))
            assertTrue(changes.receive().isEmpty())
            assertEquals(id, categories.observeAll(kind = TransactionDirection.INCOME).first().single().id)
        }
    }

    @Test
    fun categoryNamesCanRepeatButPrimaryKeysCannot() = runBlocking {
        val income = CategoryDetails("Outros", TransactionDirection.INCOME)
        val expense = income.copy(kind = TransactionDirection.EXPENSE)
        val id = categories.create(income)
        val sameNameId = categories.create(income)
        val expenseId = categories.create(expense)

        assertEquals(setOf(id, sameNameId), categories.observeAll(kind = income.kind).first().map { it.id }.toSet())
        assertEquals(expenseId, categories.observeAll(kind = expense.kind).first().single().id)
        val duplicate = CategoryEntity(id.toString(), "Outro registro", income.kind.name, null, null, null)
        val error = runCatching { database.categoryDao().insert(duplicate) }.exceptionOrNull()
        assertTrue(error is SQLiteConstraintException)
        assertEquals(income, categories.observe(id).first()!!.details)

        val missing = UUID.randomUUID()
        assertNull(categories.observe(missing).first())
        assertFalse(categories.update(missing, income))
        assertFalse(categories.archive(missing))
        assertFalse(categories.reactivate(missing))
    }

    private suspend fun <T> withChanges(
        flow: Flow<T>,
        block: suspend (ReceiveChannel<T>) -> Unit,
    ) = coroutineScope {
        val changes = Channel<T>(Channel.UNLIMITED)
        val collector = launch { flow.collect { changes.send(it) } }
        try {
            withTimeout(15_000) { block(changes) }
        } finally {
            collector.cancel()
            changes.cancel()
        }
    }
}
