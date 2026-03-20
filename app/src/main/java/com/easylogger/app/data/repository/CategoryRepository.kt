package com.easylogger.app.data.repository

import com.easylogger.app.data.local.dao.CategoryDao
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.CategoryWithLastLog
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllWithLastLog(): Flow<List<CategoryWithLastLog>> = categoryDao.getAllWithLastLog()

    fun getTopLevelWithLastLog(): Flow<List<CategoryWithLastLog>> = categoryDao.getTopLevelWithLastLog()

    fun getCategoriesInFolder(folderId: Long): Flow<List<CategoryWithLastLog>> =
        categoryDao.getCategoriesInFolder(folderId)

    fun getAll(): Flow<List<Category>> = categoryDao.getAll()

    suspend fun getById(id: Long): Category? = categoryDao.getById(id)

    suspend fun insert(name: String, folderId: Long? = null): Long {
        val category = if (folderId != null) {
            val nextFolderOrder = categoryDao.getNextFolderSortOrder(folderId)
            Category(
                name = name,
                sortOrder = 0,
                createdAt = System.currentTimeMillis(),
                folderId = folderId,
                folderSortOrder = nextFolderOrder
            )
        } else {
            val nextOrder = categoryDao.getNextSortOrder()
            Category(
                name = name,
                sortOrder = nextOrder,
                createdAt = System.currentTimeMillis()
            )
        }
        return categoryDao.insert(category)
    }

    suspend fun update(category: Category) = categoryDao.update(category)

    suspend fun updateSortOrders(categories: List<Category>) = categoryDao.updateAll(categories)

    suspend fun moveCategoryToFolder(categoryId: Long, folderId: Long) {
        val nextOrder = categoryDao.getNextFolderSortOrder(folderId)
        categoryDao.moveCategoryToFolder(categoryId, folderId, nextOrder)
    }

    suspend fun removeCategoryFromFolder(categoryId: Long) {
        val nextOrder = categoryDao.getNextSortOrder()
        categoryDao.removeCategoryFromFolder(categoryId, nextOrder)
    }

    suspend fun delete(category: Category) = categoryDao.delete(category)
}
