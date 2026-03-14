package com.easylogger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.CategoryWithLastLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, createdAt ASC")
    fun getAll(): Flow<List<Category>>

    @Query(
        """
        SELECT c.id, c.name, c.sortOrder, c.createdAt,
               (SELECT MAX(le.timestamp) FROM log_entries le WHERE le.categoryId = c.id) AS lastLogTimestamp
        FROM categories c
        ORDER BY c.sortOrder ASC, c.createdAt ASC
        """
    )
    fun getAllWithLastLog(): Flow<List<CategoryWithLastLog>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllList(): List<Category>

    @Delete
    suspend fun delete(category: Category)
}
