package com.easylogger.app.data.repository

import com.easylogger.app.data.local.dao.UserPreferenceDao
import com.easylogger.app.data.local.entity.UserPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferenceRepository @Inject constructor(
    private val userPreferenceDao: UserPreferenceDao
) {
    companion object {
        const val KEY_VIEW_MODE = "view_mode"
        const val VIEW_MODE_LIST = "list"
        const val VIEW_MODE_GRID = "grid"
    }

    fun getViewMode(): Flow<String> =
        userPreferenceDao.get(KEY_VIEW_MODE).map { it?.value ?: VIEW_MODE_LIST }

    suspend fun setViewMode(mode: String) {
        userPreferenceDao.set(UserPreference(KEY_VIEW_MODE, mode))
    }

    fun get(key: String): Flow<UserPreference?> = userPreferenceDao.get(key)

    suspend fun set(key: String, value: String) {
        userPreferenceDao.set(UserPreference(key, value))
    }
}
