package com.easylogger.app.data.repository

import com.easylogger.app.data.local.dao.FolderDao
import com.easylogger.app.data.local.entity.Folder
import com.easylogger.app.data.local.entity.FolderWithCount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val folderDao: FolderDao
) {
    fun getAllWithCount(): Flow<List<FolderWithCount>> = folderDao.getAllWithCount()

    suspend fun getById(id: Long): Folder? = folderDao.getById(id)

    suspend fun insert(name: String): Long {
        val nextOrder = folderDao.getNextTopLevelSortOrder()
        val folder = Folder(
            name = name,
            sortOrder = nextOrder,
            createdAt = System.currentTimeMillis()
        )
        return folderDao.insert(folder)
    }

    suspend fun update(folder: Folder) = folderDao.update(folder)

    suspend fun updateSortOrders(folders: List<Folder>) = folderDao.updateAll(folders)

    suspend fun delete(folder: Folder) = folderDao.delete(folder)
}
