package com.fluxa.personal.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE goals (
                id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, targetAmountCents INTEGER NOT NULL,
                targetDate INTEGER, accountId TEXT NOT NULL, createdAt TEXT NOT NULL, updatedAt TEXT NOT NULL,
                completedAt TEXT, archivedAt TEXT,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX index_goals_accountId ON goals(accountId)")
        db.execSQL("""
            CREATE TABLE goal_movements (
                id TEXT NOT NULL PRIMARY KEY, goalId TEXT NOT NULL, type TEXT NOT NULL,
                amountCents INTEGER NOT NULL, occurredOn INTEGER NOT NULL, createdAt TEXT NOT NULL,
                FOREIGN KEY(goalId) REFERENCES goals(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX index_goal_movements_goalId_occurredOn_createdAt_id ON goal_movements(goalId, occurredOn, createdAt, id)")
        installGoalGuards(db)
    }
}

internal fun installGoalGuards(db: SupportSQLiteDatabase) {
    for (event in listOf("INSERT", "UPDATE")) {
        db.execSQL("""
            CREATE TRIGGER goals_validate_${event.lowercase()} BEFORE $event ON goals
            WHEN typeof(NEW.targetAmountCents) <> 'integer' OR NEW.targetAmountCents <= 0 OR trim(NEW.name) = ''
            BEGIN SELECT RAISE(ABORT, 'INVALID_GOAL_DETAILS'); END
        """.trimIndent())
    }
    db.execSQL("""
        CREATE TRIGGER goal_movements_validate_insert BEFORE INSERT ON goal_movements
        WHEN NEW.type NOT IN ('ALLOCATE', 'RELEASE') OR typeof(NEW.amountCents) <> 'integer' OR NEW.amountCents <= 0
        BEGIN SELECT RAISE(ABORT, 'INVALID_GOAL_MOVEMENT'); END
    """.trimIndent())
    db.execSQL("""
        CREATE TRIGGER goals_preserve_account BEFORE UPDATE OF accountId ON goals
        WHEN OLD.accountId <> NEW.accountId AND EXISTS (SELECT 1 FROM goal_movements WHERE goalId = OLD.id)
        BEGIN SELECT RAISE(ABORT, 'GOAL_ACCOUNT_HAS_MOVEMENTS'); END
    """.trimIndent())
}
