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
import org.junit.Assert.assertNull
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
        assertNull(inserted.parentFolderId)
    }

    @Test
    fun `delete removes folder`() = runTest {
        val id = repository.insert("Temp")
        val folder = fakeDao.folders.first { it.id == id }
        repository.delete(folder)
        assertTrue(fakeDao.folders.isEmpty())
    }

    @Test
    fun `getTopLevelWithCount returns top-level folders`() = runTest {
        repository.insert("Folder A")
        repository.insert("Folder B")
        val result = repository.getTopLevelWithCount().first()
        assertEquals(2, result.size)
    }

    @Test
    fun `insertInFolder creates folder with parentFolderId`() = runTest {
        val parentId = repository.insert("Parent")
        val childId = repository.insertInFolder("Child", parentId)
        val child = fakeDao.folders.first { it.id == childId }
        assertEquals("Child", child.name)
        assertEquals(parentId, child.parentFolderId)
    }

    @Test
    fun `getTopLevelWithCount excludes sub-folders`() = runTest {
        val parentId = repository.insert("Parent")
        repository.insertInFolder("Child", parentId)
        val result = repository.getTopLevelWithCount().first()
        assertEquals(1, result.size)
        assertEquals("Parent", result[0].name)
    }

    @Test
    fun `getFoldersInFolder returns sub-folders only`() = runTest {
        val parentId = repository.insert("Parent")
        repository.insertInFolder("Child A", parentId)
        repository.insertInFolder("Child B", parentId)
        repository.insert("Other Top Level")
        val result = repository.getFoldersInFolder(parentId).first()
        assertEquals(2, result.size)
    }

    @Test
    fun `moveFolderToFolder sets parentFolderId`() = runTest {
        val parentId = repository.insert("Parent")
        val childId = repository.insert("Child")
        repository.moveFolderToFolder(childId, parentId)
        val child = fakeDao.folders.first { it.id == childId }
        assertEquals(parentId, child.parentFolderId)
    }

    @Test
    fun `removeFolderFromParent clears parentFolderId`() = runTest {
        val parentId = repository.insert("Parent")
        val childId = repository.insertInFolder("Child", parentId)
        repository.removeFolderFromParent(childId)
        val child = fakeDao.folders.first { it.id == childId }
        assertNull(child.parentFolderId)
    }

    @Test
    fun `delete folder orphans sub-folders to top level`() = runTest {
        val parentId = repository.insert("Parent")
        val childId = repository.insertInFolder("Child", parentId)
        val parent = fakeDao.folders.first { it.id == parentId }
        repository.delete(parent)
        val child = fakeDao.folders.first { it.id == childId }
        assertNull(child.parentFolderId)
    }
}

class FakeFolderDao : FolderDao {
    val folders = mutableListOf<Folder>()
    private val flow = MutableStateFlow<List<Folder>>(emptyList())
    private var nextId = 1L

    private fun toFolderWithCount(folder: Folder): FolderWithCount =
        FolderWithCount(
            folder.id, folder.name, folder.sortOrder, folder.createdAt,
            folder.parentFolderId, folder.folderSortOrder,
            childCount = folders.count { it.parentFolderId == folder.id }
        )

    override fun getTopLevelWithCount(): Flow<List<FolderWithCount>> =
        flow.map { list ->
            list.filter { it.parentFolderId == null }.map { toFolderWithCount(it) }
        }

    override fun getFoldersInFolder(parentFolderId: Long): Flow<List<FolderWithCount>> =
        flow.map { list ->
            list.filter { it.parentFolderId == parentFolderId }
                .sortedBy { it.folderSortOrder }
                .map { toFolderWithCount(it) }
        }

    override suspend fun getNextTopLevelSortOrder(): Int =
        (folders.filter { it.parentFolderId == null }.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun getNextFolderSortOrder(parentFolderId: Long): Int =
        (folders.filter { it.parentFolderId == parentFolderId }.maxOfOrNull { it.folderSortOrder } ?: -1) + 1

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
        // Simulate SET_NULL FK behavior
        folders.forEachIndexed { idx, f ->
            if (f.parentFolderId == folder.id) {
                folders[idx] = f.copy(parentFolderId = null)
            }
        }
        folders.removeAll { it.id == folder.id }
        flow.value = folders.toList()
    }

    override suspend fun moveFolderToFolder(folderId: Long, parentFolderId: Long, folderSortOrder: Int) {
        val idx = folders.indexOfFirst { it.id == folderId }
        if (idx >= 0) {
            folders[idx] = folders[idx].copy(parentFolderId = parentFolderId, folderSortOrder = folderSortOrder)
            flow.value = folders.toList()
        }
    }

    override suspend fun removeFolderFromParent(folderId: Long, newSortOrder: Int) {
        val idx = folders.indexOfFirst { it.id == folderId }
        if (idx >= 0) {
            folders[idx] = folders[idx].copy(parentFolderId = null, folderSortOrder = 0, sortOrder = newSortOrder)
            flow.value = folders.toList()
        }
    }
}
