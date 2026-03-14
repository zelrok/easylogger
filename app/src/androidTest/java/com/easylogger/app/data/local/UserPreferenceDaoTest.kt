package com.easylogger.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.easylogger.app.data.local.dao.UserPreferenceDao
import com.easylogger.app.data.local.entity.UserPreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPreferenceDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: UserPreferenceDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.userPreferenceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieve() = runTest {
        dao.set(UserPreference("view_mode", "list"))
        val result = dao.get("view_mode").first()
        assertEquals("list", result?.value)
    }

    @Test
    fun upsertOverwritesExistingValue() = runTest {
        dao.set(UserPreference("view_mode", "list"))
        dao.set(UserPreference("view_mode", "grid"))
        val result = dao.get("view_mode").first()
        assertEquals("grid", result?.value)
    }

    @Test
    fun getNonExistentKeyReturnsNull() = runTest {
        val result = dao.get("nonexistent").first()
        assertNull(result)
    }
}
