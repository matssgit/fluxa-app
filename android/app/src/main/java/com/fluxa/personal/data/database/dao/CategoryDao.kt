package com.fluxa.personal.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fluxa.personal.data.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    fun observe(id: String): Flow<CategoryEntity?>

    @Query(
        """
        SELECT * FROM categories
        WHERE (:includeArchived OR archivedAt IS NULL) AND (:kind IS NULL OR kind = :kind)
        ORDER BY name COLLATE NOCASE, id
        """,
    )
    fun observeAll(includeArchived: Boolean, kind: String?): Flow<List<CategoryEntity>>

    @Query(
        """
        UPDATE categories SET name = :name, kind = :kind, icon = :icon, color = :color
        WHERE id = :id
        """,
    )
    suspend fun update(id: String, name: String, kind: String, icon: String?, color: String?): Int

    @Query("UPDATE categories SET archivedAt = :archivedAt WHERE id = :id AND archivedAt IS NULL")
    suspend fun archive(id: String, archivedAt: String): Int

    @Query("UPDATE categories SET archivedAt = NULL WHERE id = :id AND archivedAt IS NOT NULL")
    suspend fun reactivate(id: String): Int
}
