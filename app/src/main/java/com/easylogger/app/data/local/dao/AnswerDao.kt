package com.easylogger.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.easylogger.app.data.local.entity.Answer

@Dao
interface AnswerDao {

    @Query("SELECT * FROM answers WHERE questionId = :questionId ORDER BY createdAt DESC")
    fun getByQuestionId(questionId: Long): PagingSource<Int, Answer>

    @Query("SELECT * FROM answers ORDER BY questionId ASC, createdAt ASC")
    suspend fun getAll(): List<Answer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: Answer): Long

    @Update
    suspend fun update(answer: Answer)

    @Delete
    suspend fun delete(answer: Answer)
}
