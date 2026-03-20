package com.easylogger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.easylogger.app.data.local.entity.Folder
import com.easylogger.app.data.local.entity.FolderWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query(
        """
        SELECT f.id, f.name, f.sortOrder, f.createdAt,
               (SELECT COUNT(*) FROM categories c WHERE c.folderId = f.id) AS categoryCount
        FROM folders f
        ORDER BY f.sortOrder ASC, f.createdAt ASC
        """
    )
    fun getAllWithCount(): Flow<List<FolderWithCount>>

    @Query(
        """
        SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM (
            SELECT sortOrder FROM categories WHERE folderId IS NULL
            UNION ALL
            SELECT sortOrder FROM folders
        )
        """
    )
    suspend fun getNextTopLevelSortOrder(): Int

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: Folder): Long

    @Update
    suspend fun update(folder: Folder)

    @Update
    suspend fun updateAll(folders: List<Folder>)

    @Delete
    suspend fun delete(folder: Folder)
}
