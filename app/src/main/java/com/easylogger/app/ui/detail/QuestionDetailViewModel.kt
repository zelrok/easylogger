package com.easylogger.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.easylogger.app.data.local.entity.Answer
import com.easylogger.app.data.local.entity.Question
import com.easylogger.app.data.repository.AnswerRepository
import com.easylogger.app.data.repository.QuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QuestionDetailEvent {
    data class AnsweredAt(val formattedTime: String) : QuestionDetailEvent()
}

@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val questionRepository: QuestionRepository,
    private val answerRepository: AnswerRepository
) : ViewModel() {

    val questionId: Long = savedStateHandle.get<Long>("questionId") ?: 0L

    private val _events = Channel<QuestionDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _question = MutableStateFlow<Question?>(null)
    val question: StateFlow<Question?> = _question.asStateFlow()

    val entries: Flow<PagingData<Answer>> = Pager(
        config = PagingConfig(pageSize = 50),
        pagingSourceFactory = { answerRepository.getByQuestionId(questionId) }
    ).flow.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            _question.value = questionRepository.getById(questionId)
        }
    }

    fun submitAnswer(value: String) {
        viewModelScope.launch {
            answerRepository.insert(questionId, value)
            val now = System.currentTimeMillis()
            val formatted = java.text.SimpleDateFormat(
                "h:mm a",
                java.util.Locale.getDefault()
            ).format(java.util.Date(now))
            _events.send(QuestionDetailEvent.AnsweredAt(formatted))
        }
    }

    fun updateAnswer(answer: Answer, newValue: String) {
        viewModelScope.launch {
            answerRepository.update(answer.copy(value = newValue))
        }
    }

    fun deleteAnswer(answer: Answer) {
        viewModelScope.launch {
            answerRepository.delete(answer)
        }
    }
}
