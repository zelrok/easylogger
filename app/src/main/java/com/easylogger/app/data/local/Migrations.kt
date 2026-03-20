package com.easylogger.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS folders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
            """
        )
        db.execSQL("ALTER TABLE categories ADD COLUMN folderId INTEGER DEFAULT NULL REFERENCES folders(id) ON DELETE SET NULL")
        db.execSQL("ALTER TABLE categories ADD COLUMN folderSortOrder INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_categories_folderId ON categories(folderId)")
    }
}
