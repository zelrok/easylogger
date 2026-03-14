package com.easylogger.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.LogEntry
import com.easylogger.app.data.repository.CategoryRepository
import com.easylogger.app.data.repository.LogEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetailEvent {
    data class LoggedAt(val formattedTime: String) : DetailEvent()
}

@HiltViewModel
class LogDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val logEntryRepository: LogEntryRepository
) : ViewModel() {

    val categoryId: Long = savedStateHandle.get<Long>("categoryId") ?: 0L

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _category = MutableStateFlow<Category?>(null)
    val category: StateFlow<Category?> = _category.asStateFlow()

    private val _cooldownActive = MutableStateFlow(false)
    val cooldownActive: StateFlow<Boolean> = _cooldownActive.asStateFlow()

    private var lastLogTimeMillis = 0L

    val entries: Flow<PagingData<LogEntry>> = Pager(
        config = PagingConfig(pageSize = 50),
        pagingSourceFactory = { logEntryRepository.getByCategoryId(categoryId) }
    ).flow.cachedIn(viewModelScope)

    val lastTimestamp: StateFlow<Long?> = logEntryRepository.getLastTimestamp(categoryId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            _category.value = categoryRepository.getById(categoryId)
        }
    }

    fun logNow() {
        val now = System.currentTimeMillis()
        if (now - lastLogTimeMillis < 500) return

        lastLogTimeMillis = now
        _cooldownActive.value = true

        viewModelScope.launch {
            logEntryRepository.insert(categoryId, now)
            val formatted = java.text.SimpleDateFormat(
                "h:mm a",
                java.util.Locale.getDefault()
            ).format(java.util.Date(now))
            _events.send(DetailEvent.LoggedAt(formatted))

            kotlinx.coroutines.delay(500)
            _cooldownActive.value = false
        }
    }

    fun logManual(timestamp: Long) {
        viewModelScope.launch {
            logEntryRepository.insert(categoryId, timestamp)
        }
    }

    fun updateEntry(entry: LogEntry, newTimestamp: Long) {
        viewModelScope.launch {
            logEntryRepository.update(entry.copy(timestamp = newTimestamp))
        }
    }

    fun deleteEntry(entry: LogEntry) {
        viewModelScope.launch {
            logEntryRepository.delete(entry)
        }
    }
}
