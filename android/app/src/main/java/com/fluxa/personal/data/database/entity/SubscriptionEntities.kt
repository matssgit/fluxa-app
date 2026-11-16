package com.fluxa.personal.data.database.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

internal data class SubscriptionColumns(
    val name: String,
    val amountCents: Long,
    val categoryId: String,
    val accountId: String?,
    val cardId: String?,
)

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["accountId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CreditCardEntity::class, parentColumns = ["id"], childColumns = ["cardId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("categoryId"), Index("accountId"), Index("cardId")],
)
internal data class SubscriptionEntity(
    @PrimaryKey val id: String,
    @Embedded val details: SubscriptionColumns,
    val frequency: String,
    val anchorDate: Long,
    val billingDay: Int,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val pausedAt: String?,
    val cancelledAt: String?,
)

@Entity(
    tableName = "subscription_terms",
    primaryKeys = ["subscriptionId", "effectiveOn"],
    foreignKeys = [
        ForeignKey(entity = SubscriptionEntity::class, parentColumns = ["id"], childColumns = ["subscriptionId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("categoryId")],
)
internal data class SubscriptionTermsEntity(
    val subscriptionId: String,
    val effectiveOn: Long,
    @Embedded val details: SubscriptionColumns,
    val enabled: Boolean,
)

@Entity(
    tableName = "subscription_resolutions",
    primaryKeys = ["subscriptionId", "occurrenceDate"],
    foreignKeys = [
        ForeignKey(entity = SubscriptionEntity::class, parentColumns = ["id"], childColumns = ["subscriptionId"], onDelete = ForeignKey.RESTRICT),
    ],
)
internal data class SubscriptionResolutionEntity(
    val subscriptionId: String,
    val occurrenceDate: Long,
    @Embedded val details: SubscriptionColumns,
    val status: String,
    val resultId: String?,
)
