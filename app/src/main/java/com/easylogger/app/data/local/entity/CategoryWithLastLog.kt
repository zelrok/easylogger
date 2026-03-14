package com.easylogger.app.data.local.entity

data class CategoryWithLastLog(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val lastLogTimestamp: Long?
)
