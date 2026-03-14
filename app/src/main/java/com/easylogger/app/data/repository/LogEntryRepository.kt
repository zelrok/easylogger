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

    fun getLastTimestamp(categoryId: Long): Flow<Long?> =
        logEntryDao.getLastTimestamp(categoryId)

    suspend fun getAll(): List<LogEntry> = logEntryDao.getAll()

    suspend fun insert(categoryId: Long, timestamp: Long): Long {
        val entry = LogEntry(
            categoryId = categoryId,
            timestamp = timestamp,
            createdAt = System.currentTimeMillis()
        )
        return logEntryDao.insert(entry)
    }

    suspend fun update(logEntry: LogEntry) = logEntryDao.update(logEntry)

    suspend fun delete(logEntry: LogEntry) = logEntryDao.delete(logEntry)
}
