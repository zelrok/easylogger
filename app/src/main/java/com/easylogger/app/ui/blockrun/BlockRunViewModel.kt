package com.easylogger.app.ui.blockrun

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.Question
import com.easylogger.app.data.repository.AnswerRepository
import com.easylogger.app.data.repository.CategoryRepository
import com.easylogger.app.data.repository.FolderRepository
import com.easylogger.app.data.repository.LogEntryRepository
import com.easylogger.app.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BlockItem(val folderSortOrder: Int) {
    data class CategoryBlockItem(val category: Category) : BlockItem(category.folderSortOrder)
    data class QuestionBlockItem(val question: Question) : BlockItem(question.folderSortOrder)

    val name: String
        get() = when (this) {
            is CategoryBlockItem -> category.name
            is QuestionBlockItem -> question.name
        }

    val desiredDurationSeconds: Int?
        get() = when (this) {
            is CategoryBlockItem -> category.desiredDurationSeconds
            is QuestionBlockItem -> question.desiredDurationSeconds
        }
}

enum class BlockRunPhase { SHOWING_ITEM, SHOWING_REST, COMPLETED }
enum class TimerState { IDLE, RUNNING, PAUSED }

data class BlockRunUiState(
    val folderName: String = "",
    val items: List<BlockItem> = emptyList(),
    val currentIndex: Int = 0,
    val phase: BlockRunPhase = BlockRunPhase.SHOWING_ITEM,
    val timerState: TimerState = TimerState.IDLE,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val isTimed: Boolean = false,
    val audioEnabled: Boolean = false,
    val autoNextEnabled: Boolean = false,
    val restDurationSeconds: Int? = null,
    val hasOpenLogEntry: Boolean = false,
    val logStartTimeMillis: Long = 0L,
    val pausedAtMillis: Long = 0L,
    val totalPausedMillis: Long = 0L,
    val isLoading: Boolean = true
) {
    val currentItem: BlockItem? get() = items.getOrNull(currentIndex)
    val isLastItem: Boolean get() = currentIndex >= items.size - 1
    val itemCount: Int get() = items.size
}

sealed class BlockRunEvent {
    data object PlayDing : BlockRunEvent()
    data object RunCompleted : BlockRunEvent()
}

@HiltViewModel
class BlockRunViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val folderRepository: FolderRepository,
    private val categoryRepository: CategoryRepository,
    private val questionRepository: QuestionRepository,
    private val logEntryRepository: LogEntryRepository,
    private val answerRepository: AnswerRepository
) : ViewModel() {

    private val folderId: Long = savedStateHandle["folderId"]!!

    private val _state = MutableStateFlow(BlockRunUiState())
    val state: StateFlow<BlockRunUiState> = _state.asStateFlow()

    private val _events = Channel<BlockRunEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var timerJob: Job? = null
    private var openLogEntryId: Long? = null

    init {
        viewModelScope.launch {
            val folder = folderRepository.getById(folderId) ?: return@launch
            val categories = categoryRepository.getCategoriesInFolderOrdered(folderId)
            val questions = questionRepository.getQuestionsInFolderOrdered(folderId)

            val items = mutableListOf<BlockItem>()
            categories.forEach { items.add(BlockItem.CategoryBlockItem(it)) }
            questions.forEach { items.add(BlockItem.QuestionBlockItem(it)) }
            items.sortBy { it.folderSortOrder }

            val isTimed = folder.audioEnabled || folder.autoNextEnabled

            _state.value = BlockRunUiState(
                folderName = folder.name,
                items = items,
                isTimed = isTimed,
                audioEnabled = folder.audioEnabled,
                autoNextEnabled = folder.autoNextEnabled,
                restDurationSeconds = folder.restDurationSeconds,
                isLoading = false
            )

            if (items.isNotEmpty()) {
                startItem()
            } else {
                _state.value = _state.value.copy(phase = BlockRunPhase.COMPLETED)
            }
        }
    }

    fun logNow() {
        val current = _state.value.currentItem ?: return
        if (current !is BlockItem.CategoryBlockItem) return
        viewModelScope.launch {
            // Auto-close any open entry first
            if (openLogEntryId != null) {
                val open = logEntryRepository.getOpenEntry(current.category.id).first()
                if (open != null) {
                    logEntryRepository.stopEntry(open, System.currentTimeMillis())
                }
                openLogEntryId = null
            }
            val now = System.currentTimeMillis()
            logEntryRepository.insertInstant(current.category.id, now)
            cancelTimer()
            advanceToNext()
        }
    }

    fun submitAnswer(value: String) {
        val current = _state.value.currentItem ?: return
        if (current !is BlockItem.QuestionBlockItem) return
        viewModelScope.launch {
            answerRepository.insert(current.question.id, value)
            cancelTimer()
            advanceToNext()
        }
    }

    fun logStart() {
        val current = _state.value.currentItem ?: return
        if (current !is BlockItem.CategoryBlockItem) return
        if (_state.value.hasOpenLogEntry) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            openLogEntryId = logEntryRepository.insertStart(current.category.id, now)
            _state.value = _state.value.copy(hasOpenLogEntry = true, logStartTimeMillis = now)
        }
    }

    fun logStop() {
        val current = _state.value.currentItem ?: return
        if (current !is BlockItem.CategoryBlockItem) return
        if (!_state.value.hasOpenLogEntry) return
        viewModelScope.launch {
            val open = logEntryRepository.getOpenEntry(current.category.id).first()
                ?: return@launch
            val now = System.currentTimeMillis()
            logEntryRepository.stopEntry(open, now)
            openLogEntryId = null
            _state.value = _state.value.copy(hasOpenLogEntry = false)
            advanceToNext()
        }
    }

    fun pauseTimer() {
        if (_state.value.timerState != TimerState.RUNNING) return
        timerJob?.cancel()
        timerJob = null
        _state.value = _state.value.copy(
            timerState = TimerState.PAUSED,
            pausedAtMillis = System.currentTimeMillis()
        )
    }

    fun resumeTimer() {
        if (_state.value.timerState != TimerState.PAUSED) return
        val s = _state.value
        val pauseDuration = if (s.pausedAtMillis > 0) System.currentTimeMillis() - s.pausedAtMillis else 0L
        _state.value = s.copy(
            totalPausedMillis = s.totalPausedMillis + pauseDuration,
            pausedAtMillis = 0L
        )
        startCountdown(_state.value.remainingSeconds)
    }

    fun skipRest() {
        if (_state.value.phase != BlockRunPhase.SHOWING_REST) return
        cancelTimer()
        moveToNextItem()
    }

    private fun startItem() {
        val s = _state.value
        val item = s.items.getOrNull(s.currentIndex) ?: return
        openLogEntryId = null
        _state.value = s.copy(
            phase = BlockRunPhase.SHOWING_ITEM,
            hasOpenLogEntry = false,
            logStartTimeMillis = 0L,
            pausedAtMillis = 0L,
            totalPausedMillis = 0L,
            timerState = TimerState.IDLE,
            remainingSeconds = 0,
            totalSeconds = 0
        )
        if (s.isTimed) {
            val duration = item.desiredDurationSeconds ?: 0
            if (duration > 0) {
                _state.value = _state.value.copy(
                    totalSeconds = duration,
                    remainingSeconds = duration
                )
                startCountdown(duration)
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        timerJob?.cancel()
        _state.value = _state.value.copy(
            timerState = TimerState.RUNNING,
            remainingSeconds = seconds
        )
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _state.value = _state.value.copy(remainingSeconds = remaining)
            }
            onTimerComplete()
        }
    }

    private fun onTimerComplete() {
        val s = _state.value
        _state.value = s.copy(timerState = TimerState.IDLE)
        viewModelScope.launch {
            if (s.audioEnabled) {
                _events.send(BlockRunEvent.PlayDing)
            }
        }
        if (s.phase == BlockRunPhase.SHOWING_REST) {
            moveToNextItem()
        } else if (s.phase == BlockRunPhase.SHOWING_ITEM) {
            // Auto-advance if autoNext is on
            if (s.autoNextEnabled) {
                // For categories, create an instant log if no entry was made
                val current = s.currentItem
                if (current is BlockItem.CategoryBlockItem && !s.hasOpenLogEntry) {
                    viewModelScope.launch {
                        logEntryRepository.insertInstant(
                            current.category.id,
                            System.currentTimeMillis()
                        )
                        advanceToNext()
                    }
                } else {
                    advanceToNext()
                }
            }
            // If autoNext is off but audio is on, user still needs to manually advance
        }
    }

    private fun advanceToNext() {
        val s = _state.value
        val restDuration = s.restDurationSeconds
        if (!s.isLastItem && restDuration != null && restDuration > 0) {
            // Show rest step before next item
            _state.value = s.copy(
                phase = BlockRunPhase.SHOWING_REST,
                timerState = TimerState.IDLE,
                totalSeconds = restDuration,
                remainingSeconds = restDuration,
                hasOpenLogEntry = false
            )
            startCountdown(restDuration)
        } else {
            moveToNextItem()
        }
    }

    private fun moveToNextItem() {
        val s = _state.value
        if (s.isLastItem) {
            cancelTimer()
            _state.value = s.copy(phase = BlockRunPhase.COMPLETED, timerState = TimerState.IDLE)
            viewModelScope.launch { _events.send(BlockRunEvent.RunCompleted) }
        } else {
            _state.value = s.copy(currentIndex = s.currentIndex + 1)
            startItem()
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelTimer()
    }
}
