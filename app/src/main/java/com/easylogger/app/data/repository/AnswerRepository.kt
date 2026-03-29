package com.easylogger.app.data.repository

import androidx.paging.PagingSource
import com.easylogger.app.data.local.dao.AnswerDao
import com.easylogger.app.data.local.entity.Answer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnswerRepository @Inject constructor(
    private val answerDao: AnswerDao
) {
    fun getByQuestionId(questionId: Long): PagingSource<Int, Answer> =
        answerDao.getByQuestionId(questionId)

    suspend fun getAll(): List<Answer> = answerDao.getAll()

    suspend fun insert(questionId: Long, value: String): Long {
        val answer = Answer(
            questionId = questionId,
            value = value,
            createdAt = System.currentTimeMillis()
        )
        return answerDao.insert(answer)
    }

    suspend fun update(answer: Answer) = answerDao.update(answer)

    suspend fun delete(answer: Answer) = answerDao.delete(answer)
}
