package com.easylogger.app.data.repository

import androidx.paging.PagingSource
import com.easylogger.app.data.local.dao.LogEntryDao
import com.easylogger.app.data.local.entity.LogEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogEntryRepository @Inject constructor(
    private val logEntryDao: LogEntryDao
) {
    fun getByCategoryId(categoryId: Long): PagingSource<Int, LogEntry> =
        logEntryDao.getByCategoryId(categoryId)

    fun getLastStartTime(categoryId: Long): Flow<Long?> =
        logEntryDao.getLastStartTime(categoryId)

    fun getOpenEntry(categoryId: Long): Flow<LogEntry?> =
        logEntryDao.getOpenEntry(categoryId)

    suspend fun getAll(): List<LogEntry> = logEntryDao.getAll()

    suspend fun insertInstant(categoryId: Long, timestamp: Long): Long {
        val entry = LogEntry(
            categoryId = categoryId,
            startTime = timestamp,
            endTime = timestamp,
            createdAt = System.currentTimeMillis()
        )
        return logEntryDao.insert(entry)
    }

    suspend fun insertStart(categoryId: Long, startTime: Long): Long {
        val entry = LogEntry(
            categoryId = categoryId,
            startTime = startTime,
            endTime = null,
            createdAt = System.currentTimeMillis()
        )
        return logEntryDao.insert(entry)
    }

    suspend fun insertManual(categoryId: Long, startTime: Long, endTime: Long): Long {
        val entry = LogEntry(
            categoryId = categoryId,
            startTime = startTime,
            endTime = endTime,
            createdAt = System.currentTimeMillis()
        )
        return logEntryDao.insert(entry)
    }

    suspend fun stopEntry(entry: LogEntry, endTime: Long) {
        logEntryDao.update(entry.copy(endTime = endTime))
    }

    suspend fun update(logEntry: LogEntry) = logEntryDao.update(logEntry)

    suspend fun delete(logEntry: LogEntry) = logEntryDao.delete(logEntry)
}
