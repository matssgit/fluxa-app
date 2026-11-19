package com.fluxa.personal.backup

import androidx.room.withTransaction
import com.fluxa.personal.data.database.FluxaDatabase
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import java.time.LocalDate

/** Explicit v1 wire mapping, including conversion of Room epoch-day columns to ISO dates. */
internal class BackupPersistence(private val database: FluxaDatabase) {
    suspend fun snapshot(): Map<String, List<BackupRowV1>> = database.withTransaction {
        val db = database.openHelper.writableDatabase
        var totalRecords = 0
        BackupFormatV1.tables.associate { table ->
            val columns = table.fields.joinToString { it.name }
            val order = table.keys.joinToString()
            val records = db.query("SELECT $columns FROM ${table.name} ORDER BY $order").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        if (++totalRecords > 100_000) throw BackupException(BackupError.TOO_LARGE)
                        val row = JsonObject()
                        for ((index, field) in table.fields.withIndex()) {
                            if (cursor.isNull(index)) {
                                row.add(field.name, JsonNull.INSTANCE)
                            } else when (field.type) {
                                BackupFieldType.DATE -> row.addProperty(field.name,
                                    if (field.storedAsDateText) cursor.getString(index) else LocalDate.ofEpochDay(cursor.getLong(index)).toString())
                                BackupFieldType.BOOLEAN -> {
                                    val stored = cursor.getLong(index)
                                    if (stored !in 0L..1L) throw BackupException(BackupError.INVALID)
                                    row.addProperty(field.name, stored == 1L)
                                }
                                BackupFieldType.LONG -> row.addProperty(field.name, cursor.getLong(index))
                                else -> row.addProperty(field.name, cursor.getString(index))
                            }
                        }
                        add(BackupRowV1(row))
                    }
                }
            }
            table.name to records
        }
    }

    suspend fun replace(
        incoming: BackupDataV1,
        expectedCurrent: Map<String, List<BackupRowV1>>,
    ) = database.withTransaction {
        if (snapshot() != expectedCurrent) throw BackupException(BackupError.RESTORE_CONFLICT)
        val db = database.openHelper.writableDatabase
        for (table in BackupFormatV1.tables.asReversed()) db.execSQL("DELETE FROM ${table.name}")
        for (table in BackupFormatV1.tables) {
            val columns = table.fields.joinToString { it.name }
            val placeholders = List(table.fields.size) { "?" }.joinToString()
            val sql = "INSERT INTO ${table.name} ($columns) VALUES ($placeholders)"
            val statement = db.compileStatement(sql)
            statement.use {
                for (record in incoming.tables.getValue(table.name)) {
                    it.clearBindings()
                    for ((index, field) in table.fields.withIndex()) {
                        val value = record.fields.get(field.name)
                        val at = index + 1
                        if (value.isJsonNull) it.bindNull(at)
                        else when (field.type) {
                            BackupFieldType.DATE -> if (field.storedAsDateText) it.bindString(at, value.asString)
                                else it.bindLong(at, LocalDate.parse(value.asString).toEpochDay())
                            BackupFieldType.BOOLEAN -> it.bindLong(at, if (value.asBoolean) 1L else 0L)
                            BackupFieldType.LONG -> it.bindLong(at, record.fields.long(field.name))
                            else -> it.bindString(at, value.asString)
                        }
                    }
                    it.executeInsert()
                }
            }
        }
        db.query("PRAGMA foreign_key_check").use {
            if (it.moveToFirst()) throw BackupException(BackupError.RELATION_INVALID)
        }
        // Raw SQL participates in Room's transaction and invalidation triggers.
    }
}
