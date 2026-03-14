package com.easylogger.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.easylogger.app.data.local.dao.CategoryDao
import com.easylogger.app.data.local.dao.LogEntryDao
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.LogEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var logEntryDao: LogEntryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryDao = database.categoryDao()
        logEntryDao = database.logEntryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveCategory() = runTest {
        val category = Category(name = "Test", sortOrder = 0, createdAt = 100L)
        val id = categoryDao.insert(category)
        val result = categoryDao.getById(id)
        assertEquals("Test", result?.name)
    }

    @Test
    fun updateCategory() = runTest {
        val id = categoryDao.insert(Category(name = "Old", sortOrder = 0, createdAt = 100L))
        val category = categoryDao.getById(id)!!
        categoryDao.update(category.copy(name = "New"))
        val updated = categoryDao.getById(id)
        assertEquals("New", updated?.name)
    }

    @Test
    fun deleteCategory() = runTest {
        val id = categoryDao.insert(Category(name = "ToDelete", sortOrder = 0, createdAt = 100L))
        val category = categoryDao.getById(id)!!
        categoryDao.delete(category)
        assertNull(categoryDao.getById(id))
    }

    @Test
    fun cascadeDeleteRemovesLogEntries() = runTest {
        val categoryId = categoryDao.insert(Category(name = "Cat", sortOrder = 0, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = categoryId, timestamp = 200L, createdAt = 200L))
        logEntryDao.insert(LogEntry(categoryId = categoryId, timestamp = 300L, createdAt = 300L))

        val category = categoryDao.getById(categoryId)!!
        categoryDao.delete(category)

        val entries = logEntryDao.getAll()
        assertTrue(entries.isEmpty())
    }

    @Test
    fun getAllWithLastLogReturnsCorrectTimestamp() = runTest {
        val categoryId = categoryDao.insert(Category(name = "Cat", sortOrder = 0, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = categoryId, timestamp = 200L, createdAt = 200L))
        logEntryDao.insert(LogEntry(categoryId = categoryId, timestamp = 500L, createdAt = 300L))

        val result = categoryDao.getAllWithLastLog().first()
        assertEquals(1, result.size)
        assertEquals(500L, result[0].lastLogTimestamp)
    }
}
