package com.easylogger.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.CategoryWithLastLog
import com.easylogger.app.data.local.entity.Folder
import com.easylogger.app.data.local.entity.FolderWithCount
import com.easylogger.app.data.local.entity.Question
import com.easylogger.app.data.local.entity.QuestionWithLastAnswer
import com.easylogger.app.data.repository.AnswerRepository
import com.easylogger.app.data.repository.CategoryRepository
import com.easylogger.app.data.repository.FolderRepository
import com.easylogger.app.data.repository.LogEntryRepository
import com.easylogger.app.data.repository.QuestionRepository
import com.easylogger.app.data.repository.UserPreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderStackEntry(val folderId: Long, val folderName: String)

data class MainScreenState(
    val topLevelItems: List<MainListItem> = emptyList(),
    val folderItems: List<MainListItem> = emptyList(),
    val folderStack: List<FolderStackEntry> = emptyList(),
    val viewMode: String = UserPreferenceRepository.VIEW_MODE_LIST,
    val isLoading: Boolean = true,
    val dragOverFolderId: Long? = null,
    val currentFolderAudioEnabled: Boolean = false,
    val currentFolderAutoNextEnabled: Boolean = false,
    val currentFolderRestDuration: Int? = null
) {
    val currentFolderId: Long? get() = folderStack.lastOrNull()?.folderId
    val currentFolderName: String? get() = folderStack.lastOrNull()?.folderName
    val isInsideFolder: Boolean get() = folderStack.isNotEmpty()
    val currentFolderIsTimed: Boolean get() = currentFolderAudioEnabled || currentFolderAutoNextEnabled
}

sealed class MainScreenEvent {
    data class ExportReady(val suggestedFilename: String) : MainScreenEvent()
    data class AnswerExportReady(val suggestedFilename: String) : MainScreenEvent()
    data object ExportEmpty : MainScreenEvent()
    data object AnswerExportEmpty : MainScreenEvent()
}

private sealed class PendingFolderDrop {
    data class CategoryDrop(val categoryId: Long, val folderId: Long) : PendingFolderDrop()
    data class FolderDrop(val sourceFolderId: Long, val targetFolderId: Long) : PendingFolderDrop()
    data class QuestionDrop(val questionId: Long, val folderId: Long) : PendingFolderDrop()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val folderRepository: FolderRepository,
    private val logEntryRepository: LogEntryRepository,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository,
    private val userPreferenceRepository: UserPreferenceRepository
) : ViewModel() {

    private val _events = Channel<MainScreenEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _folderStack = MutableStateFlow<List<FolderStackEntry>>(emptyList())
    private val _reorderedTopLevel = MutableStateFlow<List<MainListItem>?>(null)
    private val _reorderedFolderItems = MutableStateFlow<List<MainListItem>?>(null)
    private val _pendingFolderDrop = MutableStateFlow<PendingFolderDrop?>(null)
    private val _dragOverFolderId = MutableStateFlow<Long?>(null)

    private val topLevelFlow = combine(
        categoryRepository.getTopLevelWithLastLog(),
        folderRepository.getTopLevelWithCount(),
        questionRepository.getTopLevelWithLastAnswer(),
        _reorderedTopLevel
    ) { categories, folders, questions, reordered ->
        if (reordered != null) {
            reordered
        } else {
            val items = mutableListOf<MainListItem>()
            categories.forEach { items.add(MainListItem.CategoryItem(it)) }
            folders.forEach { items.add(MainListItem.FolderItem(it)) }
            questions.forEach { items.add(MainListItem.QuestionItem(it)) }
            items.sortedBy { it.sortOrder }
        }
    }

    private val folderItemsFlow = _folderStack.flatMapLatest { stack ->
        val folderId = stack.lastOrNull()?.folderId
        if (folderId != null) {
            combine(
                categoryRepository.getCategoriesInFolder(folderId),
                folderRepository.getFoldersInFolder(folderId),
                questionRepository.getQuestionsInFolder(folderId),
                _reorderedFolderItems
            ) { categories, subFolders, questions, reordered ->
                if (reordered != null) {
                    reordered
                } else {
                    val items = mutableListOf<MainListItem>()
                    categories.forEach { items.add(MainListItem.CategoryItem(it)) }
                    subFolders.forEach { items.add(MainListItem.FolderItem(it)) }
                    questions.forEach { items.add(MainListItem.QuestionItem(it)) }
                    items.sortedBy { it.folderSortOrder }
                }
            }
        } else {
            flowOf(emptyList())
        }
    }

    private val _currentFolderSettings = MutableStateFlow<com.easylogger.app.data.local.entity.Folder?>(null)

    init {
        viewModelScope.launch {
            _folderStack.collect { stack ->
                val folderId = stack.lastOrNull()?.folderId
                _currentFolderSettings.value = if (folderId != null) {
                    folderRepository.getById(folderId)
                } else {
                    null
                }
            }
        }
    }

    val state: StateFlow<MainScreenState> = combine(
        topLevelFlow,
        folderItemsFlow,
        userPreferenceRepository.getViewMode(),
        _folderStack,
        _dragOverFolderId,
        _currentFolderSettings
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val topLevel = args[0] as List<MainListItem>
        @Suppress("UNCHECKED_CAST")
        val folderItems = args[1] as List<MainListItem>
        val viewMode = args[2] as String
        @Suppress("UNCHECKED_CAST")
        val stack = args[3] as List<FolderStackEntry>
        val dragOver = args[4] as Long?
        val currentFolder = args[5] as? com.easylogger.app.data.local.entity.Folder

        MainScreenState(
            topLevelItems = topLevel,
            folderItems = folderItems,
            folderStack = stack,
            viewMode = viewMode,
            isLoading = false,
            dragOverFolderId = dragOver,
            currentFolderAudioEnabled = currentFolder?.audioEnabled ?: false,
            currentFolderAutoNextEnabled = currentFolder?.autoNextEnabled ?: false,
            currentFolderRestDuration = currentFolder?.restDurationSeconds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenState()
    )

    // --- Category operations ---

    fun addCategory(name: String, desiredDurationSeconds: Int? = null) {
        viewModelScope.launch {
            val currentFolder = _folderStack.value.lastOrNull()?.folderId
            categoryRepository.insert(name, currentFolder, desiredDurationSeconds)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.update(category)
        }
    }

    fun deleteCategory(category: CategoryWithLastLog) {
        viewModelScope.launch {
            val cat = categoryRepository.getById(category.id)
            if (cat != null) {
                categoryRepository.delete(cat)
            }
        }
    }

    fun removeCategoryFromFolder(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.removeCategoryFromFolder(categoryId)
        }
    }

    // --- Folder operations ---

    fun addFolder(name: String) {
        viewModelScope.launch {
            val currentFolder = _folderStack.value.lastOrNull()?.folderId
            if (currentFolder != null) {
                folderRepository.insertInFolder(name, currentFolder)
            } else {
                folderRepository.insert(name)
            }
        }
    }

    fun updateFolder(folder: Folder) {
        viewModelScope.launch {
            folderRepository.update(folder)
        }
    }

    fun deleteFolder(folder: FolderWithCount) {
        viewModelScope.launch {
            val f = folderRepository.getById(folder.id)
            if (f != null) {
                folderRepository.delete(f)
            }
        }
    }

    fun enterFolder(folderId: Long, folderName: String) {
        _reorderedTopLevel.value = null
        _reorderedFolderItems.value = null
        _folderStack.value = _folderStack.value + FolderStackEntry(folderId, folderName)
    }

    fun exitFolder() {
        _reorderedFolderItems.value = null
        val stack = _folderStack.value
        _folderStack.value = if (stack.size <= 1) emptyList() else stack.dropLast(1)
        if (_folderStack.value.isEmpty()) {
            _reorderedTopLevel.value = null
        }
    }

    fun removeFolderFromParent(folderId: Long) {
        viewModelScope.launch {
            folderRepository.removeFolderFromParent(folderId)
        }
    }

    fun updateFolderSettings(
        folderId: Long,
        audioEnabled: Boolean,
        autoNextEnabled: Boolean,
        restDurationSeconds: Int?
    ) {
        viewModelScope.launch {
            folderRepository.updateSettings(folderId, audioEnabled, autoNextEnabled, restDurationSeconds)
            _currentFolderSettings.value = folderRepository.getById(folderId)
        }
    }

    // --- Question operations ---

    fun addQuestion(
        name: String,
        answerType: String,
        textOptions: String?,
        scaleMin: Int,
        scaleMax: Int,
        desiredDurationSeconds: Int? = null
    ) {
        viewModelScope.launch {
            val currentFolder = _folderStack.value.lastOrNull()?.folderId
            questionRepository.insert(name, answerType, textOptions, scaleMin, scaleMax, currentFolder, desiredDurationSeconds)
        }
    }

    fun updateQuestion(question: Question) {
        viewModelScope.launch {
            questionRepository.update(question)
        }
    }

    fun deleteQuestion(question: QuestionWithLastAnswer) {
        viewModelScope.launch {
            val q = questionRepository.getById(question.id)
            if (q != null) {
                questionRepository.delete(q)
            }
        }
    }

    fun removeQuestionFromFolder(questionId: Long) {
        viewModelScope.launch {
            questionRepository.removeQuestionFromFolder(questionId)
        }
    }

    // --- Reorder operations ---

    fun onReorder(fromIndex: Int, toIndex: Int) {
        if (_folderStack.value.isNotEmpty()) {
            onReorderInsideFolder(fromIndex, toIndex)
        } else {
            onReorderTopLevel(fromIndex, toIndex)
        }
    }

    private fun onReorderTopLevel(fromIndex: Int, toIndex: Int) {
        val current = state.value.topLevelItems.toMutableList()
        val draggedItem = current[fromIndex]
        val targetItem = current[toIndex]

        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _reorderedTopLevel.value = current

        if (targetItem is MainListItem.FolderItem) {
            when (draggedItem) {
                is MainListItem.CategoryItem -> {
                    _pendingFolderDrop.value = PendingFolderDrop.CategoryDrop(draggedItem.data.id, targetItem.data.id)
                    _dragOverFolderId.value = targetItem.data.id
                }
                is MainListItem.FolderItem -> {
                    _pendingFolderDrop.value = PendingFolderDrop.FolderDrop(draggedItem.data.id, targetItem.data.id)
                    _dragOverFolderId.value = targetItem.data.id
                }
                is MainListItem.QuestionItem -> {
                    _pendingFolderDrop.value = PendingFolderDrop.QuestionDrop(draggedItem.data.id, targetItem.data.id)
                    _dragOverFolderId.value = targetItem.data.id
                }
            }
        } else {
            _pendingFolderDrop.value = null
            _dragOverFolderId.value = null
        }
    }

    private fun onReorderInsideFolder(fromIndex: Int, toIndex: Int) {
        val current = state.value.folderItems.toMutableList()
        val draggedItem = current[fromIndex]
        val targetItem = current[toIndex]

        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _reorderedFolderItems.value = current

        if (targetItem is MainListItem.FolderItem) {
            when (draggedItem) {
                is MainListItem.CategoryItem -> {
                    _pendingFolderDrop.value = PendingFolderDrop.CategoryDrop(draggedItem.data.id, targetItem.data.id)
                    _dragOverFolderId.value = targetItem.data.id
                }
                is MainListItem.FolderItem -> {
                    _pendingFolderDrop.value = PendingFolderDrop.FolderDrop(draggedItem.data.id, targetItem.data.id)
                    _dragOverFolderId.value = targetItem.data.id
                }
                is MainListItem.QuestionItem -> {
                    _pendingFolderDrop.value = PendingFolderDrop.QuestionDrop(draggedItem.data.id, targetItem.data.id)
                    _dragOverFolderId.value = targetItem.data.id
                }
            }
        } else {
            _pendingFolderDrop.value = null
            _dragOverFolderId.value = null
        }
    }

    fun onReorderConfirmed() {
        val drop = _pendingFolderDrop.value
        if (drop != null) {
            viewModelScope.launch {
                when (drop) {
                    is PendingFolderDrop.CategoryDrop -> {
                        categoryRepository.moveCategoryToFolder(drop.categoryId, drop.folderId)
                    }
                    is PendingFolderDrop.FolderDrop -> {
                        if (drop.sourceFolderId != drop.targetFolderId) {
                            folderRepository.moveFolderToFolder(drop.sourceFolderId, drop.targetFolderId)
                        }
                    }
                    is PendingFolderDrop.QuestionDrop -> {
                        questionRepository.moveQuestionToFolder(drop.questionId, drop.folderId)
                    }
                }
                _pendingFolderDrop.value = null
                _dragOverFolderId.value = null
                _reorderedTopLevel.value = null
                _reorderedFolderItems.value = null
            }
            return
        }

        if (_folderStack.value.isNotEmpty()) {
            confirmFolderReorder()
        } else {
            confirmTopLevelReorder()
        }
    }

    private fun confirmTopLevelReorder() {
        val reordered = _reorderedTopLevel.value ?: return
        viewModelScope.launch {
            val updatedCategories = mutableListOf<Category>()
            val updatedFolders = mutableListOf<Folder>()
            val updatedQuestions = mutableListOf<Question>()

            reordered.forEachIndexed { index, item ->
                when (item) {
                    is MainListItem.CategoryItem -> {
                        val cwl = item.data
                        updatedCategories.add(
                            Category(
                                id = cwl.id,
                                name = cwl.name,
                                sortOrder = index,
                                createdAt = cwl.createdAt,
                                folderId = cwl.folderId,
                                folderSortOrder = cwl.folderSortOrder,
                                desiredDurationSeconds = cwl.desiredDurationSeconds
                            )
                        )
                    }
                    is MainListItem.FolderItem -> {
                        val fwc = item.data
                        updatedFolders.add(
                            Folder(
                                id = fwc.id,
                                name = fwc.name,
                                sortOrder = index,
                                createdAt = fwc.createdAt,
                                parentFolderId = fwc.parentFolderId,
                                folderSortOrder = fwc.folderSortOrder,
                                audioEnabled = fwc.audioEnabled,
                                autoNextEnabled = fwc.autoNextEnabled,
                                restDurationSeconds = fwc.restDurationSeconds
                            )
                        )
                    }
                    is MainListItem.QuestionItem -> {
                        val qwa = item.data
                        updatedQuestions.add(
                            Question(
                                id = qwa.id,
                                name = qwa.name,
                                answerType = qwa.answerType,
                                textOptions = qwa.textOptions,
                                scaleMin = qwa.scaleMin,
                                scaleMax = qwa.scaleMax,
                                sortOrder = index,
                                createdAt = qwa.createdAt,
                                folderId = qwa.folderId,
                                folderSortOrder = qwa.folderSortOrder,
                                desiredDurationSeconds = qwa.desiredDurationSeconds
                            )
                        )
                    }
                }
            }

            if (updatedCategories.isNotEmpty()) {
                categoryRepository.updateSortOrders(updatedCategories)
            }
            if (updatedFolders.isNotEmpty()) {
                folderRepository.updateSortOrders(updatedFolders)
            }
            if (updatedQuestions.isNotEmpty()) {
                questionRepository.updateSortOrders(updatedQuestions)
            }
            _reorderedTopLevel.value = null
        }
    }

    private fun confirmFolderReorder() {
        val reordered = _reorderedFolderItems.value ?: return
        viewModelScope.launch {
            val updatedCategories = mutableListOf<Category>()
            val updatedFolders = mutableListOf<Folder>()
            val updatedQuestions = mutableListOf<Question>()

            reordered.forEachIndexed { index, item ->
                when (item) {
                    is MainListItem.CategoryItem -> {
                        val cwl = item.data
                        updatedCategories.add(
                            Category(
                                id = cwl.id,
                                name = cwl.name,
                                sortOrder = cwl.sortOrder,
                                createdAt = cwl.createdAt,
                                folderId = cwl.folderId,
                                folderSortOrder = index,
                                desiredDurationSeconds = cwl.desiredDurationSeconds
                            )
                        )
                    }
                    is MainListItem.FolderItem -> {
                        val fwc = item.data
                        updatedFolders.add(
                            Folder(
                                id = fwc.id,
                                name = fwc.name,
                                sortOrder = fwc.sortOrder,
                                createdAt = fwc.createdAt,
                                parentFolderId = fwc.parentFolderId,
                                folderSortOrder = index,
                                audioEnabled = fwc.audioEnabled,
                                autoNextEnabled = fwc.autoNextEnabled,
                                restDurationSeconds = fwc.restDurationSeconds
                            )
                        )
                    }
                    is MainListItem.QuestionItem -> {
                        val qwa = item.data
                        updatedQuestions.add(
                            Question(
                                id = qwa.id,
                                name = qwa.name,
                                answerType = qwa.answerType,
                                textOptions = qwa.textOptions,
                                scaleMin = qwa.scaleMin,
                                scaleMax = qwa.scaleMax,
                                sortOrder = qwa.sortOrder,
                                createdAt = qwa.createdAt,
                                folderId = qwa.folderId,
                                folderSortOrder = index,
                                desiredDurationSeconds = qwa.desiredDurationSeconds
                            )
                        )
                    }
                }
            }

            if (updatedCategories.isNotEmpty()) {
                categoryRepository.updateSortOrders(updatedCategories)
            }
            if (updatedFolders.isNotEmpty()) {
                folderRepository.updateSortOrders(updatedFolders)
            }
            if (updatedQuestions.isNotEmpty()) {
                questionRepository.updateSortOrders(updatedQuestions)
            }
            _reorderedFolderItems.value = null
        }
    }

    // --- View mode ---

    fun toggleViewMode() {
        viewModelScope.launch {
            val current = state.value.viewMode
            val newMode = if (current == UserPreferenceRepository.VIEW_MODE_LIST) {
                UserPreferenceRepository.VIEW_MODE_GRID
            } else {
                UserPreferenceRepository.VIEW_MODE_LIST
            }
            userPreferenceRepository.setViewMode(newMode)
        }
    }

    // --- Export ---

    fun requestExport() {
        viewModelScope.launch {
            val entries = logEntryRepository.getAll()
            if (entries.isEmpty()) {
                _events.send(MainScreenEvent.ExportEmpty)
            } else {
                val timestamp = java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    java.util.Locale.US
                ).format(java.util.Date())
                val filename = "easylogger_export_$timestamp.csv"
                _events.send(MainScreenEvent.ExportReady(filename))
            }
        }
    }

    fun requestAnswerExport() {
        viewModelScope.launch {
            val answers = answerRepository.getAll()
            if (answers.isEmpty()) {
                _events.send(MainScreenEvent.AnswerExportEmpty)
            } else {
                val timestamp = java.text.SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    java.util.Locale.US
                ).format(java.util.Date())
                val filename = "easylogger_answers_$timestamp.csv"
                _events.send(MainScreenEvent.AnswerExportReady(filename))
            }
        }
    }
}
