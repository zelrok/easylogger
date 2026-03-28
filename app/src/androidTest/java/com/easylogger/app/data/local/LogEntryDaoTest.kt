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
class LogEntryDaoTest {

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
    fun insertAndRetrieveEntry() = runTest {
        val categoryId = categoryDao.insert(Category(name = "Cat", sortOrder = 0, createdAt = 100L))
        val entryId = logEntryDao.insert(
            LogEntry(categoryId = categoryId, startTime = 200L, endTime = 200L, createdAt = 200L)
        )
        val entries = logEntryDao.getAll()
        assertEquals(1, entries.size)
        assertEquals(entryId, entries[0].id)
    }

    @Test
    fun updateEntry() = runTest {
        val categoryId = categoryDao.insert(Category(name = "Cat", sortOrder = 0, createdAt = 100L))
        logEntryDao.insert(
            LogEntry(categoryId = categoryId, startTime = 200L, endTime = 200L, createdAt = 200L)
        )
        val entry = logEntryDao.getAll().first()
        logEntryDao.update(entry.copy(startTime = 999L, endTime = 999L))
        val updated = logEntryDao.getAll().first()
        assertEquals(999L, updated.startTime)
        assertEquals(999L, updated.endTime)
    }

    @Test
    fun deleteEntry() = runTest {
        val categoryId = categoryDao.insert(Category(name = "Cat", sortOrder = 0, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = categoryId, startTime = 200L, endTime = 200L, createdAt = 200L))
        val entry = logEntryDao.getAll().first()
        logEntryDao.delete(entry)
        assertTrue(logEntryDao.getAll().isEmpty())
    }

    @Test
    fun getAllReturnsSortedByCategoryAndStartTime() = runTest {
        val cat1 = categoryDao.insert(Category(name = "A", sortOrder = 0, createdAt = 100L))
        val cat2 = categoryDao.insert(Category(name = "B", sortOrder = 1, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = cat2, startTime = 300L, endTime = 300L, createdAt = 300L))
        logEntryDao.insert(LogEntry(categoryId = cat1, startTime = 100L, endTime = 100L, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = cat1, startTime = 200L, endTime = 200L, createdAt = 200L))

        val entries = logEntryDao.getAll()
        assertEquals(3, entries.size)
        assertEquals(cat1, entries[0].categoryId)
        assertEquals(100L, entries[0].startTime)
        assertEquals(cat1, entries[1].categoryId)
        assertEquals(200L, entries[1].startTime)
        assertEquals(cat2, entries[2].categoryId)
    }

    @Test
    fun getOpenEntryReturnsNullEndTimeEntry() = runTest {
        val categoryId = categoryDao.insert(Category(name = "Cat", sortOrder = 0, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = categoryId, startTime = 100L, endTime = 100L, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = categoryId, startTime = 200L, endTime = null, createdAt = 200L))

        val open = logEntryDao.getOpenEntry(categoryId).first()
        assertEquals(200L, open?.startTime)
        assertNull(open?.endTime)
    }

    @Test
    fun getOpenEntryReturnsNullWhenAllClosed() = runTest {
        val categoryId = categoryDao.insert(Category(name = "Cat", sortOrder = 0, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = categoryId, startTime = 100L, endTime = 100L, createdAt = 100L))

        val open = logEntryDao.getOpenEntry(categoryId).first()
        assertNull(open)
    }
}
