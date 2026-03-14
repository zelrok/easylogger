package com.easylogger.app.data.repository

import com.easylogger.app.data.local.dao.UserPreferenceDao
import com.easylogger.app.data.local.entity.UserPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class UserPreferenceRepositoryTest {

    private lateinit var fakeDao: FakeUserPreferenceDao
    private lateinit var repository: UserPreferenceRepository

    @Before
    fun setup() {
        fakeDao = FakeUserPreferenceDao()
        repository = UserPreferenceRepository(fakeDao)
    }

    @Test
    fun `getViewMode returns list as default`() = runTest {
        val mode = repository.getViewMode().first()
        assertEquals("list", mode)
    }

    @Test
    fun `setViewMode persists value`() = runTest {
        repository.setViewMode("grid")
        val mode = repository.getViewMode().first()
        assertEquals("grid", mode)
    }

    @Test
    fun `set and get custom preference`() = runTest {
        repository.set("custom_key", "custom_value")
        val pref = repository.get("custom_key").first()
        assertEquals("custom_value", pref?.value)
    }
}

class FakeUserPreferenceDao : UserPreferenceDao {
    private val prefs = mutableMapOf<String, UserPreference>()
    private val flows = mutableMapOf<String, MutableStateFlow<UserPreference?>>()

    override fun get(key: String): Flow<UserPreference?> {
        return flows.getOrPut(key) { MutableStateFlow(prefs[key]) }
    }

    override suspend fun set(preference: UserPreference) {
        prefs[preference.key] = preference
        flows.getOrPut(preference.key) { MutableStateFlow(null) }.value = preference
    }
}
