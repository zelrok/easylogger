package com.easylogger.app.ui.main

import com.easylogger.app.data.repository.CategoryRepository
import com.easylogger.app.data.repository.FakeCategoryDao
import com.easylogger.app.data.repository.FakeFolderDao
import com.easylogger.app.data.repository.FakeLogEntryDao
import com.easylogger.app.data.repository.FakeUserPreferenceDao
import com.easylogger.app.data.repository.FolderRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: CategoryListViewModel
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var folderRepository: FolderRepository
    private lateinit var logEntryRepository: LogEntryRepository
    private lateinit var userPreferenceRepository: UserPreferenceRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = CategoryRepository(FakeCategoryDao())
        folderRepository = FolderRepository(FakeFolderDao())
        logEntryRepository = LogEntryRepository(FakeLogEntryDao())
        userPreferenceRepository = UserPreferenceRepository(FakeUserPreferenceDao())
        viewModel = CategoryListViewModel(
            categoryRepository, folderRepository, logEntryRepository, userPreferenceRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty items and list view mode`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(emptyList<Any>(), state.topLevelItems)
        assertEquals("list", state.viewMode)
        assertNull(state.currentFolderId)
        collectJob.cancel()
    }

    @Test
    fun `addCategory adds to top-level items`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addCategory("Test Category")
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(1, state.topLevelItems.size)
        val item = state.topLevelItems[0] as MainListItem.CategoryItem
        assertEquals("Test Category", item.data.name)
        collectJob.cancel()
    }

    @Test
    fun `addFolder adds to top-level items`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addFolder("My Folder")
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(1, state.topLevelItems.size)
        val item = state.topLevelItems[0] as MainListItem.FolderItem
        assertEquals("My Folder", item.data.name)
        collectJob.cancel()
    }

    @Test
    fun `enterFolder sets currentFolderId`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.enterFolder(1L, "Test Folder")
        advanceUntilIdle()
        assertEquals(1L, viewModel.state.value.currentFolderId)
        assertEquals("Test Folder", viewModel.state.value.currentFolderName)
        collectJob.cancel()
    }

    @Test
    fun `exitFolder clears currentFolderId`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.enterFolder(1L, "Test Folder")
        advanceUntilIdle()
        viewModel.exitFolder()
        advanceUntilIdle()
        assertNull(viewModel.state.value.currentFolderId)
        assertNull(viewModel.state.value.currentFolderName)
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
