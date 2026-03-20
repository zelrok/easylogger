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
        SELECT c.id, c.name, c.sortOrder, c.createdAt, c.folderId, c.folderSortOrder,
               (SELECT MAX(le.timestamp) FROM log_entries le WHERE le.categoryId = c.id) AS lastLogTimestamp
        FROM categories c
        ORDER BY c.sortOrder ASC, c.createdAt ASC
        """
    )
    fun getAllWithLastLog(): Flow<List<CategoryWithLastLog>>

    @Query(
        """
        SELECT c.id, c.name, c.sortOrder, c.createdAt, c.folderId, c.folderSortOrder,
               (SELECT MAX(le.timestamp) FROM log_entries le WHERE le.categoryId = c.id) AS lastLogTimestamp
        FROM categories c
        WHERE c.folderId IS NULL
        ORDER BY c.sortOrder ASC, c.createdAt ASC
        """
    )
    fun getTopLevelWithLastLog(): Flow<List<CategoryWithLastLog>>

    @Query(
        """
        SELECT c.id, c.name, c.sortOrder, c.createdAt, c.folderId, c.folderSortOrder,
               (SELECT MAX(le.timestamp) FROM log_entries le WHERE le.categoryId = c.id) AS lastLogTimestamp
        FROM categories c
        WHERE c.folderId = :folderId
        ORDER BY c.folderSortOrder ASC, c.createdAt ASC
        """
    )
    fun getCategoriesInFolder(folderId: Long): Flow<List<CategoryWithLastLog>>

    @Query(
        """
        SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM (
            SELECT sortOrder FROM categories WHERE folderId IS NULL
            UNION ALL
            SELECT sortOrder FROM folders
        )
        """
    )
    suspend fun getNextSortOrder(): Int

    @Query("SELECT COALESCE(MAX(folderSortOrder), -1) + 1 FROM categories WHERE folderId = :folderId")
    suspend fun getNextFolderSortOrder(folderId: Long): Int

    @Query("UPDATE categories SET folderId = :folderId, folderSortOrder = :folderSortOrder WHERE id = :categoryId")
    suspend fun moveCategoryToFolder(categoryId: Long, folderId: Long, folderSortOrder: Int)

    @Query("UPDATE categories SET folderId = NULL, folderSortOrder = 0, sortOrder = :newSortOrder WHERE id = :categoryId")
    suspend fun removeCategoryFromFolder(categoryId: Long, newSortOrder: Int)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllList(): List<Category>

    @Update
    suspend fun updateAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)
}
