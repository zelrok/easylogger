package com.easylogger.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.easylogger.app.data.local.dao.CategoryDao
import com.easylogger.app.data.local.dao.LogEntryDao
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.LogEntry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
            LogEntry(categoryId = categoryId, timestamp = 200L, createdAt = 200L)
        )
        val entries = logEntryDao.getAll()
        assertEquals(1, entries.size)
        assertEquals(entryId, entries[0].id)
    }

    @Test
    fun updateEntry() = runTest {
        val categoryId = categoryDao.insert(Category(name = "Cat", sortOrder = 0, createdAt = 100L))
        val entryId = logEntryDao.insert(
            LogEntry(categoryId = categoryId, timestamp = 200L, createdAt = 200L)
        )
        val entry = logEntryDao.getAll().first()
        logEntryDao.update(entry.copy(timestamp = 999L))
        val updated = logEntryDao.getAll().first()
        assertEquals(999L, updated.timestamp)
    }

    @Test
    fun deleteEntry() = runTest {
        val categoryId = categoryDao.insert(Category(name = "Cat", sortOrder = 0, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = categoryId, timestamp = 200L, createdAt = 200L))
        val entry = logEntryDao.getAll().first()
        logEntryDao.delete(entry)
        assertTrue(logEntryDao.getAll().isEmpty())
    }

    @Test
    fun getAllReturnsSortedByCategoryAndTimestamp() = runTest {
        val cat1 = categoryDao.insert(Category(name = "A", sortOrder = 0, createdAt = 100L))
        val cat2 = categoryDao.insert(Category(name = "B", sortOrder = 1, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = cat2, timestamp = 300L, createdAt = 300L))
        logEntryDao.insert(LogEntry(categoryId = cat1, timestamp = 100L, createdAt = 100L))
        logEntryDao.insert(LogEntry(categoryId = cat1, timestamp = 200L, createdAt = 200L))

        val entries = logEntryDao.getAll()
        assertEquals(3, entries.size)
        // Sorted by categoryId ASC, timestamp ASC
        assertEquals(cat1, entries[0].categoryId)
        assertEquals(100L, entries[0].timestamp)
        assertEquals(cat1, entries[1].categoryId)
        assertEquals(200L, entries[1].timestamp)
        assertEquals(cat2, entries[2].categoryId)
    }
}
