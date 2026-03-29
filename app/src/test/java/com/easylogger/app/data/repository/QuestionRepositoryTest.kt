package com.easylogger.app.data.repository

import com.easylogger.app.data.local.dao.QuestionDao
import com.easylogger.app.data.local.entity.Question
import com.easylogger.app.data.local.entity.QuestionWithLastAnswer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeQuestionDao : QuestionDao {
    val questions = mutableListOf<Question>()
    private val flow = MutableStateFlow<List<Question>>(emptyList())
    private var nextId = 1L

    private fun mapToWithLastAnswer(list: List<Question>): List<QuestionWithLastAnswer> =
        list.map {
            QuestionWithLastAnswer(
                it.id, it.name, it.answerType, it.textOptions,
                it.scaleMin, it.scaleMax, it.sortOrder, it.createdAt,
                null, null, it.folderId, it.folderSortOrder
            )
        }

    override fun getTopLevelWithLastAnswer(): Flow<List<QuestionWithLastAnswer>> =
        flow.map { list -> mapToWithLastAnswer(list.filter { it.folderId == null }) }

    override fun getQuestionsInFolder(folderId: Long): Flow<List<QuestionWithLastAnswer>> =
        flow.map { list ->
            mapToWithLastAnswer(list.filter { it.folderId == folderId }
                .sortedBy { it.folderSortOrder })
        }

    override suspend fun getNextSortOrder(): Int =
        (questions.filter { it.folderId == null }.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun getNextFolderSortOrder(folderId: Long): Int =
        (questions.filter { it.folderId == folderId }.maxOfOrNull { it.folderSortOrder } ?: -1) + 1

    override suspend fun moveQuestionToFolder(questionId: Long, folderId: Long, folderSortOrder: Int) {
        val idx = questions.indexOfFirst { it.id == questionId }
        if (idx >= 0) {
            questions[idx] = questions[idx].copy(folderId = folderId, folderSortOrder = folderSortOrder)
            flow.value = questions.toList()
        }
    }

    override suspend fun removeQuestionFromFolder(questionId: Long, newSortOrder: Int) {
        val idx = questions.indexOfFirst { it.id == questionId }
        if (idx >= 0) {
            questions[idx] = questions[idx].copy(folderId = null, folderSortOrder = 0, sortOrder = newSortOrder)
            flow.value = questions.toList()
        }
    }

    override suspend fun getById(id: Long): Question? = questions.find { it.id == id }

    override suspend fun insert(question: Question): Long {
        val id = nextId++
        questions.add(question.copy(id = id))
        flow.value = questions.toList()
        return id
    }

    override suspend fun update(question: Question) {
        val idx = questions.indexOfFirst { it.id == question.id }
        if (idx >= 0) {
            questions[idx] = question
            flow.value = questions.toList()
        }
    }

    override suspend fun updateAll(questions: List<Question>) {
        for (updated in questions) {
            val idx = this.questions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) this.questions[idx] = updated
        }
        flow.value = this.questions.toList()
    }

    override suspend fun getAllList(): List<Question> = questions.toList()

    override suspend fun delete(question: Question) {
        questions.removeAll { it.id == question.id }
        flow.value = questions.toList()
    }
}
