package com.easylogger.app.ui.main

import com.easylogger.app.data.repository.CategoryRepository
import com.easylogger.app.data.repository.FakeCategoryDao
import com.easylogger.app.data.repository.FakeLogEntryDao
import com.easylogger.app.data.repository.FakeUserPreferenceDao
import com.easylogger.app.data.repository.LogEntryRepository
import com.easylogger.app.data.repository.UserPreferenceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: CategoryListViewModel
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var logEntryRepository: LogEntryRepository
    private lateinit var userPreferenceRepository: UserPreferenceRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = CategoryRepository(FakeCategoryDao())
        logEntryRepository = LogEntryRepository(FakeLogEntryDao())
        userPreferenceRepository = UserPreferenceRepository(FakeUserPreferenceDao())
        viewModel = CategoryListViewModel(categoryRepository, logEntryRepository, userPreferenceRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty categories and list view mode`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(emptyList<Any>(), state.categories)
        assertEquals("list", state.viewMode)
        collectJob.cancel()
    }

    @Test
    fun `addCategory adds to state`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addCategory("Test Category")
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(1, state.categories.size)
        assertEquals("Test Category", state.categories[0].name)
        collectJob.cancel()
    }

    @Test
    fun `toggleViewMode switches between list and grid`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        advanceUntilIdle()
        assertEquals("list", viewModel.state.value.viewMode)

        viewModel.toggleViewMode()
        advanceUntilIdle()
        assertEquals("grid", viewModel.state.value.viewMode)

        viewModel.toggleViewMode()
        advanceUntilIdle()
        assertEquals("list", viewModel.state.value.viewMode)
        collectJob.cancel()
    }

    @Test
    fun `requestExport emits empty event when no entries`() = runTest {
        viewModel.requestExport()
        advanceUntilIdle()
        val event = viewModel.events.first()
        assert(event is MainScreenEvent.ExportEmpty)
    }

    @Test
    fun `state is not loading after initial load`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
        collectJob.cancel()
    }
}
