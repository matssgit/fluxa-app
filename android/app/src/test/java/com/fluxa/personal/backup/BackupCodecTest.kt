package com.fluxa.personal.backup

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Test

class BackupCodecTest {
    private val stamp = Instant.parse("2024-02-29T10:00:00Z")
    private val id = UUID(0, 1).toString()

    @Test
    fun roundTripsLongCentsUuidIsoDatesManifestAndPortableSettings() {
        for (cents in listOf(Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE)) {
            val data = data(account(cents))
            val decoded = BackupCodec.decode(input(BackupCodec.encode(data, stamp, "0.1.0")))
            assertTrue("Payload must round-trip exactly", data == decoded.data)
            assertEquals(cents, decoded.data.tables.getValue("accounts").single().fields.long("openingBalanceCents"))
            assertEquals(id, decoded.data.tables.getValue("accounts").single().fields.text("id"))
            assertEquals("2024-02-29", decoded.data.tables.getValue("accounts").single().fields.text("openingDate"))
            assertEquals(stamp, decoded.manifest.createdAt)
            assertEquals(1, decoded.manifest.formatVersion)
            assertEquals(7, decoded.manifest.databaseSchemaVersion)
            assertEquals("BRL", decoded.manifest.currency)
            assertEquals(PortableSettingsV1("DARK", true), decoded.data.settings)
        }
    }

    @Test
    fun emptyCompleteDatabaseIsPortable() {
        val empty = BackupDataV1(BackupFormatV1.tables.associate { it.name to emptyList() }, PortableSettingsV1("SYSTEM", false))
        assertTrue("Empty payload must round-trip", empty == BackupCodec.decode(input(BackupCodec.encode(empty, stamp, "test"))).data)
    }

    @Test
    fun rejectsUnknownFormatAndEncryptionBeforeParsingPayload() {
        assertError(BackupError.VERSION_UNSUPPORTED, tamper(manifest = { it.addProperty("formatVersion", 999) }))
        assertError(BackupError.VERSION_UNSUPPORTED, tamper(manifest = { it.addProperty("encryption", "FUTURE") }))
    }

    @Test
    fun detectsChecksumAndRecordCountChanges() {
        assertError(BackupError.INTEGRITY_FAILED, tamper(manifest = { it.addProperty("dataSha256", "0".repeat(64)) }))
        assertError(BackupError.INTEGRITY_FAILED, tamper(manifest = { it.getAsJsonObject("recordCounts").addProperty("accounts", 99) }))
    }

    @Test
    fun validatesUuidEnumsDatesAndExactLongIntegersEvenWithARecomputedChecksum() {
        val mutations: List<(JsonObject) -> Unit> = listOf(
            { it.addProperty("id", "1-1-1-1-1") },
            { it.addProperty("type", "CREDIT_CARD") },
            { it.addProperty("openingDate", "2023-02-29") },
            { it.addProperty("createdAt", "2024-02-29T10:00:00+03:00") },
            { it.addProperty("openingBalanceCents", java.math.BigInteger("9223372036854775808")) },
            { it.addProperty("openingBalanceCents", java.math.BigDecimal("1.5")) },
            { it.addProperty("openingBalanceCents", "1000") },
            { it.add("name", JsonNull.INSTANCE) },
        )
        mutations.forEach { mutation ->
            assertError(BackupError.INVALID, tamper(mutateData = { root ->
                mutation(root.getAsJsonObject("tables").getAsJsonArray("accounts")[0].asJsonObject)
            }))
        }
    }

    @Test
    fun rejectsDuplicateIdsAndMissingForeignKeys() {
        assertError(BackupError.INVALID, tamper(mutateData = { root ->
            val records = root.getAsJsonObject("tables").getAsJsonArray("accounts")
            records.add(records[0].deepCopy())
        }))
        val tables = data(account(1000)).tables.toMutableMap()
        val goal = JsonParser.parseString("""
            {"id":"00000000-0000-0000-0000-000000000002","name":"Meta","targetAmountCents":1000,
             "targetDate":null,"accountId":"00000000-0000-0000-0000-000000000099",
             "createdAt":"2024-02-29T10:00:00Z","updatedAt":"2024-02-29T10:00:00Z","completedAt":null,"archivedAt":null}
        """).asJsonObject
        tables["goals"] = listOf(BackupRowV1(goal))
        val error = runCatching { BackupValidator.validateData(BackupDataV1(tables, PortableSettingsV1("SYSTEM", false))) }.exceptionOrNull()
        assertEquals(BackupError.RELATION_INVALID, (error as BackupException).error)
    }

    @Test
    fun rejectsEmptyInvalidZipMissingManifestAndUnexpectedArchivePaths() {
        for (bytes in listOf(byteArrayOf(), "invalid zip".toByteArray(), zip(mapOf("data.json" to "{}".toByteArray())))) {
            assertTrue(runCatching { BackupCodec.decode(input(bytes)) }.exceptionOrNull() is BackupException)
        }
        val complete = BackupCodec.encode(data(account(1000)), stamp, "test")
        assertError(BackupError.CORRUPTED, complete.copyOf(complete.size - 22))
        assertError(BackupError.INVALID, zip(mapOf("../manifest.json" to "{}".toByteArray(), "data.json" to "{}".toByteArray())))
    }

    @Test
    fun rejectsCorruptedJsonDuplicateFieldsAndExpansionBeyondTheLimit() {
        val files = unzip(BackupCodec.encode(data(account(1000)), stamp, "test")).toMutableMap()
        val corrupted = "{".toByteArray()
        val manifest = JsonParser.parseString(files.getValue("manifest.json").toString(Charsets.UTF_8)).asJsonObject
        manifest.addProperty("dataSha256", BackupCodec.sha256(corrupted))
        files["manifest.json"] = manifest.toString().toByteArray()
        files["data.json"] = corrupted
        assertTrue(runCatching { BackupCodec.decode(input(zip(files))) }.exceptionOrNull() is BackupException)
        files["manifest.json"] = """{"formatVersion":1,"formatVersion":999}""".toByteArray()
        assertError(BackupError.INVALID, zip(files))
        assertError(BackupError.TOO_LARGE, zip(mapOf("manifest.json" to "{}".toByteArray(), "data.json" to ByteArray(BackupCodec.MAX_DATA_BYTES + 1))))
    }

    private fun data(record: BackupRowV1): BackupDataV1 = BackupDataV1(
        BackupFormatV1.tables.associate { it.name to if (it.name == "accounts") listOf(record) else emptyList() },
        PortableSettingsV1("DARK", true),
    )

    private fun account(cents: Long): BackupRowV1 = BackupRowV1(JsonObject().apply {
        addProperty("id", id)
        addProperty("name", "Conta ç")
        addProperty("type", "CHECKING")
        addProperty("openingBalanceCents", cents)
        addProperty("openingDate", "2024-02-29")
        addProperty("includeInAvailable", true)
        addProperty("createdAt", stamp.toString())
        addProperty("updatedAt", stamp.toString())
        add("archivedAt", JsonNull.INSTANCE)
    })

    private fun tamper(
        manifest: (JsonObject) -> Unit = {},
        mutateData: (JsonObject) -> Unit = {},
    ): ByteArray {
        val files = unzip(BackupCodec.encode(data(account(1000)), stamp, "test")).toMutableMap()
        val payload = JsonParser.parseString(files.getValue("data.json").toString(Charsets.UTF_8)).asJsonObject
        mutateData(payload)
        files["data.json"] = payload.toString().toByteArray()
        val header = JsonParser.parseString(files.getValue("manifest.json").toString(Charsets.UTF_8)).asJsonObject
        header.addProperty("dataSha256", BackupCodec.sha256(files.getValue("data.json")))
        manifest(header)
        files["manifest.json"] = header.toString().toByteArray()
        return zip(files)
    }

    private fun assertError(expected: BackupError, bytes: ByteArray) {
        val error = runCatching { BackupCodec.decode(input(bytes)) }.exceptionOrNull()
        assertTrue("Expected $expected, got $error", error is BackupException)
        assertEquals(expected, (error as BackupException).error)
    }

    private fun input(bytes: ByteArray) = ByteArrayInputStream(bytes)

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> = buildMap {
        ZipInputStream(input(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                put(entry.name, zip.readBytes())
            }
        }
    }

    private fun zip(files: Map<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.forEach { (name, bytes) -> zip.putNextEntry(ZipEntry(name)); zip.write(bytes); zip.closeEntry() }
        }
        return output.toByteArray()
    }
}
