package com.easylogger.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = Question::class,
            parentColumns = ["id"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("questionId")]
)
data class Answer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionId: Long,
    val value: String,
    val createdAt: Long = System.currentTimeMillis()
)
