package com.fluxa.personal.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.fluxa.personal.data.database.CATEGORY_KIND_IN_USE
import com.fluxa.personal.data.database.dao.CategoryDao
import com.fluxa.personal.data.database.mapper.toDomain
import com.fluxa.personal.data.database.mapper.toEntity
import com.fluxa.personal.domain.model.Category
import com.fluxa.personal.domain.model.CategoryDetails
import com.fluxa.personal.domain.model.TransactionDirection
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class CategoryKindInUse(cause: Throwable) :
    IllegalStateException("A category referenced by a transaction or purchase cannot change direction.", cause)

internal class CategoryRepository(
    private val dao: CategoryDao,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun observe(id: UUID): Flow<Category?> =
        dao.observe(id.toString()).map { it?.toDomain() }.distinctUntilChanged()

    fun observeAll(
        includeArchived: Boolean = false,
        kind: TransactionDirection? = null,
    ): Flow<List<Category>> =
        dao.observeAll(includeArchived, kind?.name)
            .map { categories -> categories.map { it.toDomain() } }
            .distinctUntilChanged()

    suspend fun create(details: CategoryDetails): UUID {
        val id = UUID.randomUUID()
        dao.insert(Category(id, details, archivedAt = null).toEntity())
        return id
    }

    suspend fun update(id: UUID, details: CategoryDetails): Boolean = try {
        dao.update(
            id = id.toString(),
            name = details.name.trim(),
            kind = details.kind.name,
            icon = details.icon,
            color = details.color,
        ) == 1
    } catch (exception: SQLiteConstraintException) {
        if (exception.message?.contains(CATEGORY_KIND_IN_USE) == true) throw CategoryKindInUse(exception)
        throw exception
    }

    suspend fun archive(id: UUID): Boolean =
        dao.archive(id.toString(), clock.instant().toString()) == 1

    suspend fun reactivate(id: UUID): Boolean = dao.reactivate(id.toString()) == 1
}
