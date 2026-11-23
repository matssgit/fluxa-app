package com.fluxa.personal.backup

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fluxa.personal.core.date.BillingDay
import com.fluxa.personal.core.date.FinancialCalendar
import com.fluxa.personal.core.money.Money
import com.fluxa.personal.core.preferences.LocalPreferences
import com.fluxa.personal.data.database.FluxaDatabase
import com.fluxa.personal.data.database.entity.TransactionEntity
import com.fluxa.personal.data.repository.*
import com.fluxa.personal.domain.model.*
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "backup-${UUID.randomUUID()}"
    private val clock = Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC)
    private val calendar = FinancialCalendar(clock) { ZoneOffset.ofHours(-3) }
    private val today = LocalDate.of(2026, 11, 19)
    private lateinit var db: FluxaDatabase
    private val settings = TestSettings()

    private class TestSettings : BackupSettingsStore {
        var value = PortableSettingsV1("DARK", true)
        var failApply = false
        override suspend fun read() = value
        override suspend fun apply(settings: PortableSettingsV1) {
            if (failApply) throw IOException("Settings unavailable")
            value = settings
        }
    }

    private fun service() = FluxaBackupService(db, settings, "0.1.0", clock)
    private fun safe() = SafeToSpendRepository(db, GoalRepository(db, clock, calendar), clock, calendar)

    @Before
    fun setup() { db = FluxaDatabase.create(context, databaseName) }

    @After
    fun cleanup() { db.close(); context.deleteDatabase(databaseName) }

    @Test
    fun completeRoundTripPreservesIdsRelationsHistoryAndSafeToSpendAcrossReopeningAtVolume() = runBlocking {
        seed()
        val before = BackupPersistence(db).snapshot()
        val expectedSafe = safe().observeSafeToSpend().first()
        val output = ByteArrayOutputStream()
        val exported = service().export(password(), output)
        assertTrue(exported.toString(), exported is BackupResult.Success)
        val bytes = output.toByteArray()
        assertTrue(bytes.size > 10_000)
        val imported = success(service().validate(password(), ByteArrayInputStream(bytes)))
        val sentinel = AccountRepository(db.accountDao(), clock).create(
            AccountDetails("Será substituída", AccountType.WALLET, Money(1), today),
        )
        settings.value = PortableSettingsV1("LIGHT", false)
        val emergency = ByteArrayOutputStream()
        assertEquals(BackupResult.Success(RestoreReceipt(true)), service().restore(imported, password(), emergency))
        assertEquals(PortableSettingsV1("DARK", true), settings.value)
        assertTrue("Financial snapshot must stay identical", before == BackupPersistence(db).snapshot())
        val preventive = decodeEncrypted(emergency.toByteArray())
        assertTrue(preventive.data.tables.getValue("accounts").any { it.fields.text("id") == sentinel.toString() })
        assertEquals(PortableSettingsV1("LIGHT", false), preventive.data.settings)
        assertTrue("SafeToSpend must stay identical", expectedSafe == safe().observeSafeToSpend().first())

        db.close()
        db = FluxaDatabase.create(context, databaseName)
        assertTrue("Financial snapshot must stay identical", before == BackupPersistence(db).snapshot())
        assertTrue("SafeToSpend must stay identical", expectedSafe == safe().observeSafeToSpend().first())
        val restored = BackupPersistence(db).snapshot()
        assertEquals(before.getValue("financial_transactions").map { it.fields.text("id") },
            restored.getValue("financial_transactions").map { it.fields.text("id") })
        assertTrue(restored.getValue("financial_transactions").any { it.fields.optional("installmentId") != null })
        assertTrue(restored.getValue("credit_purchases").any { it.fields.optional("subscriptionId") != null })
        assertTrue(restored.getValue("subscription_resolutions").any { it.fields.text("status") == "PAID" })
        assertTrue(restored.getValue("goals").any { it.fields.optional("archivedAt") != null })
        assertEquals(12, restored.size)
    }

    @Test
    fun aFailureAfterDeletionRollsBackAllTablesAndKeepsSettingsUntouched() = runBlocking {
        seed(volume = false)
        val incoming = exported()
        val sentinel = AccountRepository(db.accountDao(), clock).create(
            AccountDetails("Atual", AccountType.WALLET, Money(100), today),
        )
        val before = BackupPersistence(db).snapshot()
        withContext(Dispatchers.IO) {
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_restore BEFORE INSERT ON financial_transactions BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
        }
        settings.value = PortableSettingsV1("SYSTEM", false)
        val emergency = ByteArrayOutputStream()
        assertEquals(BackupResult.Failure(BackupError.RESTORE_FAILED, preventiveBackupCreated = true), service().restore(incoming, password(), emergency))
        assertTrue("Financial snapshot must stay identical", before == BackupPersistence(db).snapshot())
        assertEquals(PortableSettingsV1("SYSTEM", false), settings.value)
        assertTrue(decodeEncrypted(emergency.toByteArray()).data.tables.getValue("accounts")
            .any { it.fields.text("id") == sentinel.toString() })
    }

    @Test
    fun preventiveWriteAndCloseFailuresCannotReplaceTheCurrentDatabase() = runBlocking {
        seed(volume = false)
        val incoming = exported()
        val before = BackupPersistence(db).snapshot()
        for (failOnClose in listOf(false, true)) {
            val failing = object : OutputStream() {
                override fun write(value: Int) { if (!failOnClose) throw IOException("write") }
                override fun close() { if (failOnClose) throw IOException("close") }
            }
            assertEquals(BackupResult.Failure(BackupError.RESTORE_FAILED, cleanup = BackupCleanup.FAILED), service().restore(incoming, password(), failing))
            assertTrue("Financial snapshot must stay identical", before == BackupPersistence(db).snapshot())
        }
    }

    @Test
    fun editsDuringPreventiveOutputRejectReplacementWithoutLosingTheEdit() = runBlocking {
        seed(volume = false)
        val incoming = exported()
        var changed = false
        val output = object : ByteArrayOutputStream() {
            override fun flush() {
                super.flush()
                if (!changed) {
                    changed = true
                    runBlocking(Dispatchers.IO) {
                        AccountRepository(db.accountDao(), clock).create(
                            AccountDetails("Edição concorrente", AccountType.CHECKING, Money(123), today),
                        )
                    }
                }
            }
        }
        assertEquals(BackupResult.Failure(BackupError.RESTORE_CONFLICT, preventiveBackupCreated = true), service().restore(incoming, password(), output))
        assertTrue(AccountRepository(db.accountDao()).observeAll().first().any { it.details.name == "Edição concorrente" })
    }

    @Test
    fun settingsFailureIsSecondaryAndSuccessfulRestoreInvalidatesExistingRoomFlows() = runBlocking {
        seed(volume = false)
        val incoming = exported()
        val sentinel = AccountRepository(db.accountDao(), clock).create(
            AccountDetails("Temporária", AccountType.WALLET, Money(123), today),
        )
        val observing = async {
            withTimeout(10_000) { AccountRepository(db.accountDao()).observe(sentinel).first { it == null } }
        }
        settings.failApply = true
        assertEquals(BackupResult.Success(RestoreReceipt(false)), service().restore(incoming, password(), ByteArrayOutputStream()))
        assertNull(observing.await())
        assertFalse(BackupPersistence(db).snapshot().getValue("accounts").any { it.fields.text("id") == sentinel.toString() })
    }

    @Test
    fun invalidArchivesNeverChangeTheCurrentFinancialState() = runBlocking {
        seed(volume = false)
        val original = ByteArrayOutputStream().also { success(service().export(password(), it)) }.toByteArray()
        val before = BackupPersistence(db).snapshot()
        val variants = listOf(
            byteArrayOf(), "invalid".toByteArray(),
            mutate(original) { manifest, _ -> manifest.addProperty("formatVersion", 999) },
            mutate(original, checksum = false) { _, data -> data.addProperty("unexpected", true) },
            mutate(original) { _, data ->
                data.getAsJsonObject("tables").getAsJsonArray("accounts")[0].asJsonObject.addProperty("type", "BAD")
            },
            mutate(original) { _, data ->
                data.getAsJsonObject("tables").getAsJsonArray("accounts")[0].asJsonObject.addProperty("openingBalanceCents",
                    java.math.BigInteger("9223372036854775808"))
            },
            mutate(original) { _, data ->
                val rows = data.getAsJsonObject("tables").getAsJsonArray("accounts")
                rows.add(rows[0].deepCopy())
            },
            mutate(original) { _, data ->
                data.getAsJsonObject("tables").getAsJsonArray("goal_movements")[0].asJsonObject
                    .addProperty("goalId", UUID.randomUUID().toString())
            },
        )
        for (bytes in variants) {
            assertTrue(service().validate(password(), ByteArrayInputStream(bytes)) is BackupResult.Failure)
            assertTrue("Financial snapshot must stay identical", before == BackupPersistence(db).snapshot())
        }
    }

    @Test
    fun portableDataStoreSettingsCanBeAppliedAndReadAsOneEdit() = runBlocking {
        val preferences = LocalPreferences(context)
        val previous = preferences.read()
        try {
            val portable = PortableSettingsV1("DARK", true)
            preferences.apply(portable)
            assertEquals(portable, preferences.read())
            assertEquals(setOf("theme", "hideAmounts"),
                BackupCodec.dataJson(BackupDataV1(BackupFormatV1.tables.associate { it.name to emptyList() }, portable))
                    .getAsJsonObject("settings").keySet())
        } finally {
            preferences.apply(previous)
        }
    }

    @Test
    fun androidAutomaticBackupAndDeviceTransferExcludeDatabaseAndDataStore() {
        val flags = context.applicationInfo.flags
        assertFalse(flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP != 0)
        val parser = context.resources.getXml(com.fluxa.personal.R.xml.data_extraction_rules)
        val excluded = mutableMapOf<String, MutableSet<String>>()
        var section = ""
        try {
            while (parser.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    if (parser.name in setOf("cloud-backup", "device-transfer")) section = parser.name
                    if (parser.name == "exclude" && parser.getAttributeValue(null, "path") == ".") {
                        excluded.getOrPut(section) { mutableSetOf() }.add(parser.getAttributeValue(null, "domain"))
                    }
                }
                parser.next()
            }
        } finally {
            parser.close()
        }
        for (mode in listOf("cloud-backup", "device-transfer")) {
            assertTrue(excluded.getValue(mode).containsAll(setOf("database", "file", "sharedpref", "root", "device_database", "device_file")))
        }
    }

    @Test
    fun wrongPasswordAndAuthenticatedCorruptionKeepRoomAndSettingsIntactAndClearPasswords() = runBlocking {
        seed(volume = false)
        val output = ByteArrayOutputStream()
        val exportPassword = password()
        success(service().export(exportPassword, output))
        assertTrue(exportPassword.all { it == '\u0000' })
        val original = output.toByteArray()
        val before = BackupPersistence(db).snapshot()
        val previousSettings = settings.value
        val wrong = "outra frase de teste".toCharArray()
        assertEquals(BackupResult.Failure(BackupError.INVALID_PASSWORD), service().validate(wrong, ByteArrayInputStream(original)))
        assertTrue(wrong.all { it == '\u0000' })
        for (offset in listOf(BackupCrypto.HEADER_BYTES, original.lastIndex, 24, 40)) {
            val changed = original.copyOf()
            changed[offset] = (changed[offset].toInt() xor 1).toByte()
            val secret = password()
            assertEquals(BackupResult.Failure(BackupError.INVALID_PASSWORD), service().validate(secret, ByteArrayInputStream(changed)))
            assertTrue(secret.all { it == '\u0000' })
            assertTrue("Financial snapshot must stay identical", before == BackupPersistence(db).snapshot())
            assertEquals(previousSettings, settings.value)
        }
        assertEquals(BackupResult.Failure(BackupError.PASSWORD_REQUIRED), service().export(charArrayOf(), ByteArrayOutputStream()))
        val prepared = exported()
        val emergency = ByteArrayOutputStream()
        assertEquals(BackupResult.Failure(BackupError.PASSWORD_REQUIRED, cleanup = BackupCleanup.FAILED), service().restore(prepared, charArrayOf(), emergency))
        assertEquals(0, emergency.size())
        assertTrue("Financial snapshot must stay identical", before == BackupPersistence(db).snapshot())
    }

    @Test
    fun plaintextDevelopmentBackupsAreRejectedByTheProductionService() = runBlocking {
        seed(volume = false)
        val before = BackupPersistence(db).snapshot()
        val legacy = BackupCodec.encode(BackupDataV1(before, settings.value), clock.instant(), "test")
        try {
            assertEquals(BackupResult.Failure(BackupError.NOT_FLUXA),
                service().validate(password(), ByteArrayInputStream(legacy)))
            assertTrue("Financial snapshot must stay identical", before == BackupPersistence(db).snapshot())
        } finally {
            legacy.fill(0)
        }
    }

    @Test
    fun incompletePreventiveDocumentsAreDeletedOrReportedAndRoomNeverChanges() = runBlocking {
        seed(volume = false)
        val imported = exported()
        AccountRepository(db.accountDao(), clock).create(AccountDetails("Estado A", AccountType.WALLET, Money(123), today))
        val before = BackupPersistence(db).snapshot()
        val beforeSettings = settings.value
        for (fault in listOf("open", "zero", "header", "ciphertext", "flush", "close")) {
            val destination = FaultDocument(fault)
            val result = service().restore(imported, password(), destination)
            assertEquals(BackupResult.Failure(BackupError.RESTORE_FAILED, cleanup = BackupCleanup.DELETED), result)
            assertEquals(1, destination.deletes)
            assertEquals(0, destination.bytes.size())
            assertEquals(if (fault == "open") 0 else 1, destination.closes)
            assertTrue("Failed preventive output must preserve Room", before == BackupPersistence(db).snapshot())
            assertEquals(beforeSettings, settings.value)
        }
        for (throwsOnDelete in listOf(false, true)) {
            val destination = FaultDocument("ciphertext", acceptsDelete = false, throwsOnDelete = throwsOnDelete)
            assertEquals(BackupResult.Failure(BackupError.RESTORE_FAILED, cleanup = BackupCleanup.FAILED),
                service().restore(imported, password(), destination))
            assertEquals(1, destination.deletes)
            assertTrue("Refused cleanup must preserve Room", before == BackupPersistence(db).snapshot())
        }
    }

    @Test
    fun aCompletedPreventiveIsClosedOnceAndRetainedOnSuccessOrLaterCommitFailure() = runBlocking {
        seed(volume = false)
        val expected = BackupPersistence(db).snapshot()
        val imported = exported()
        for (failCommit in listOf(false, true)) {
            val sentinel = AccountRepository(db.accountDao(), clock).create(
                AccountDetails("Estado A", AccountType.WALLET, Money(123), today),
            )
            val before = BackupPersistence(db).snapshot()
            if (failCommit) withContext(Dispatchers.IO) {
                db.openHelper.writableDatabase.execSQL(
                    "CREATE TRIGGER prevent_commit BEFORE INSERT ON financial_transactions BEGIN SELECT RAISE(ABORT, 'test'); END",
                )
            }
            val destination = FaultDocument()
            val result = service().restore(imported, password(), destination)
            assertEquals(1, destination.closes)
            assertEquals(0, destination.deletes)
            val preventive = decodeEncrypted(destination.bytes.toByteArray())
            assertTrue("Completed preventive must contain state A",
                preventive.data.tables.getValue("accounts").any { it.fields.text("id") == sentinel.toString() })
            if (failCommit) {
                assertEquals(BackupResult.Failure(BackupError.RESTORE_FAILED, preventiveBackupCreated = true), result)
                assertTrue("Commit rollback must retain state A", before == BackupPersistence(db).snapshot())
            } else {
                assertEquals(BackupResult.Success(RestoreReceipt(true, preventiveBackupCreated = true)), result)
                assertTrue("Successful restore must load state B", expected == BackupPersistence(db).snapshot())
            }
        }
    }

    @Test
    fun safProviderOpenFailureAttemptsDeleteOfTheExactSelectedUri() = runBlocking {
        seed(volume = false)
        val imported = exported()
        val before = BackupPersistence(db).snapshot()
        for (acceptsDelete in listOf(true, false)) {
            var deletedUri: android.net.Uri? = null
            var deleteCalls = 0
            val provider = object : android.content.ContentProvider() {
                override fun onCreate() = true
                override fun getType(uri: android.net.Uri) = FLUXA_BACKUP_MIME
                override fun query(uri: android.net.Uri, projection: Array<out String>?, selection: String?,
                    selectionArgs: Array<out String>?, sortOrder: String?): android.database.Cursor? = null
                override fun insert(uri: android.net.Uri, values: android.content.ContentValues?): android.net.Uri? = null
                override fun update(uri: android.net.Uri, values: android.content.ContentValues?, selection: String?,
                    selectionArgs: Array<out String>?) = 0
                override fun delete(uri: android.net.Uri, selection: String?, selectionArgs: Array<out String>?): Int {
                    deletedUri = uri
                    deleteCalls++
                    return if (acceptsDelete) 1 else 0
                }
                override fun openFile(uri: android.net.Uri, mode: String): android.os.ParcelFileDescriptor =
                    throw java.io.FileNotFoundException("injected-provider-open")
            }
            val info = android.content.pm.ProviderInfo().apply {
                authority = "com.fluxa.test.preventive"
                exported = false
            }
            provider.attachInfo(context, info)
            val resolver = android.content.ContentResolver.wrap(provider)
            val uri = android.net.Uri.parse("content://com.fluxa.test.preventive/${UUID.randomUUID()}")
            val adapter = BackupDocuments(resolver, service())
            val expectedCleanup = if (acceptsDelete) BackupCleanup.DELETED else BackupCleanup.FAILED
            assertEquals(BackupResult.Failure(BackupError.RESTORE_FAILED, cleanup = expectedCleanup),
                adapter.restore(imported, password(), uri))
            assertEquals(uri, deletedUri)
            assertEquals(1, deleteCalls)
            assertTrue("Provider failure must preserve Room", before == BackupPersistence(db).snapshot())
        }
    }

    private class FaultDocument(
        private val fault: String = "",
        private val acceptsDelete: Boolean = true,
        private val throwsOnDelete: Boolean = false,
    ) : BackupDestination {
        val bytes = ByteArrayOutputStream()
        var closes = 0
        var deletes = 0
        override fun open(): OutputStream {
            if (fault == "open") throw IOException("injected-open")
            return object : OutputStream() {
                override fun write(value: Int) = write(byteArrayOf(value.toByte()), 0, 1)
                override fun write(source: ByteArray, offset: Int, length: Int) {
                    val limit = when (fault) {
                        "zero" -> 0
                        "header" -> 52
                        "ciphertext" -> 80
                        else -> Int.MAX_VALUE
                    }
                    if (bytes.size().toLong() + length > limit) {
                        bytes.write(source, offset, maxOf(0, limit - bytes.size()))
                        throw IOException("injected-write")
                    }
                    bytes.write(source, offset, length)
                }
                override fun flush() {
                    if (fault == "flush") throw IOException("injected-flush")
                }
                override fun close() {
                    closes++
                    if (fault == "close") throw IOException("injected-close")
                }
            }
        }
        override fun delete(): Boolean {
            deletes++
            if (throwsOnDelete) throw SecurityException("delete refused")
            if (acceptsDelete) bytes.reset()
            return acceptsDelete
        }
    }

    private suspend fun seed(volume: Boolean = true) {
        val accounts = AccountRepository(db.accountDao(), clock)
        val categories = CategoryRepository(db.categoryDao(), clock)
        val ledger = TransactionRepository(db, clock, calendar)
        val credit = CreditRepository(db, clock, calendar)
        val subscriptions = SubscriptionRepository(db, credit, clock, calendar)
        val goals = GoalRepository(db, clock, calendar)
        val main = accounts.create(AccountDetails("Banco", AccountType.CHECKING, Money(1_000_000), LocalDate.of(2024, 1, 1)))
        val excluded = accounts.create(AccountDetails("Arquivada", AccountType.SAVINGS, Money(50_000), today, false))
        val expense = categories.create(CategoryDetails("Compras", TransactionDirection.EXPENSE))
        val income = categories.create(CategoryDetails("Salário", TransactionDirection.INCOME))
        successTransaction(ledger.create(UUID.randomUUID(), TransactionDraft(TransactionDirection.INCOME, main, income, Money(100_000), today)))
        val cancelled = successTransaction(ledger.create(UUID.randomUUID(), TransactionDraft(
            TransactionDirection.EXPENSE, main, expense, Money(2000), today,
        )))
        assertTrue(ledger.cancel(cancelled.id) is TransactionResult.Saved)
        val card = (credit.createCard(CreditCardDetails("Cartão", Money(2_000_000), BillingDay(20), BillingDay(27))) as CreditResult.Saved).value
        val purchase = (credit.createPurchase(UUID.randomUUID(), PurchaseDraft(
            card.id, expense, "Parcelada", Money(60_000), 3, today,
        )) as CreditResult.Saved).value
        val part = credit.observeInstallments(purchase.id).first().first()
        assertTrue(credit.payInstallment(part.id, main, today) is InstallmentPaymentResult.Paid)
        val cancelledPurchase = (credit.createPurchase(UUID.randomUUID(), PurchaseDraft(
            card.id, expense, "Cancelada", Money(1000), 1, today,
        )) as CreditResult.Saved).value
        assertTrue(credit.cancelPurchase(cancelledPurchase.id) is CreditResult.Saved)
        suspend fun subscription(source: SubscriptionSource, title: String) = (subscriptions.create(
            SubscriptionDetails(title, Money(3000), expense, source), SubscriptionFrequency.YEARLY, today, BillingDay(19),
        ) as SubscriptionResult.Saved).value
        val accountSub = subscription(SubscriptionSource.Account(main), "Em conta")
        assertTrue(subscriptions.paySubscriptionOccurrence(accountSub.id, today, today) is SubscriptionResult.Saved)
        assertTrue(subscriptions.pause(accountSub.id) is SubscriptionResult.Saved)
        val cardSub = subscription(SubscriptionSource.Card(card.id), "No cartão")
        assertTrue(subscriptions.paySubscriptionOccurrence(cardSub.id, today, today) is SubscriptionResult.Saved)
        val skipped = subscription(SubscriptionSource.Account(main), "Pulada")
        assertTrue(subscriptions.skipSubscriptionOccurrence(skipped.id, today) is SubscriptionResult.Saved)
        assertTrue(subscriptions.cancel(skipped.id) is SubscriptionResult.Saved)

        suspend fun goal(title: String, account: UUID, target: Long) =
            (goals.createGoal(GoalDetails(title, Money(target), account, today.plusYears(1))) as GoalResult.Saved).value
        val activeGoal = goal("Viagem", main, 10_000)
        assertTrue(goals.allocateToGoal(activeGoal.id, Money(5000)) is GoalResult.Saved)
        val completed = goal("Concluída", main, 1000)
        assertTrue(goals.allocateToGoal(completed.id, Money(1000)) is GoalResult.Saved)
        assertTrue(goals.completeGoal(completed.id) is GoalResult.Saved)
        val archived = goal("Arquivada", excluded, 1000)
        assertTrue(goals.allocateToGoal(archived.id, Money(1000)) is GoalResult.Saved)
        assertTrue(goals.releaseFromGoal(archived.id, Money(1000)) is GoalResult.Saved)
        assertTrue(goals.archiveGoal(archived.id) is GoalResult.Saved)

        if (volume) {
            val large = credit.createPurchase(UUID.randomUUID(), PurchaseDraft(card.id, expense, "20 anos", Money(240_000), 240, today))
            assertTrue(large is CreditResult.Saved)
            db.withTransaction {
                for (index in 0 until 3000) {
                    db.transactionDao().insert(TransactionEntity(
                        UUID.randomUUID().toString(), main.toString(), expense.toString(), "EXPENSE", 1,
                        today.minusDays(index.toLong() % 1000).toEpochDay(), "Histórico", clock.instant().toString(),
                        clock.instant().toString(), null,
                    ))
                }
            }
        }
        assertTrue(accounts.archive(excluded))
        assertTrue(categories.archive(expense))
        assertTrue(categories.archive(income))
        assertTrue(credit.archiveCard(card.id))
        withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL("INSERT INTO local_schema_marker (id) VALUES (42)") }
    }

    private suspend fun exported(): PreparedFluxaBackup {
        val output = ByteArrayOutputStream()
        success(service().export(password(), output))
        return success(service().validate(password(), ByteArrayInputStream(output.toByteArray())))
    }

    private fun successTransaction(result: TransactionResult): FinancialTransaction =
        (result as TransactionResult.Saved).transaction

    private fun <T> success(result: BackupResult<T>): T {
        assertTrue(result.toString(), result is BackupResult.Success)
        return (result as BackupResult.Success).value
    }

    private fun mutate(
        bytes: ByteArray,
        checksum: Boolean = true,
        change: (com.google.gson.JsonObject, com.google.gson.JsonObject) -> Unit,
    ): ByteArray {
        val files = linkedMapOf<String, ByteArray>()
        val payload = decrypt(bytes)
        ZipInputStream(ByteArrayInputStream(payload)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                files[entry.name] = zip.readBytes()
            }
        }
        payload.fill(0)
        val manifest = JsonParser.parseString(files.getValue("manifest.json").toString(Charsets.UTF_8)).asJsonObject
        val data = JsonParser.parseString(files.getValue("data.json").toString(Charsets.UTF_8)).asJsonObject
        change(manifest, data)
        files["data.json"] = data.toString().toByteArray()
        if (checksum) manifest.addProperty("dataSha256", BackupCodec.sha256(files.getValue("data.json")))
        files["manifest.json"] = manifest.toString().toByteArray()
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for ((name, content) in files) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return encrypt(output.toByteArray())
    }

    private fun password() = "frase de teste portátil".toCharArray()

    private fun decrypt(bytes: ByteArray): ByteArray {
        val secret = password()
        return try {
            BackupCrypto.decrypt(ByteArrayInputStream(bytes), secret)
        } finally {
            secret.fill('\u0000')
        }
    }

    private fun encrypt(payload: ByteArray): ByteArray {
        val secret = password()
        val output = ByteArrayOutputStream()
        try {
            BackupCrypto.encrypt(payload, secret, output)
        } finally {
            secret.fill('\u0000')
            payload.fill(0)
        }
        return output.toByteArray()
    }

    private fun decodeEncrypted(bytes: ByteArray): BackupDocumentV1 {
        val payload = decrypt(bytes)
        return try { BackupCodec.decode(payload) } finally { payload.fill(0) }
    }
}
