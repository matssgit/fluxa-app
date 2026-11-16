package com.fluxa.personal.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = InstallmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["installmentId"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["accountId", "occurredOn"]),
        Index(value = ["categoryId", "occurredOn"]),
        Index(value = ["occurredOn"]),
        Index(value = ["installmentId"], unique = true),
    ],
)
internal data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val categoryId: String,
    val direction: String,
    val amountCents: Long,
    val occurredOn: Long,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
    val cancelledAt: String?,
    val installmentId: String? = null,
)
