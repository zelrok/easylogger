package com.easylogger.app.data.local.entity

data class CategoryWithLastLog(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val lastLogTimestamp: Long?,
    val folderId: Long? = null,
    val folderSortOrder: Int = 0,
    val desiredDurationSeconds: Int? = null
)
