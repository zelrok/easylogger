package com.easylogger.app.data.repository

import com.easylogger.app.data.local.dao.QuestionDao
import com.easylogger.app.data.local.entity.Question
import com.easylogger.app.data.local.entity.QuestionWithLastAnswer
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

class QuestionRepositoryTest {

    private lateinit var fakeDao: FakeQuestionDao
    private lateinit var repository: QuestionRepository

    @Before
    fun setup() {
        fakeDao = FakeQuestionDao()
        repository = QuestionRepository(fakeDao)
    }

    @Test
    fun `insert text question sets fields`() = runTest {
        val id = repository.insert("How do you feel?", "TEXT", "good,ok,bad")
        val inserted = fakeDao.questions.first { it.id == id }
        assertEquals("How do you feel?", inserted.name)
        assertEquals("TEXT", inserted.answerType)
        assertEquals("good,ok,bad", inserted.textOptions)
        assertTrue(inserted.createdAt > 0)
    }

    @Test
    fun `insert scale question sets min and max`() = runTest {
        val id = repository.insert("Rate your mood", "SCALE", null, 1, 10)
        val inserted = fakeDao.questions.first { it.id == id }
        assertEquals("SCALE", inserted.answerType)
        assertEquals(1, inserted.scaleMin)
        assertEquals(10, inserted.scaleMax)
        assertNull(inserted.textOptions)
    }

    @Test
    fun `insert into folder sets folderId`() = runTest {
        val id = repository.insert("Question", "TEXT", "yes,no", folderId = 5L)
        val inserted = fakeDao.questions.first { it.id == id }
        assertEquals(5L, inserted.folderId)
    }

    @Test
    fun `getTopLevelWithLastAnswer returns only top-level`() = runTest {
        fakeDao.insertDirect(Question(1, "Q1", "TEXT", "yes,no", 1, 5, 0, 100L, null, 0))
        fakeDao.insertDirect(Question(2, "Q2", "SCALE", null, 1, 5, 0, 200L, 5L, 0))
        val result = repository.getTopLevelWithLastAnswer().first()
        assertEquals(1, result.size)
        assertEquals("Q1", result[0].name)
    }

    @Test
    fun `moveQuestionToFolder sets folderId`() = runTest {
        fakeDao.insertDirect(Question(1, "Q1", "TEXT", "yes,no", 1, 5, 0, 100L))
        repository.moveQuestionToFolder(1L, 5L)
        val q = fakeDao.questions.first { it.id == 1L }
        assertEquals(5L, q.folderId)
    }

    @Test
    fun `removeQuestionFromFolder clears folderId`() = runTest {
        fakeDao.insertDirect(Question(1, "Q1", "TEXT", "yes,no", 1, 5, 0, 100L, 5L, 2))
        repository.removeQuestionFromFolder(1L)
        val q = fakeDao.questions.first { it.id == 1L }
        assertNull(q.folderId)
        assertEquals(0, q.folderSortOrder)
    }

    @Test
    fun `delete removes question`() = runTest {
        fakeDao.insertDirect(Question(1, "Q1", "TEXT", "yes,no", 1, 5, 0, 100L))
        repository.delete(Question(1, "Q1", "TEXT", "yes,no", 1, 5, 0, 100L))
        assertTrue(fakeDao.questions.isEmpty())
    }

    @Test
    fun `update modifies question`() = runTest {
        fakeDao.insertDirect(Question(1, "Q1", "TEXT", "yes,no", 1, 5, 0, 100L))
        repository.update(Question(1, "Updated Q1", "TEXT", "yes,no,maybe", 1, 5, 0, 100L))
        val q = fakeDao.questions.first { it.id == 1L }
        assertEquals("Updated Q1", q.name)
        assertEquals("yes,no,maybe", q.textOptions)
    }
}

class FakeQuestionDao : QuestionDao {
    val questions = mutableListOf<Question>()
    private val flow = MutableStateFlow<List<Question>>(emptyList())
    private var nextId = 1L

    fun insertDirect(question: Question) {
        questions.add(question)
        flow.value = questions.toList()
    }

    private fun mapToWithLastAnswer(list: List<Question>): List<QuestionWithLastAnswer> =
        list.map {
            QuestionWithLastAnswer(
                it.id, it.name, it.answerType, it.textOptions,
                it.scaleMin, it.scaleMax, it.sortOrder, it.createdAt,
                null, null, it.folderId, it.folderSortOrder, it.desiredDurationSeconds
            )
        }

    override fun getTopLevelWithLastAnswer(): Flow<List<QuestionWithLastAnswer>> =
        flow.map { list -> mapToWithLastAnswer(list.filter { it.folderId == null }) }

    override fun getQuestionsInFolder(folderId: Long): Flow<List<QuestionWithLastAnswer>> =
        flow.map { list ->
            mapToWithLastAnswer(list.filter { it.folderId == folderId }
                .sortedBy { it.folderSortOrder })
        }

    override suspend fun getNextSortOrder(): Int =
        (questions.filter { it.folderId == null }.maxOfOrNull { it.sortOrder } ?: -1) + 1

    override suspend fun getNextFolderSortOrder(folderId: Long): Int =
        (questions.filter { it.folderId == folderId }.maxOfOrNull { it.folderSortOrder } ?: -1) + 1

    override suspend fun moveQuestionToFolder(questionId: Long, folderId: Long, folderSortOrder: Int) {
        val idx = questions.indexOfFirst { it.id == questionId }
        if (idx >= 0) {
            questions[idx] = questions[idx].copy(folderId = folderId, folderSortOrder = folderSortOrder)
            flow.value = questions.toList()
        }
    }

    override suspend fun removeQuestionFromFolder(questionId: Long, newSortOrder: Int) {
        val idx = questions.indexOfFirst { it.id == questionId }
        if (idx >= 0) {
            questions[idx] = questions[idx].copy(folderId = null, folderSortOrder = 0, sortOrder = newSortOrder)
            flow.value = questions.toList()
        }
    }

    override suspend fun getById(id: Long): Question? = questions.find { it.id == id }

    override suspend fun insert(question: Question): Long {
        val id = nextId++
        questions.add(question.copy(id = id))
        flow.value = questions.toList()
        return id
    }

    override suspend fun update(question: Question) {
        val idx = questions.indexOfFirst { it.id == question.id }
        if (idx >= 0) {
            questions[idx] = question
            flow.value = questions.toList()
        }
    }

    override suspend fun updateAll(questions: List<Question>) {
        for (updated in questions) {
            val idx = this.questions.indexOfFirst { it.id == updated.id }
            if (idx >= 0) this.questions[idx] = updated
        }
        flow.value = this.questions.toList()
    }

    override suspend fun getQuestionsInFolderOrdered(folderId: Long): List<Question> =
        questions.filter { it.folderId == folderId }.sortedBy { it.folderSortOrder }

    override suspend fun getAllList(): List<Question> = questions.toList()

    override suspend fun delete(question: Question) {
        questions.removeAll { it.id == question.id }
        flow.value = questions.toList()
    }
}
