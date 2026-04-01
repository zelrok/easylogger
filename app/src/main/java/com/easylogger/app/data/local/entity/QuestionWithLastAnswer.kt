package com.easylogger.app.data.local.entity

data class QuestionWithLastAnswer(
    val id: Long,
    val name: String,
    val answerType: String,
    val textOptions: String?,
    val scaleMin: Int,
    val scaleMax: Int,
    val sortOrder: Int,
    val createdAt: Long,
    val lastAnswerValue: String?,
    val lastAnswerTimestamp: Long?,
    val folderId: Long? = null,
    val folderSortOrder: Int = 0
)
