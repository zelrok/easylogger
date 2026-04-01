package com.easylogger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.easylogger.app.data.local.entity.Question
import com.easylogger.app.data.local.entity.QuestionWithLastAnswer
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {

    @Query(
        """
        SELECT q.id, q.name, q.answerType, q.textOptions, q.scaleMin, q.scaleMax,
               q.sortOrder, q.createdAt, q.folderId, q.folderSortOrder, q.desiredDurationSeconds,
               (SELECT a.value FROM answers a WHERE a.questionId = q.id ORDER BY a.createdAt DESC LIMIT 1) AS lastAnswerValue,
               (SELECT MAX(a.createdAt) FROM answers a WHERE a.questionId = q.id) AS lastAnswerTimestamp
        FROM questions q
        WHERE q.folderId IS NULL
        ORDER BY q.sortOrder ASC, q.createdAt ASC
        """
    )
    fun getTopLevelWithLastAnswer(): Flow<List<QuestionWithLastAnswer>>

    @Query(
        """
        SELECT q.id, q.name, q.answerType, q.textOptions, q.scaleMin, q.scaleMax,
               q.sortOrder, q.createdAt, q.folderId, q.folderSortOrder, q.desiredDurationSeconds,
               (SELECT a.value FROM answers a WHERE a.questionId = q.id ORDER BY a.createdAt DESC LIMIT 1) AS lastAnswerValue,
               (SELECT MAX(a.createdAt) FROM answers a WHERE a.questionId = q.id) AS lastAnswerTimestamp
        FROM questions q
        WHERE q.folderId = :folderId
        ORDER BY q.folderSortOrder ASC, q.createdAt ASC
        """
    )
    fun getQuestionsInFolder(folderId: Long): Flow<List<QuestionWithLastAnswer>>

    @Query(
        """
        SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM (
            SELECT sortOrder FROM categories WHERE folderId IS NULL
            UNION ALL
            SELECT sortOrder FROM folders WHERE parentFolderId IS NULL
            UNION ALL
            SELECT sortOrder FROM questions WHERE folderId IS NULL
        )
        """
    )
    suspend fun getNextSortOrder(): Int

    @Query(
        """
        SELECT COALESCE(MAX(ord), -1) + 1 FROM (
            SELECT folderSortOrder AS ord FROM categories WHERE folderId = :folderId
            UNION ALL
            SELECT folderSortOrder AS ord FROM folders WHERE parentFolderId = :folderId
            UNION ALL
            SELECT folderSortOrder AS ord FROM questions WHERE folderId = :folderId
        )
        """
    )
    suspend fun getNextFolderSortOrder(folderId: Long): Int

    @Query("UPDATE questions SET folderId = :folderId, folderSortOrder = :folderSortOrder WHERE id = :questionId")
    suspend fun moveQuestionToFolder(questionId: Long, folderId: Long, folderSortOrder: Int)

    @Query("UPDATE questions SET folderId = NULL, folderSortOrder = 0, sortOrder = :newSortOrder WHERE id = :questionId")
    suspend fun removeQuestionFromFolder(questionId: Long, newSortOrder: Int)

    @Query("SELECT * FROM questions WHERE id = :id")
    suspend fun getById(id: Long): Question?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: Question): Long

    @Update
    suspend fun update(question: Question)

    @Query("SELECT * FROM questions ORDER BY name ASC")
    suspend fun getAllList(): List<Question>

    @Update
    suspend fun updateAll(questions: List<Question>)

    @Query("SELECT * FROM questions WHERE folderId = :folderId ORDER BY folderSortOrder ASC")
    suspend fun getQuestionsInFolderOrdered(folderId: Long): List<Question>

    @Delete
    suspend fun delete(question: Question)
}
