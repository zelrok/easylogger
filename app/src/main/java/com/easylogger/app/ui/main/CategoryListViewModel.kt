package com.easylogger.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.CategoryWithLastLog
import com.easylogger.app.data.local.entity.Folder
import com.easylogger.app.data.local.entity.FolderWithCount
import com.easylogger.app.data.repository.CategoryRepository
import com.easylogger.app.data.repository.FolderRepository
import com.easylogger.app.data.repository.LogEntryRepository
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

data class MainScreenState(
    val topLevelItems: List<MainListItem> = emptyList(),
    val folderCategories: List<CategoryWithLastLog> = emptyList(),
    val currentFolderId: Long? = null,
    val currentFolderName: String? = null,
    val viewMode: String = UserPreferenceRepository.VIEW_MODE_LIST,
    val isLoading: Boolean = true,
    val dragOverFolderId: Long? = null
)

sealed class MainScreenEvent {
    data class ExportReady(val suggestedFilename: String) : MainScreenEvent()
    data object ExportEmpty : MainScreenEvent()
}

private data class PendingFolderDrop(val categoryId: Long, val folderId: Long)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val folderRepository: FolderRepository,
    private val logEntryRepository: LogEntryRepository,
    private val userPreferenceRepository: UserPreferenceRepository
) : ViewModel() {

    private val _events = Channel<MainScreenEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    private val _currentFolderName = MutableStateFlow<String?>(null)
    private val _reorderedTopLevel = MutableStateFlow<List<MainListItem>?>(null)
    private val _reorderedFolderCategories = MutableStateFlow<List<CategoryWithLastLog>?>(null)
    private val _pendingFolderDrop = MutableStateFlow<PendingFolderDrop?>(null)
    private val _dragOverFolderId = MutableStateFlow<Long?>(null)

    private val topLevelFlow = combine(
        categoryRepository.getTopLevelWithLastLog(),
        folderRepository.getAllWithCount(),
        _reorderedTopLevel
    ) { categories, folders, reordered ->
        if (reordered != null) {
            reordered
        } else {
            val items = mutableListOf<MainListItem>()
            categories.forEach { items.add(MainListItem.CategoryItem(it)) }
            folders.forEach { items.add(MainListItem.FolderItem(it)) }
            items.sortedBy { it.sortOrder }
        }
    }

    private val folderCategoriesFlow = _currentFolderId.flatMapLatest { folderId ->
        if (folderId != null) {
            combine(
                categoryRepository.getCategoriesInFolder(folderId),
                _reorderedFolderCategories
            ) { dbCategories, reordered ->
                reordered ?: dbCategories
            }
        } else {
            flowOf(emptyList())
        }
    }

    val state: StateFlow<MainScreenState> = combine(
        topLevelFlow,
        folderCategoriesFlow,
        userPreferenceRepository.getViewMode(),
        _currentFolderId,
        _currentFolderName,
        _dragOverFolderId
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val topLevel = args[0] as List<MainListItem>
        @Suppress("UNCHECKED_CAST")
        val folderCats = args[1] as List<CategoryWithLastLog>
        val viewMode = args[2] as String
        val folderId = args[3] as Long?
        val folderName = args[4] as String?
        val dragOver = args[5] as Long?

        MainScreenState(
            topLevelItems = topLevel,
            folderCategories = folderCats,
            currentFolderId = folderId,
            currentFolderName = folderName,
            viewMode = viewMode,
            isLoading = false,
            dragOverFolderId = dragOver
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenState()
    )

    // --- Category operations ---

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.insert(name, _currentFolderId.value)
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
            folderRepository.insert(name)
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
        _currentFolderId.value = folderId
        _currentFolderName.value = folderName
    }

    fun exitFolder() {
        _reorderedFolderCategories.value = null
        _currentFolderId.value = null
        _currentFolderName.value = null
    }

    // --- Reorder operations ---

    fun onReorder(fromIndex: Int, toIndex: Int) {
        if (_currentFolderId.value != null) {
            onReorderInsideFolder(fromIndex, toIndex)
        } else {
            onReorderTopLevel(fromIndex, toIndex)
        }
    }

    private fun onReorderTopLevel(fromIndex: Int, toIndex: Int) {
        val current = state.value.topLevelItems.toMutableList()
        val draggedItem = current[fromIndex]
        val targetItem = current[toIndex]

        // Always perform the normal swap so dragging feels smooth
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _reorderedTopLevel.value = current

        // Track if a category is currently hovering over a folder position
        if (draggedItem is MainListItem.CategoryItem && targetItem is MainListItem.FolderItem) {
            _pendingFolderDrop.value = PendingFolderDrop(draggedItem.data.id, targetItem.data.id)
            _dragOverFolderId.value = targetItem.data.id
        } else {
            _pendingFolderDrop.value = null
            _dragOverFolderId.value = null
        }
    }

    private fun onReorderInsideFolder(fromIndex: Int, toIndex: Int) {
        val current = state.value.folderCategories.toMutableList()
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)
        _reorderedFolderCategories.value = current
    }

    fun onReorderConfirmed() {
        val drop = _pendingFolderDrop.value
        if (drop != null) {
            viewModelScope.launch {
                categoryRepository.moveCategoryToFolder(drop.categoryId, drop.folderId)
                _pendingFolderDrop.value = null
                _dragOverFolderId.value = null
                _reorderedTopLevel.value = null
            }
            return
        }

        if (_currentFolderId.value != null) {
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
                                folderSortOrder = cwl.folderSortOrder
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
                                createdAt = fwc.createdAt
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
            _reorderedTopLevel.value = null
        }
    }

    private fun confirmFolderReorder() {
        val reordered = _reorderedFolderCategories.value ?: return
        viewModelScope.launch {
            val updated = reordered.mapIndexed { index, cwl ->
                Category(
                    id = cwl.id,
                    name = cwl.name,
                    sortOrder = cwl.sortOrder,
                    createdAt = cwl.createdAt,
                    folderId = cwl.folderId,
                    folderSortOrder = index
                )
            }
            categoryRepository.updateSortOrders(updated)
            _reorderedFolderCategories.value = null
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
}
