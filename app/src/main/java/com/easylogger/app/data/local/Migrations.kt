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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS log_entries_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                categoryId INTEGER NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                createdAt INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE CASCADE
            )
            """
        )
        db.execSQL(
            """
            INSERT INTO log_entries_new (id, categoryId, startTime, endTime, createdAt)
            SELECT id, categoryId, timestamp, timestamp, createdAt FROM log_entries
            """
        )
        db.execSQL("DROP TABLE log_entries")
        db.execSQL("ALTER TABLE log_entries_new RENAME TO log_entries")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_log_entries_categoryId ON log_entries(categoryId)")
    }
}
