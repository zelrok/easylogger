package com.easylogger.app.data.repository

import com.easylogger.app.data.local.dao.FolderDao
import com.easylogger.app.data.local.entity.Folder
import com.easylogger.app.data.local.entity.FolderWithCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FolderRepositoryTest {

    private lateinit var fakeDao: FakeFolderDao
    private lateinit var repository: FolderRepository

    @Before
    fun setup() {
        fakeDao = FakeFolderDao()
        repository = FolderRepository(fakeDao)
    }

    @Test
    fun `insert creates folder with name`() = runTest {
        val id = repository.insert("My Folder")
        val inserted = fakeDao.folders.first { it.id == id }
        assertEquals("My Folder", inserted.name)
        assertTrue(inserted.createdAt > 0)
    }

    @Test
    fun `delete removes folder`() = runTest {
        val id = repository.insert("Temp")
        val folder = fakeDao.folders.first { it.id == id }
        repository.delete(folder)
        assertTrue(fakeDao.folders.isEmpty())
    }

    @Test
    fun `getAllWithCount returns folders`() = runTest {
        repository.insert("Folder A")
        repository.insert("Folder B")
        val result = repository.getAllWithCount().first()
        assertEquals(2, result.size)
    }
}

class FakeFolderDao : FolderDao {
    val folders = mutableListOf<Folder>()
    private val flow = MutableStateFlow<List<Folder>>(emptyList())
    private var nextId = 1L

    override fun getAllWithCount(): Flow<List<FolderWithCount>> =
        flow.map { list ->
            list.map { FolderWithCount(it.id, it.name, it.sortOrder, it.createdAt, 0) }
        }

    override suspend fun getNextTopLevelSortOrder(): Int =
        (folders.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun getById(id: Long): Folder? = folders.find { it.id == id }

    override suspend fun insert(folder: Folder): Long {
        val id = nextId++
        folders.add(folder.copy(id = id))
        flow.value = folders.toList()
        return id
    }

    override suspend fun update(folder: Folder) {
        val idx = folders.indexOfFirst { it.id == folder.id }
        if (idx >= 0) {
            folders[idx] = folder
            flow.value = folders.toList()
        }
    }

    override suspend fun updateAll(folders: List<Folder>) {
        for (updated in folders) {
            val idx = this.folders.indexOfFirst { it.id == updated.id }
            if (idx >= 0) this.folders[idx] = updated
        }
        flow.value = this.folders.toList()
    }

    override suspend fun delete(folder: Folder) {
        folders.removeAll { it.id == folder.id }
        flow.value = folders.toList()
    }
}
