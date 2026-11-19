package com.fluxa.personal.backup

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.nio.ByteOrder
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal object BackupCodec {
    const val MAX_DATA_BYTES = 32 * 1024 * 1024
    private const val MAX_MANIFEST_BYTES = 64 * 1024

    fun encode(data: BackupDataV1, createdAt: Instant, appVersion: String): ByteArray {
        BackupValidator.validateData(data)
        val bytes = dataJson(data).toString().toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_DATA_BYTES) throw BackupException(BackupError.TOO_LARGE)
        val manifest = BackupManifestV1(
            1, createdAt, appVersion, 7, "BRL", "NONE", sha256(bytes),
            data.tables.mapValues { it.value.size },
        )
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entry(zip, "manifest.json", manifestJson(manifest).toString().toByteArray(Charsets.UTF_8))
            entry(zip, "data.json", bytes)
        }
        return output.toByteArray()
    }

    fun decode(input: InputStream): BackupDocumentV1 = try {
        decodeChecked(input)
    } catch (error: BackupException) {
        throw error
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (_: java.io.IOException) {
        throw BackupException(BackupError.CORRUPTED)
    } catch (_: RuntimeException) {
        throw BackupException(BackupError.INVALID)
    }

    private fun decodeChecked(input: InputStream): BackupDocumentV1 {
        val archive = readLimited(input, MAX_DATA_BYTES + MAX_MANIFEST_BYTES)
        validateZipContainer(archive)
        val files = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name !in setOf("manifest.json", "data.json") || entry.name in files || entry.isDirectory) {
                    throw BackupException(BackupError.INVALID)
                }
                files[entry.name] = readLimited(zip, if (entry.name == "manifest.json") MAX_MANIFEST_BYTES else MAX_DATA_BYTES)
                zip.closeEntry()
            }
        }
        if (files.keys != setOf("manifest.json", "data.json")) throw BackupException(BackupError.INVALID)
        val manifest = parseManifest(parseJson(files.getValue("manifest.json")).asObject())
        val bytes = files.getValue("data.json")
        if (sha256(bytes) != manifest.dataSha256) throw BackupException(BackupError.INTEGRITY_FAILED)
        val root = parseJson(bytes).asObject()
        requireKeys(root, setOf("tables", "settings"))
        val tablesJson = root.get("tables").asObject()
        val tables = tablesJson.entrySet().associate { (name, value) ->
            if (!value.isJsonArray) throw BackupException(BackupError.INVALID)
            name to value.asJsonArray.map { BackupRowV1(it.asObject()) }
        }
        val settings = root.get("settings").asObject()
        requireKeys(settings, setOf("theme", "hideAmounts"))
        val data = BackupDataV1(tables, PortableSettingsV1(settings.text("theme"), settings.bool("hideAmounts")))
        BackupValidator.validateData(data)
        if (manifest.recordCounts != tables.mapValues { it.value.size }) throw BackupException(BackupError.INTEGRITY_FAILED)
        return BackupDocumentV1(manifest, data)
    }

    fun dataJson(data: BackupDataV1): JsonObject = JsonObject().apply {
        add("tables", JsonObject().apply {
            for (table in BackupFormatV1.tables) {
                add(table.name, JsonArray().apply { data.tables.getValue(table.name).forEach { add(it.fields) } })
            }
        })
        add("settings", JsonObject().apply {
            addProperty("theme", data.settings.theme)
            addProperty("hideAmounts", data.settings.hideAmounts)
        })
    }

    private fun manifestJson(m: BackupManifestV1) = JsonObject().apply {
        addProperty("formatVersion", m.formatVersion)
        addProperty("createdAt", m.createdAt.toString())
        addProperty("appVersion", m.appVersion)
        addProperty("databaseSchemaVersion", m.databaseSchemaVersion)
        addProperty("currency", m.currency)
        addProperty("encryption", m.encryption)
        addProperty("dataSha256", m.dataSha256)
        add("recordCounts", JsonObject().apply { m.recordCounts.forEach { (name, count) -> addProperty(name, count) } })
    }

    private fun parseManifest(root: JsonObject): BackupManifestV1 {
        if (root.long("formatVersion") != 1L) throw BackupException(BackupError.VERSION_UNSUPPORTED)
        requireKeys(root, setOf("formatVersion", "createdAt", "appVersion", "databaseSchemaVersion", "currency", "encryption", "dataSha256", "recordCounts"))
        if (root.long("databaseSchemaVersion") != 7L || root.text("encryption") != "NONE") {
            throw BackupException(BackupError.VERSION_UNSUPPORTED)
        }
        if (root.text("currency") != "BRL" || root.text("appVersion").isBlank()) throw BackupException(BackupError.INVALID)
        val stamp = Instant.parse(root.text("createdAt"))
        if (stamp.toString() != root.text("createdAt")) throw BackupException(BackupError.INVALID)
        val hash = root.text("dataSha256")
        if (!hash.matches(Regex("[0-9a-f]{64}"))) throw BackupException(BackupError.INVALID)
        val counts = root.get("recordCounts").asObject()
        requireKeys(counts, BackupFormatV1.tables.map { it.name }.toSet())
        return BackupManifestV1(1, stamp, root.text("appVersion"), 7, "BRL", "NONE", hash,
            counts.keySet().associateWith {
                val n = counts.long(it)
                if (n !in 0..100_000) throw BackupException(BackupError.INVALID)
                n.toInt()
            })
    }

    private fun parseJson(bytes: ByteArray): JsonElement {
        val utf8 = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
        JsonReader(StringReader(utf8)).use { reader ->
            reader.isLenient = false
            val parsed = readJson(reader, 0)
            if (reader.peek() != JsonToken.END_DOCUMENT) throw BackupException(BackupError.CORRUPTED)
            return parsed
        }
    }

    private fun readJson(reader: JsonReader, depth: Int): JsonElement {
        if (depth > 32) throw BackupException(BackupError.INVALID)
        return when (reader.peek()) {
            JsonToken.BEGIN_OBJECT -> JsonObject().apply {
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (has(name)) throw BackupException(BackupError.INVALID)
                    add(name, readJson(reader, depth + 1))
                }
                reader.endObject()
            }
            JsonToken.BEGIN_ARRAY -> JsonArray().apply {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (size() >= 100_000) throw BackupException(BackupError.TOO_LARGE)
                    add(readJson(reader, depth + 1))
                }
                reader.endArray()
            }
            JsonToken.STRING -> JsonPrimitive(reader.nextString())
            JsonToken.NUMBER -> JsonPrimitive(BigDecimal(reader.nextString()))
            JsonToken.BOOLEAN -> JsonPrimitive(reader.nextBoolean())
            JsonToken.NULL -> { reader.nextNull(); JsonNull.INSTANCE }
            else -> throw BackupException(BackupError.CORRUPTED)
        }
    }

    private fun entry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun readLimited(input: InputStream, limit: Int): ByteArray {
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) return result.toByteArray()
            if (result.size().toLong() + count > limit) throw BackupException(BackupError.TOO_LARGE)
            result.write(buffer, 0, count)
        }
    }

    private fun validateZipContainer(bytes: ByteArray) {
        if (bytes.size < 22) throw BackupException(BackupError.CORRUPTED)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun short(at: Int): Int = buffer.getShort(at).toInt() and 65535
        var end = bytes.size - 22
        val earliest = maxOf(0, bytes.size - 22 - 65535)
        while (end >= earliest && buffer.getInt(end) != 0x06054b50) end--
        if (end < earliest || end + 22 + short(end + 20) != bytes.size ||
            short(end + 4) != 0 || short(end + 6) != 0 || short(end + 8) != 2 || short(end + 10) != 2
        ) throw BackupException(BackupError.CORRUPTED)
        val centralSize = buffer.getInt(end + 12)
        val start = buffer.getInt(end + 16)
        if (start < 0 || centralSize < 0 || start.toLong() + centralSize != end.toLong() ||
            buffer.getInt(0) != 0x04034b50
        ) throw BackupException(BackupError.CORRUPTED)
        var at = start
        repeat(2) {
            if (at.toLong() + 46 > end || buffer.getInt(at) != 0x02014b50) throw BackupException(BackupError.CORRUPTED)
            val size = 46 + short(at + 28) + short(at + 30) + short(at + 32)
            if (at.toLong() + size > end || short(at + 8) and 1 != 0) throw BackupException(BackupError.CORRUPTED)
            at += size
        }
        if (at != end) throw BackupException(BackupError.CORRUPTED)
    }

    internal fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
}

internal fun JsonElement?.asObject(): JsonObject =
    if (this != null && isJsonObject) asJsonObject else throw BackupException(BackupError.INVALID)

internal fun requireKeys(row: JsonObject, keys: Set<String>) {
    if (row.keySet() != keys) throw BackupException(BackupError.INVALID)
}

internal fun JsonObject.text(name: String): String {
    val value = get(name)
    if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isString) throw BackupException(BackupError.INVALID)
    return value.asString
}

internal fun JsonObject.long(name: String): Long {
    val value = get(name)
    if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isNumber) throw BackupException(BackupError.INVALID)
    val raw = value.asString
    if (!raw.matches(Regex("-?(0|[1-9][0-9]*)"))) throw BackupException(BackupError.INVALID)
    return raw.toLongOrNull() ?: throw BackupException(BackupError.INVALID)
}

internal fun JsonObject.bool(name: String): Boolean {
    val value = get(name)
    if (value == null || !value.isJsonPrimitive || !value.asJsonPrimitive.isBoolean) throw BackupException(BackupError.INVALID)
    return value.asBoolean
}
