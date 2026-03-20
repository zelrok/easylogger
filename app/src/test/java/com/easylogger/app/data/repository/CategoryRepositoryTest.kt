package com.easylogger.app.data.repository

import com.easylogger.app.data.local.dao.CategoryDao
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.CategoryWithLastLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {

    private lateinit var fakeDao: FakeCategoryDao
    private lateinit var repository: CategoryRepository

    @Before
    fun setup() {
        fakeDao = FakeCategoryDao()
        repository = CategoryRepository(fakeDao)
    }

    @Test
    fun `insert sets createdAt`() = runTest {
        val id = repository.insert("Test")
        val inserted = fakeDao.categories.first { it.id == id }
        assertTrue(inserted.createdAt > 0)
        assertEquals("Test", inserted.name)
    }

    @Test
    fun `getAll delegates to dao`() = runTest {
        fakeDao.insertDirect(Category(1, "A", 0, 100L))
        val result = repository.getAll().first()
        assertEquals(1, result.size)
        assertEquals("A", result[0].name)
    }

    @Test
    fun `delete removes category`() = runTest {
        fakeDao.insertDirect(Category(1, "A", 0, 100L))
        repository.delete(Category(1, "A", 0, 100L))
        val result = repository.getAll().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `moveCategoryToFolder sets folderId`() = runTest {
        fakeDao.insertDirect(Category(1, "A", 0, 100L))
        repository.moveCategoryToFolder(1L, 5L)
        val cat = fakeDao.categories.first { it.id == 1L }
        assertEquals(5L, cat.folderId)
    }

    @Test
    fun `removeCategoryFromFolder clears folderId`() = runTest {
        fakeDao.insertDirect(Category(1, "A", 0, 100L, folderId = 5L, folderSortOrder = 2))
        repository.removeCategoryFromFolder(1L)
        val cat = fakeDao.categories.first { it.id == 1L }
        assertNull(cat.folderId)
        assertEquals(0, cat.folderSortOrder)
    }
}

class FakeCategoryDao : CategoryDao {
    val categories = mutableListOf<Category>()
    private val flow = MutableStateFlow<List<Category>>(emptyList())
    private var nextId = 1L

    fun insertDirect(category: Category) {
        categories.add(category)
        flow.value = categories.toList()
    }

    override fun getAll(): Flow<List<Category>> = flow

    private fun mapToWithLastLog(list: List<Category>): List<CategoryWithLastLog> =
        list.map {
            CategoryWithLastLog(
                it.id, it.name, it.sortOrder, it.createdAt, null,
                it.folderId, it.folderSortOrder
            )
        }

    override fun getAllWithLastLog(): Flow<List<CategoryWithLastLog>> =
        flow.map { mapToWithLastLog(it) }

    override fun getTopLevelWithLastLog(): Flow<List<CategoryWithLastLog>> =
        flow.map { list -> mapToWithLastLog(list.filter { it.folderId == null }) }

    override fun getCategoriesInFolder(folderId: Long): Flow<List<CategoryWithLastLog>> =
        flow.map { list ->
            mapToWithLastLog(list.filter { it.folderId == folderId }
                .sortedBy { it.folderSortOrder })
        }

    override suspend fun getNextSortOrder(): Int =
        (categories.filter { it.folderId == null }.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun getNextFolderSortOrder(folderId: Long): Int =
        (categories.filter { it.folderId == folderId }.maxOfOrNull { it.folderSortOrder } ?: -1) + 1

    override suspend fun moveCategoryToFolder(categoryId: Long, folderId: Long, folderSortOrder: Int) {
        val idx = categories.indexOfFirst { it.id == categoryId }
        if (idx >= 0) {
            categories[idx] = categories[idx].copy(folderId = folderId, folderSortOrder = folderSortOrder)
            flow.value = categories.toList()
        }
    }

    override suspend fun removeCategoryFromFolder(categoryId: Long, newSortOrder: Int) {
        val idx = categories.indexOfFirst { it.id == categoryId }
        if (idx >= 0) {
            categories[idx] = categories[idx].copy(folderId = null, folderSortOrder = 0, sortOrder = newSortOrder)
            flow.value = categories.toList()
        }
    }

    override suspend fun getById(id: Long): Category? = categories.find { it.id == id }

    override suspend fun insert(category: Category): Long {
        val id = nextId++
        categories.add(category.copy(id = id))
        flow.value = categories.toList()
        return id
    }

    override suspend fun update(category: Category) {
        val idx = categories.indexOfFirst { it.id == category.id }
        if (idx >= 0) {
            categories[idx] = category
            flow.value = categories.toList()
        }
    }

    override suspend fun updateAll(categories: List<Category>) {
        for (updated in categories) {
            val idx = this.categories.indexOfFirst { it.id == updated.id }
            if (idx >= 0) this.categories[idx] = updated
        }
        flow.value = this.categories.toList()
    }

    override suspend fun getAllList(): List<Category> = categories.toList()

    override suspend fun delete(category: Category) {
        categories.removeAll { it.id == category.id }
        flow.value = categories.toList()
    }
}
