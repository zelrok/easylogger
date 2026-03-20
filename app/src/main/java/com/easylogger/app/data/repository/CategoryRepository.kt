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

    fun getAll(): Flow<List<Category>> = categoryDao.getAll()

    suspend fun getById(id: Long): Category? = categoryDao.getById(id)

    suspend fun insert(name: String): Long {
        val nextOrder = categoryDao.getNextSortOrder()
        val category = Category(
            name = name,
            sortOrder = nextOrder,
            createdAt = System.currentTimeMillis()
        )
        return categoryDao.insert(category)
    }

    suspend fun update(category: Category) = categoryDao.update(category)

    suspend fun updateSortOrders(categories: List<Category>) = categoryDao.updateAll(categories)

    suspend fun delete(category: Category) = categoryDao.delete(category)
}
