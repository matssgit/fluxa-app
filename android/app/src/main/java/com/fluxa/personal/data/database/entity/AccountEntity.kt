package com.fluxa.personal.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
internal data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val openingBalanceCents: Long,
    val openingDate: String,
    val includeInAvailable: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String?,
)
