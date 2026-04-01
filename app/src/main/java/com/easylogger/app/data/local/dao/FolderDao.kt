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
        SELECT f.id, f.name, f.sortOrder, f.createdAt, f.parentFolderId, f.folderSortOrder,
               f.audioEnabled, f.autoNextEnabled, f.restDurationSeconds,
               (SELECT COUNT(*) FROM categories c WHERE c.folderId = f.id) +
               (SELECT COUNT(*) FROM folders sf WHERE sf.parentFolderId = f.id) +
               (SELECT COUNT(*) FROM questions q WHERE q.folderId = f.id) AS childCount
        FROM folders f
        WHERE f.parentFolderId IS NULL
        ORDER BY f.sortOrder ASC, f.createdAt ASC
        """
    )
    fun getTopLevelWithCount(): Flow<List<FolderWithCount>>

    @Query(
        """
        SELECT f.id, f.name, f.sortOrder, f.createdAt, f.parentFolderId, f.folderSortOrder,
               f.audioEnabled, f.autoNextEnabled, f.restDurationSeconds,
               (SELECT COUNT(*) FROM categories c WHERE c.folderId = f.id) +
               (SELECT COUNT(*) FROM folders sf WHERE sf.parentFolderId = f.id) +
               (SELECT COUNT(*) FROM questions q WHERE q.folderId = f.id) AS childCount
        FROM folders f
        WHERE f.parentFolderId = :parentFolderId
        ORDER BY f.folderSortOrder ASC, f.createdAt ASC
        """
    )
    fun getFoldersInFolder(parentFolderId: Long): Flow<List<FolderWithCount>>

    @Query(
        """
        SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM (
            SELECT sortOrder FROM categories WHERE folderId IS NULL
            UNION ALL
            SELECT sortOrder FROM folders WHERE parentFolderId IS NULL
            UNION ALL
            SELECT sortOrder FROM questions WHERE folderId IS NULL
        )
        """
    )
    suspend fun getNextTopLevelSortOrder(): Int

    @Query(
        """
        SELECT COALESCE(MAX(ord), -1) + 1 FROM (
            SELECT folderSortOrder AS ord FROM categories WHERE folderId = :parentFolderId
            UNION ALL
            SELECT folderSortOrder AS ord FROM folders WHERE parentFolderId = :parentFolderId
            UNION ALL
            SELECT folderSortOrder AS ord FROM questions WHERE folderId = :parentFolderId
        )
        """
    )
    suspend fun getNextFolderSortOrder(parentFolderId: Long): Int

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

    @Query("UPDATE folders SET parentFolderId = :parentFolderId, folderSortOrder = :folderSortOrder WHERE id = :folderId")
    suspend fun moveFolderToFolder(folderId: Long, parentFolderId: Long, folderSortOrder: Int)

    @Query("UPDATE folders SET parentFolderId = NULL, folderSortOrder = 0, sortOrder = :newSortOrder WHERE id = :folderId")
    suspend fun removeFolderFromParent(folderId: Long, newSortOrder: Int)
}
