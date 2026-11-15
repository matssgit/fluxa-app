package com.fluxa.personal.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
internal data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val icon: String?,
    val color: String?,
    val archivedAt: String?,
)
