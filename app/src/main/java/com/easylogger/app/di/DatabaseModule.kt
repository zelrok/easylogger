package com.easylogger.app.di

import android.content.Context
import androidx.room.Room
import com.easylogger.app.data.local.AppDatabase
import com.easylogger.app.data.local.dao.CategoryDao
import com.easylogger.app.data.local.dao.LogEntryDao
import com.easylogger.app.data.local.dao.UserPreferenceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "easylogger.db"
        ).build()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideLogEntryDao(database: AppDatabase): LogEntryDao = database.logEntryDao()

    @Provides
    fun provideUserPreferenceDao(database: AppDatabase): UserPreferenceDao = database.userPreferenceDao()
}
