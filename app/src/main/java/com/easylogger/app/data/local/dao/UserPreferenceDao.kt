package com.easylogger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.easylogger.app.data.local.entity.UserPreference
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferenceDao {

    @Query("SELECT * FROM user_preferences WHERE `key` = :key")
    fun get(key: String): Flow<UserPreference?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(preference: UserPreference)
}
