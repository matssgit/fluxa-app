package com.fluxa.personal.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS accounts (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                openingBalanceCents INTEGER NOT NULL,
                openingDate TEXT NOT NULL,
                includeInAvailable INTEGER NOT NULL,
                createdAt TEXT NOT NULL,
                updatedAt TEXT NOT NULL,
                archivedAt TEXT,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS categories (
                id TEXT NOT NULL,
                name TEXT NOT NULL,
                kind TEXT NOT NULL,
                icon TEXT,
                color TEXT,
                archivedAt TEXT,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
    }
}
