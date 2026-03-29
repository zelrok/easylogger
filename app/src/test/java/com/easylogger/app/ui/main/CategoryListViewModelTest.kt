package com.easylogger.app.ui.main

import com.easylogger.app.data.repository.AnswerRepository
import com.easylogger.app.data.repository.CategoryRepository
import com.easylogger.app.data.repository.FakeAnswerDao
import com.easylogger.app.data.repository.FakeCategoryDao
import com.easylogger.app.data.repository.FakeFolderDao
import com.easylogger.app.data.repository.FakeLogEntryDao
import com.easylogger.app.data.repository.FakeQuestionDao
import com.easylogger.app.data.repository.FakeUserPreferenceDao
import com.easylogger.app.data.repository.FolderRepository
import com.easylogger.app.data.repository.LogEntryRepository
import com.easylogger.app.data.repository.QuestionRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: CategoryListViewModel
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var folderRepository: FolderRepository
    private lateinit var logEntryRepository: LogEntryRepository
    private lateinit var questionRepository: QuestionRepository
    private lateinit var answerRepository: AnswerRepository
    private lateinit var userPreferenceRepository: UserPreferenceRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        categoryRepository = CategoryRepository(FakeCategoryDao())
        folderRepository = FolderRepository(FakeFolderDao())
        logEntryRepository = LogEntryRepository(FakeLogEntryDao())
        questionRepository = QuestionRepository(FakeQuestionDao())
        answerRepository = AnswerRepository(FakeAnswerDao())
        userPreferenceRepository = UserPreferenceRepository(FakeUserPreferenceDao())
        viewModel = CategoryListViewModel(
            categoryRepository, folderRepository, logEntryRepository,
            questionRepository, answerRepository, userPreferenceRepository
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
        assertTrue(state.folderStack.isEmpty())
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
    fun `enterFolder pushes to folder stack`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.enterFolder(1L, "Test Folder")
        advanceUntilIdle()
        assertEquals(1L, viewModel.state.value.currentFolderId)
        assertEquals("Test Folder", viewModel.state.value.currentFolderName)
        assertEquals(1, viewModel.state.value.folderStack.size)
        assertTrue(viewModel.state.value.isInsideFolder)
        collectJob.cancel()
    }

    @Test
    fun `enterFolder twice creates two-deep stack`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.enterFolder(1L, "Folder A")
        viewModel.enterFolder(2L, "Folder B")
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.folderStack.size)
        assertEquals(2L, viewModel.state.value.currentFolderId)
        assertEquals("Folder B", viewModel.state.value.currentFolderName)
        collectJob.cancel()
    }

    @Test
    fun `exitFolder pops one level from stack`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.enterFolder(1L, "Folder A")
        viewModel.enterFolder(2L, "Folder B")
        advanceUntilIdle()
        viewModel.exitFolder()
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.folderStack.size)
        assertEquals(1L, viewModel.state.value.currentFolderId)
        assertEquals("Folder A", viewModel.state.value.currentFolderName)
        collectJob.cancel()
    }

    @Test
    fun `exitFolder from single depth returns to top level`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.enterFolder(1L, "Test Folder")
        advanceUntilIdle()
        viewModel.exitFolder()
        advanceUntilIdle()
        assertNull(viewModel.state.value.currentFolderId)
        assertNull(viewModel.state.value.currentFolderName)
        assertTrue(viewModel.state.value.folderStack.isEmpty())
        assertFalse(viewModel.state.value.isInsideFolder)
        collectJob.cancel()
    }

    @Test
    fun `addFolder inside folder creates sub-folder`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addFolder("Parent")
        advanceUntilIdle()
        val parentFolder = viewModel.state.value.topLevelItems[0] as MainListItem.FolderItem
        viewModel.enterFolder(parentFolder.data.id, parentFolder.data.name)
        advanceUntilIdle()
        viewModel.addFolder("Child")
        advanceUntilIdle()
        val folderItems = viewModel.state.value.folderItems
        assertEquals(1, folderItems.size)
        val child = folderItems[0] as MainListItem.FolderItem
        assertEquals("Child", child.data.name)
        collectJob.cancel()
    }

    @Test
    fun `folderItems shows sub-folders and categories`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addFolder("Parent")
        advanceUntilIdle()
        val parentFolder = viewModel.state.value.topLevelItems[0] as MainListItem.FolderItem
        viewModel.enterFolder(parentFolder.data.id, parentFolder.data.name)
        advanceUntilIdle()
        viewModel.addCategory("Cat Inside")
        viewModel.addFolder("Sub Folder")
        advanceUntilIdle()
        val folderItems = viewModel.state.value.folderItems
        assertEquals(2, folderItems.size)
        val hasCategory = folderItems.any { it is MainListItem.CategoryItem }
        val hasFolder = folderItems.any { it is MainListItem.FolderItem }
        assertTrue(hasCategory)
        assertTrue(hasFolder)
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

    @Test
    fun `addQuestion adds text question to top-level items`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addQuestion("How do you feel?", "TEXT", "good,ok,bad", 1, 5)
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(1, state.topLevelItems.size)
        val item = state.topLevelItems[0] as MainListItem.QuestionItem
        assertEquals("How do you feel?", item.data.name)
        assertEquals("TEXT", item.data.answerType)
        collectJob.cancel()
    }

    @Test
    fun `addQuestion adds scale question to top-level items`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addQuestion("Rate your mood", "SCALE", null, 1, 10)
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(1, state.topLevelItems.size)
        val item = state.topLevelItems[0] as MainListItem.QuestionItem
        assertEquals("Rate your mood", item.data.name)
        assertEquals("SCALE", item.data.answerType)
        collectJob.cancel()
    }

    @Test
    fun `questions and categories sort together`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addCategory("Cat 1")
        advanceUntilIdle()
        viewModel.addQuestion("Q1", "TEXT", "yes,no", 1, 5)
        advanceUntilIdle()
        viewModel.addCategory("Cat 2")
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(3, state.topLevelItems.size)
        assertTrue(state.topLevelItems[0] is MainListItem.CategoryItem)
        assertTrue(state.topLevelItems[1] is MainListItem.QuestionItem)
        assertTrue(state.topLevelItems[2] is MainListItem.CategoryItem)
        collectJob.cancel()
    }

    @Test
    fun `addQuestion inside folder adds to folder items`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addFolder("Parent")
        advanceUntilIdle()
        val folder = viewModel.state.value.topLevelItems[0] as MainListItem.FolderItem
        viewModel.enterFolder(folder.data.id, folder.data.name)
        advanceUntilIdle()
        viewModel.addQuestion("Folder Q", "TEXT", "a,b", 1, 5)
        advanceUntilIdle()
        val folderItems = viewModel.state.value.folderItems
        assertEquals(1, folderItems.size)
        val item = folderItems[0] as MainListItem.QuestionItem
        assertEquals("Folder Q", item.data.name)
        collectJob.cancel()
    }

    @Test
    fun `deleteQuestion removes from top-level items`() = runTest {
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        viewModel.addQuestion("Q1", "TEXT", "yes,no", 1, 5)
        advanceUntilIdle()
        val item = viewModel.state.value.topLevelItems[0] as MainListItem.QuestionItem
        viewModel.deleteQuestion(item.data)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.topLevelItems.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `requestAnswerExport emits empty event when no answers`() = runTest {
        viewModel.requestAnswerExport()
        advanceUntilIdle()
        val event = viewModel.events.first()
        assert(event is MainScreenEvent.AnswerExportEmpty)
    }
}
