package com.fluxa.personal.ui.feature

import com.fluxa.personal.data.database.dao.AccountDao
import com.fluxa.personal.data.database.dao.CategoryDao
import com.fluxa.personal.data.database.entity.AccountEntity
import com.fluxa.personal.data.database.entity.CategoryEntity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * DAOs em memória que reproduzem as consultas do Room (filtro de arquivados, ordenação por nome sem
 * diferenciar maiúsculas e depois por id, transições condicionais). Permitem testar os ViewModels com os
 * repositórios reais do Codex, sem duplicar regra nenhuma.
 */
internal class FakeAccountDao : AccountDao {
    val rows = MutableStateFlow<List<AccountEntity>>(emptyList())
    var failWrites = false
    var failReads = false
    var inserts = 0

    override suspend fun insert(account: AccountEntity) {
        if (failWrites) throw IOException("disk full")
        require(rows.value.none { it.id == account.id })
        inserts++
        rows.update { it + account }
    }

    override fun observe(id: String): Flow<AccountEntity?> =
        if (failReads) flow { throw IOException("read") } else rows.map { list -> list.firstOrNull { it.id == id } }

    override fun observeAll(includeArchived: Boolean): Flow<List<AccountEntity>> =
        if (failReads) flow { throw IOException("read") } else rows.map { list ->
            list.filter { includeArchived || it.archivedAt == null }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it: AccountEntity -> it.name }.thenBy { it.id })
        }

    override suspend fun update(
        id: String,
        name: String,
        type: String,
        openingBalanceCents: Long,
        openingDate: String,
        includeInAvailable: Boolean,
        updatedAt: String,
    ): Int {
        if (failWrites) throw IOException("disk full")
        var changed = 0
        rows.update { list ->
            list.map {
                if (it.id == id) {
                    changed++
                    it.copy(
                        name = name, type = type, openingBalanceCents = openingBalanceCents, openingDate = openingDate,
                        includeInAvailable = includeInAvailable, updatedAt = updatedAt,
                    )
                } else it
            }
        }
        return changed
    }

    override suspend fun archive(id: String, archivedAt: String): Int {
        if (failWrites) throw IOException("disk full")
        var changed = 0
        rows.update { list ->
            list.map {
                if (it.id == id && it.archivedAt == null) {
                    changed++
                    it.copy(archivedAt = archivedAt, updatedAt = archivedAt)
                } else it
            }
        }
        return changed
    }
}

internal class FakeCategoryDao : CategoryDao {
    val rows = MutableStateFlow<List<CategoryEntity>>(emptyList())
    var failWrites = false
    var inserts = 0
    var reactivations = 0

    override suspend fun insert(category: CategoryEntity) {
        if (failWrites) throw IOException("disk full")
        require(rows.value.none { it.id == category.id })
        inserts++
        rows.update { it + category }
    }

    override fun observe(id: String): Flow<CategoryEntity?> = rows.map { list -> list.firstOrNull { it.id == id } }

    override fun observeAll(includeArchived: Boolean, kind: String?): Flow<List<CategoryEntity>> = rows.map { list ->
        list.filter { (includeArchived || it.archivedAt == null) && (kind == null || it.kind == kind) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it: CategoryEntity -> it.name }.thenBy { it.id })
    }

    override suspend fun update(id: String, name: String, kind: String, icon: String?, color: String?): Int {
        if (failWrites) throw IOException("disk full")
        var changed = 0
        rows.update { list ->
            list.map {
                if (it.id == id) {
                    changed++
                    it.copy(name = name, kind = kind, icon = icon, color = color)
                } else it
            }
        }
        return changed
    }

    override suspend fun archive(id: String, archivedAt: String): Int = transition(id) { (it.archivedAt == null) to it.copy(archivedAt = archivedAt) }

    override suspend fun reactivate(id: String): Int {
        val changed = transition(id) { (it.archivedAt != null) to it.copy(archivedAt = null) }
        reactivations += changed
        return changed
    }

    private fun transition(id: String, change: (CategoryEntity) -> Pair<Boolean, CategoryEntity>): Int {
        if (failWrites) throw IOException("disk full")
        var changed = 0
        rows.update { list ->
            list.map {
                if (it.id != id) return@map it
                val (allowed, next) = change(it)
                if (allowed) {
                    changed++
                    next
                } else it
            }
        }
        return changed
    }
}
