package com.easylogger.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.CategoryWithLastLog
import com.easylogger.app.data.repository.CategoryRepository
import com.easylogger.app.data.repository.LogEntryRepository
import com.easylogger.app.data.repository.UserPreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainScreenState(
    val categories: List<CategoryWithLastLog> = emptyList(),
    val viewMode: String = UserPreferenceRepository.VIEW_MODE_LIST,
    val isLoading: Boolean = true
)

sealed class MainScreenEvent {
    data class ExportReady(val suggestedFilename: String) : MainScreenEvent()
    data object ExportEmpty : MainScreenEvent()
}

@HiltViewModel
class CategoryListViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val logEntryRepository: LogEntryRepository,
    private val userPreferenceRepository: UserPreferenceRepository
) : ViewModel() {

    private val _events = Channel<MainScreenEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val state: StateFlow<MainScreenState> = combine(
        categoryRepository.getAllWithLastLog(),
        userPreferenceRepository.getViewMode()
    ) { categories, viewMode ->
        MainScreenState(
            categories = categories,
            viewMode = viewMode,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenState()
    )

    fun addCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.insert(name)
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
