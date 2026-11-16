package com.fluxa.personal.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
internal data class CreditCardEntity(
    @PrimaryKey val id: String,
    val name: String,
    val totalLimitCents: Long,
    val closingDay: Int,
    val dueDay: Int,
    val color: String?,
    val archivedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

@Entity(
    tableName = "credit_purchases",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"], childColumns = ["cardId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"], childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["cardId", "purchaseDate"]), Index(value = ["categoryId"])],
)
internal data class PurchaseEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val categoryId: String,
    val title: String,
    val merchant: String?,
    val notes: String?,
    val totalCents: Long,
    val installmentCount: Int,
    val purchaseDate: Long,
    val firstDueDate: Long,
    val createdAt: String,
    val updatedAt: String,
    val cancelledAt: String?,
)

@Entity(
    tableName = "credit_installments",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"], childColumns = ["purchaseId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["purchaseId", "number"], unique = true)],
)
internal data class InstallmentEntity(
    @PrimaryKey val id: String,
    val purchaseId: String,
    val number: Int,
    val amountCents: Long,
    val dueDate: Long,
    val status: String,
)
