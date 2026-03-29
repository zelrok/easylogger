package com.easylogger.app.data.repository

import androidx.paging.PagingSource
import com.easylogger.app.data.local.dao.AnswerDao
import com.easylogger.app.data.local.entity.Answer

class FakeAnswerDao : AnswerDao {
    val answers = mutableListOf<Answer>()
    private var nextId = 1L

    override fun getByQuestionId(questionId: Long): PagingSource<Int, Answer> {
        throw UnsupportedOperationException("PagingSource not supported in fake")
    }

    override suspend fun getAll(): List<Answer> = answers.toList()

    override suspend fun insert(answer: Answer): Long {
        val id = nextId++
        answers.add(answer.copy(id = id))
        return id
    }

    override suspend fun update(answer: Answer) {
        val idx = answers.indexOfFirst { it.id == answer.id }
        if (idx >= 0) answers[idx] = answer
    }

    override suspend fun delete(answer: Answer) {
        answers.removeAll { it.id == answer.id }
    }
}
