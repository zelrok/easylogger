package com.easylogger.app.data.local.entity

data class FolderWithCount(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val categoryCount: Int
)
