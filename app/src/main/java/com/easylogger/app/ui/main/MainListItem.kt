package com.easylogger.app.ui.main

import com.easylogger.app.data.local.entity.CategoryWithLastLog
import com.easylogger.app.data.local.entity.FolderWithCount

sealed interface MainListItem {
    val sortOrder: Int
    val folderSortOrder: Int
    val itemKey: Long

    data class CategoryItem(val data: CategoryWithLastLog) : MainListItem {
        override val sortOrder get() = data.sortOrder
        override val folderSortOrder get() = data.folderSortOrder
        override val itemKey get() = data.id
    }

    data class FolderItem(val data: FolderWithCount) : MainListItem {
        override val sortOrder get() = data.sortOrder
        override val folderSortOrder get() = data.folderSortOrder
        override val itemKey get() = -data.id // negative to avoid key collision with categories
    }
}
