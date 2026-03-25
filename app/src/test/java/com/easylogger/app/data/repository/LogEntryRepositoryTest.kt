package com.easylogger.app.data.repository

import androidx.paging.PagingSource
import com.easylogger.app.data.local.dao.LogEntryDao
import com.easylogger.app.data.local.entity.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
        val id = repository.insertInstant(1L, timestamp)
        assertTrue(id > 0)
        val inserted = fakeDao.entries.first { it.id == id }
        assertEquals(1L, inserted.categoryId)
        assertEquals(timestamp, inserted.startTime)
        assertEquals(timestamp, inserted.endTime)
        assertTrue(inserted.createdAt > 0)
    }

    @Test
    fun `delete removes entry`() = runTest {
        val id = repository.insertInstant(1L, 1000L)
        val entry = fakeDao.entries.first { it.id == id }
        repository.delete(entry)
        assertTrue(fakeDao.entries.isEmpty())
    }

    @Test
    fun `getAll returns all entries`() = runTest {
        repository.insertInstant(1L, 1000L)
        repository.insertInstant(1L, 2000L)
        val all = repository.getAll()
        assertEquals(2, all.size)
    }

    @Test
    fun `insertStart creates entry with null endTime`() = runTest {
        val id = repository.insertStart(1L, 5000L)
        val entry = fakeDao.entries.first { it.id == id }
        assertEquals(5000L, entry.startTime)
        assertEquals(null, entry.endTime)
    }

    @Test
    fun `stopEntry sets endTime on open entry`() = runTest {
        val id = repository.insertStart(1L, 5000L)
        val entry = fakeDao.entries.first { it.id == id }
        repository.stopEntry(entry, 6000L)
        val updated = fakeDao.entries.first { it.id == id }
        assertEquals(6000L, updated.endTime)
    }

    @Test
    fun `insertManual creates entry with explicit start and end`() = runTest {
        val id = repository.insertManual(1L, 1000L, 2000L)
        val entry = fakeDao.entries.first { it.id == id }
        assertEquals(1000L, entry.startTime)
        assertEquals(2000L, entry.endTime)
    }
}

class FakeLogEntryDao : LogEntryDao {
    val entries = mutableListOf<LogEntry>()
    private var nextId = 1L
    private val _entriesFlow = MutableStateFlow<List<LogEntry>>(emptyList())

    private fun notifyChange() {
        _entriesFlow.value = entries.toList()
    }

    override fun getByCategoryId(categoryId: Long): PagingSource<Int, LogEntry> {
        throw UnsupportedOperationException("Not used in unit tests")
    }

    override suspend fun getAll(): List<LogEntry> = entries.toList()

    override fun getLastStartTime(categoryId: Long): Flow<Long?> {
        return _entriesFlow.map { list ->
            list.filter { it.categoryId == categoryId }.maxOfOrNull { it.startTime }
        }
    }

    override fun getOpenEntry(categoryId: Long): Flow<LogEntry?> {
        return _entriesFlow.map { list ->
            list.firstOrNull { it.categoryId == categoryId && it.endTime == null }
        }
    }

    override suspend fun insert(logEntry: LogEntry): Long {
        val id = nextId++
        entries.add(logEntry.copy(id = id))
        notifyChange()
        return id
    }

    override suspend fun update(logEntry: LogEntry) {
        val idx = entries.indexOfFirst { it.id == logEntry.id }
        if (idx >= 0) entries[idx] = logEntry
        notifyChange()
    }

    override suspend fun delete(logEntry: LogEntry) {
        entries.removeAll { it.id == logEntry.id }
        notifyChange()
    }
}
