package com.fluxa.personal.data.repository

import com.fluxa.personal.data.database.dao.AccountDao
import com.fluxa.personal.data.database.mapper.toDomain
import com.fluxa.personal.data.database.mapper.toEntity
import com.fluxa.personal.domain.model.AccountDetails
import com.fluxa.personal.domain.model.FinancialAccount
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal class AccountRepository(
    private val dao: AccountDao,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun observe(id: UUID): Flow<FinancialAccount?> =
        dao.observe(id.toString()).map { it?.toDomain() }.distinctUntilChanged()

    fun observeAll(includeArchived: Boolean = false): Flow<List<FinancialAccount>> =
        dao.observeAll(includeArchived).map { accounts -> accounts.map { it.toDomain() } }.distinctUntilChanged()

    suspend fun create(details: AccountDetails): UUID {
        val id = UUID.randomUUID()
        val now = clock.instant()
        dao.insert(FinancialAccount(id, details, now, now, archivedAt = null).toEntity())
        return id
    }

    suspend fun update(id: UUID, details: AccountDetails): Boolean = dao.update(
        id = id.toString(),
        name = details.name.trim(),
        type = details.type.name,
        openingBalanceCents = details.openingBalance.cents,
        openingDate = details.openingDate.toString(),
        includeInAvailable = details.includeInAvailable,
        updatedAt = clock.instant().toString(),
    ) == 1

    suspend fun archive(id: UUID): Boolean =
        dao.archive(id.toString(), clock.instant().toString()) == 1
}
