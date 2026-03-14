package com.easylogger.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.easylogger.app.data.local.dao.CategoryDao
import com.easylogger.app.data.local.dao.LogEntryDao
import com.easylogger.app.data.local.dao.UserPreferenceDao
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.LogEntry
import com.easylogger.app.data.local.entity.UserPreference

@Database(
    entities = [Category::class, LogEntry::class, UserPreference::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun userPreferenceDao(): UserPreferenceDao
}
