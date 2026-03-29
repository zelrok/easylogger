package com.easylogger.app.data.repository

import androidx.paging.PagingSource
import com.easylogger.app.data.local.dao.AnswerDao
import com.easylogger.app.data.local.entity.Answer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnswerRepositoryTest {

    private lateinit var fakeDao: FakeAnswerDao
    private lateinit var repository: AnswerRepository

    @Before
    fun setup() {
        fakeDao = FakeAnswerDao()
        repository = AnswerRepository(fakeDao)
    }

    @Test
    fun `insert creates answer with value and questionId`() = runTest {
        val id = repository.insert(1L, "yes")
        val inserted = fakeDao.answers.first { it.id == id }
        assertEquals(1L, inserted.questionId)
        assertEquals("yes", inserted.value)
        assertTrue(inserted.createdAt > 0)
    }

    @Test
    fun `insert scale answer stores int as string`() = runTest {
        val id = repository.insert(1L, "3")
        val inserted = fakeDao.answers.first { it.id == id }
        assertEquals("3", inserted.value)
    }

    @Test
    fun `update modifies answer`() = runTest {
        val id = repository.insert(1L, "yes")
        val answer = fakeDao.answers.first { it.id == id }
        repository.update(answer.copy(value = "no"))
        val updated = fakeDao.answers.first { it.id == id }
        assertEquals("no", updated.value)
    }

    @Test
    fun `delete removes answer`() = runTest {
        val id = repository.insert(1L, "yes")
        val answer = fakeDao.answers.first { it.id == id }
        repository.delete(answer)
        assertTrue(fakeDao.answers.isEmpty())
    }

    @Test
    fun `getAll returns all answers`() = runTest {
        repository.insert(1L, "yes")
        repository.insert(1L, "no")
        repository.insert(2L, "3")
        val all = repository.getAll()
        assertEquals(3, all.size)
    }
}

class FakeAnswerDao : AnswerDao {
    val answers = mutableListOf<Answer>()
    private var nextId = 1L

    override fun getByQuestionId(questionId: Long): PagingSource<Int, Answer> {
        throw UnsupportedOperationException("PagingSource not supported in fake")
    }

    override suspend fun getAll(): List<Answer> = answers.toList()

    override suspend fun insert(answer: Answer): Long {
        val id = nextId++
        answers.add(answer.copy(id = id))
        return id
    }

    override suspend fun update(answer: Answer) {
        val idx = answers.indexOfFirst { it.id == answer.id }
        if (idx >= 0) answers[idx] = answer
    }

    override suspend fun delete(answer: Answer) {
        answers.removeAll { it.id == answer.id }
    }
}
