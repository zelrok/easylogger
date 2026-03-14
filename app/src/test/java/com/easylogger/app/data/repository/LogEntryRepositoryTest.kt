package com.easylogger.app.data.repository

import androidx.paging.PagingSource
import com.easylogger.app.data.local.dao.LogEntryDao
import com.easylogger.app.data.local.entity.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogEntryRepositoryTest {

    private lateinit var fakeDao: FakeLogEntryDao
    private lateinit var repository: LogEntryRepository

    @Before
    fun setup() {
        fakeDao = FakeLogEntryDao()
        repository = LogEntryRepository(fakeDao)
    }

    @Test
    fun `insert sets createdAt and returns id`() = runTest {
        val timestamp = System.currentTimeMillis()
        val id = repository.insert(1L, timestamp)
        assertTrue(id > 0)
        val inserted = fakeDao.entries.first { it.id == id }
        assertEquals(1L, inserted.categoryId)
        assertEquals(timestamp, inserted.timestamp)
        assertTrue(inserted.createdAt > 0)
    }

    @Test
    fun `delete removes entry`() = runTest {
        val id = repository.insert(1L, 1000L)
        val entry = fakeDao.entries.first { it.id == id }
        repository.delete(entry)
        assertTrue(fakeDao.entries.isEmpty())
    }

    @Test
    fun `getAll returns all entries`() = runTest {
        repository.insert(1L, 1000L)
        repository.insert(1L, 2000L)
        val all = repository.getAll()
        assertEquals(2, all.size)
    }
}

class FakeLogEntryDao : LogEntryDao {
    val entries = mutableListOf<LogEntry>()
    private var nextId = 1L

    override fun getByCategoryId(categoryId: Long): PagingSource<Int, LogEntry> {
        throw UnsupportedOperationException("Not used in unit tests")
    }

    override suspend fun getAll(): List<LogEntry> = entries.toList()

    override fun getLastTimestamp(categoryId: Long): Flow<Long?> {
        return MutableStateFlow(entries.filter { it.categoryId == categoryId }.maxOfOrNull { it.timestamp })
    }

    override suspend fun insert(logEntry: LogEntry): Long {
        val id = nextId++
        entries.add(logEntry.copy(id = id))
        return id
    }

    override suspend fun update(logEntry: LogEntry) {
        val idx = entries.indexOfFirst { it.id == logEntry.id }
        if (idx >= 0) entries[idx] = logEntry
    }

    override suspend fun delete(logEntry: LogEntry) {
        entries.removeAll { it.id == logEntry.id }
    }
}
