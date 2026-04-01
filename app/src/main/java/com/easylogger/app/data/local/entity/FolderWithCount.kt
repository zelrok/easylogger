package com.easylogger.app.data.local.entity

data class FolderWithCount(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val parentFolderId: Long?,
    val folderSortOrder: Int,
    val audioEnabled: Boolean,
    val autoNextEnabled: Boolean,
    val restDurationSeconds: Int?,
    val childCount: Int
)
