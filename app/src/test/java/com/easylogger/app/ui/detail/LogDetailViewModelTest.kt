package com.easylogger.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.repository.CategoryRepository
import com.easylogger.app.data.repository.FakeCategoryDao
import com.easylogger.app.data.repository.FakeLogEntryDao
import com.easylogger.app.data.repository.LogEntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LogDetailViewModel
    private lateinit var categoryDao: FakeCategoryDao
    private lateinit var logEntryDao: FakeLogEntryDao

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        categoryDao = FakeCategoryDao()
        logEntryDao = FakeLogEntryDao()

        categoryDao.insertDirect(Category(1L, "Test", 0, 100L))

        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to 1L))
        viewModel = LogDetailViewModel(
            savedStateHandle,
            CategoryRepository(categoryDao),
            LogEntryRepository(logEntryDao)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `category is loaded on init`() = runTest {
        advanceUntilIdle()
        assertEquals("Test", viewModel.category.value?.name)
    }

    @Test
    fun `logNow creates entry and activates cooldown`() = runTest {
        advanceUntilIdle()
        viewModel.logNow()
        advanceUntilIdle()
        assertEquals(1, logEntryDao.entries.size)
        assertEquals(1L, logEntryDao.entries[0].categoryId)
    }

    @Test
    fun `logNow debounces rapid calls`() = runTest {
        advanceUntilIdle()
        viewModel.logNow()
        viewModel.logNow() // Should be ignored due to debounce
        advanceUntilIdle()
        assertEquals(1, logEntryDao.entries.size)
    }

    @Test
    fun `logManual creates entry with specified timestamp`() = runTest {
        val timestamp = 1234567890L
        viewModel.logManual(timestamp)
        advanceUntilIdle()
        assertEquals(1, logEntryDao.entries.size)
        assertEquals(timestamp, logEntryDao.entries[0].timestamp)
    }

    @Test
    fun `deleteEntry removes entry`() = runTest {
        viewModel.logManual(1000L)
        advanceUntilIdle()
        val entry = logEntryDao.entries[0]
        viewModel.deleteEntry(entry)
        advanceUntilIdle()
        assertTrue(logEntryDao.entries.isEmpty())
    }

    @Test
    fun `cooldown deactivates after delay`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.cooldownActive.value)
        viewModel.logNow()
        advanceUntilIdle()
        assertFalse(viewModel.cooldownActive.value) // After delay, should be false
    }
}
