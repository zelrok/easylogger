package com.easylogger.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.easylogger.app.data.local.entity.LogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LogEntryDao {

    @Query("SELECT * FROM log_entries WHERE categoryId = :categoryId ORDER BY startTime DESC")
    fun getByCategoryId(categoryId: Long): PagingSource<Int, LogEntry>

    @Query("SELECT * FROM log_entries ORDER BY categoryId ASC, startTime ASC")
    suspend fun getAll(): List<LogEntry>

    @Query("SELECT MAX(startTime) FROM log_entries WHERE categoryId = :categoryId")
    fun getLastStartTime(categoryId: Long): Flow<Long?>

    @Query("SELECT * FROM log_entries WHERE categoryId = :categoryId AND endTime IS NULL LIMIT 1")
    fun getOpenEntry(categoryId: Long): Flow<LogEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(logEntry: LogEntry): Long

    @Update
    suspend fun update(logEntry: LogEntry)

    @Delete
    suspend fun delete(logEntry: LogEntry)
}
