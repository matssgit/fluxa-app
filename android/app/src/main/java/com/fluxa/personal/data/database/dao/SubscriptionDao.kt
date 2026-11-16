package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.fluxa.personal.data.database.entity.SubscriptionEntity
import com.fluxa.personal.data.database.entity.SubscriptionResolutionEntity
import com.fluxa.personal.data.database.entity.SubscriptionTermsEntity
import kotlinx.coroutines.flow.Flow

internal data class SubscriptionAggregate(
    @Embedded val subscription: SubscriptionEntity,
    @Relation(parentColumn = "id", entityColumn = "subscriptionId")
    val terms: List<SubscriptionTermsEntity>,
    @Relation(parentColumn = "id", entityColumn = "subscriptionId")
    val resolutions: List<SubscriptionResolutionEntity>,
)

@Dao
internal interface SubscriptionDao {
    @Insert suspend fun insert(subscription: SubscriptionEntity)
    @Update suspend fun update(subscription: SubscriptionEntity)
    @Insert suspend fun insertTerms(terms: SubscriptionTermsEntity)
    @Insert suspend fun resolve(resolution: SubscriptionResolutionEntity)

    @Query("DELETE FROM subscription_terms WHERE subscriptionId = :id AND effectiveOn >= :from")
    suspend fun deleteTermsFrom(id: String, from: Long)

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun find(id: String): SubscriptionAggregate?

    @Transaction
    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun observe(id: String): Flow<SubscriptionAggregate?>

    @Transaction
    @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<SubscriptionAggregate>>
}
