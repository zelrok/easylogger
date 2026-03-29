package com.easylogger.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "questions",
    indices = [Index(value = ["folderId"])],
    foreignKeys = [
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Question(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val answerType: String,
    val textOptions: String? = null,
    val scaleMin: Int = 1,
    val scaleMax: Int = 5,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val folderId: Long? = null,
    val folderSortOrder: Int = 0
)
