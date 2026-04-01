package com.easylogger.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.easylogger.app.data.local.dao.AnswerDao
import com.easylogger.app.data.local.dao.CategoryDao
import com.easylogger.app.data.local.dao.FolderDao
import com.easylogger.app.data.local.dao.LogEntryDao
import com.easylogger.app.data.local.dao.QuestionDao
import com.easylogger.app.data.local.dao.UserPreferenceDao
import com.easylogger.app.data.local.entity.Answer
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.Folder
import com.easylogger.app.data.local.entity.LogEntry
import com.easylogger.app.data.local.entity.Question
import com.easylogger.app.data.local.entity.UserPreference

@Database(
    entities = [Category::class, LogEntry::class, UserPreference::class, Folder::class, Question::class, Answer::class],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun logEntryDao(): LogEntryDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun folderDao(): FolderDao
    abstract fun questionDao(): QuestionDao
    abstract fun answerDao(): AnswerDao
}
