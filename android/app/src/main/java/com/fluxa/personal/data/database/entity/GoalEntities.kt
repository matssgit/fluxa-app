package com.fluxa.personal.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goals",
    foreignKeys = [ForeignKey(
        entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index("accountId")],
)
internal data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountCents: Long,
    val targetDate: Long?,
    val accountId: String,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?,
    val archivedAt: String?,
)

@Entity(
    tableName = "goal_movements",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class, parentColumns = ["id"], childColumns = ["goalId"],
        onDelete = ForeignKey.RESTRICT,
    )],
    indices = [Index(value = ["goalId", "occurredOn", "createdAt", "id"])],
)
internal data class GoalMovementEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val type: String,
    val amountCents: Long,
    val occurredOn: Long,
    val createdAt: String,
)
