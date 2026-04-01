package com.easylogger.app.data.repository

import com.easylogger.app.data.local.dao.QuestionDao
import com.easylogger.app.data.local.entity.Question
import com.easylogger.app.data.local.entity.QuestionWithLastAnswer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val questionDao: QuestionDao
) {
    fun getTopLevelWithLastAnswer(): Flow<List<QuestionWithLastAnswer>> =
        questionDao.getTopLevelWithLastAnswer()

    fun getQuestionsInFolder(folderId: Long): Flow<List<QuestionWithLastAnswer>> =
        questionDao.getQuestionsInFolder(folderId)

    suspend fun getById(id: Long): Question? = questionDao.getById(id)

    suspend fun getQuestionsInFolderOrdered(folderId: Long): List<Question> =
        questionDao.getQuestionsInFolderOrdered(folderId)

    suspend fun insert(
        name: String,
        answerType: String,
        textOptions: String? = null,
        scaleMin: Int = 1,
        scaleMax: Int = 5,
        folderId: Long? = null,
        desiredDurationSeconds: Int? = null
    ): Long {
        val question = if (folderId != null) {
            val nextFolderOrder = questionDao.getNextFolderSortOrder(folderId)
            Question(
                name = name,
                answerType = answerType,
                textOptions = textOptions,
                scaleMin = scaleMin,
                scaleMax = scaleMax,
                sortOrder = 0,
                createdAt = System.currentTimeMillis(),
                folderId = folderId,
                folderSortOrder = nextFolderOrder,
                desiredDurationSeconds = desiredDurationSeconds
            )
        } else {
            val nextOrder = questionDao.getNextSortOrder()
            Question(
                name = name,
                answerType = answerType,
                textOptions = textOptions,
                scaleMin = scaleMin,
                scaleMax = scaleMax,
                sortOrder = nextOrder,
                createdAt = System.currentTimeMillis(),
                desiredDurationSeconds = desiredDurationSeconds
            )
        }
        return questionDao.insert(question)
    }

    suspend fun update(question: Question) = questionDao.update(question)

    suspend fun updateSortOrders(questions: List<Question>) = questionDao.updateAll(questions)

    suspend fun moveQuestionToFolder(questionId: Long, folderId: Long) {
        val nextOrder = questionDao.getNextFolderSortOrder(folderId)
        questionDao.moveQuestionToFolder(questionId, folderId, nextOrder)
    }

    suspend fun removeQuestionFromFolder(questionId: Long) {
        val nextOrder = questionDao.getNextSortOrder()
        questionDao.removeQuestionFromFolder(questionId, nextOrder)
    }

    suspend fun delete(question: Question) = questionDao.delete(question)

    suspend fun getAllList(): List<Question> = questionDao.getAllList()
}
