package com.fluxa.personal.domain.model

import java.time.Instant
import java.util.UUID

internal data class CategoryDetails(
    val name: String,
    val kind: TransactionDirection,
    val icon: String? = null,
    val color: String? = null,
) {
    init {
        require(name.isNotBlank()) { "Category name must not be blank." }
    }
}

internal data class Category(
    val id: UUID,
    val details: CategoryDetails,
    val archivedAt: Instant?,
)
