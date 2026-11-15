package com.fluxa.personal.data.database.mapper

import com.fluxa.personal.data.database.entity.CategoryEntity
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection
import java.time.Instant
import java.util.UUID

internal fun CategoryEntity.toDomain(): Category = Category(
    id = UUID.fromString(id),
    details = CategoryDetails(
        name = name,
        kind = TransactionDirection.valueOf(kind),
        icon = icon,
        color = color,
    ),
    archivedAt = archivedAt?.let(Instant::parse),
)

internal fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id.toString(),
    name = details.name.trim(),
    kind = details.kind.name,
    icon = details.icon,
    color = details.color,
    archivedAt = archivedAt?.toString(),
)
