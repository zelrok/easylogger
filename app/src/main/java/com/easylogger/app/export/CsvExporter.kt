package com.easylogger.app.export

import com.easylogger.app.data.local.dao.CategoryDao
import com.easylogger.app.data.local.dao.LogEntryDao
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    private val logEntryDao: LogEntryDao,
    private val categoryDao: CategoryDao
) {
    suspend fun export(outputStream: OutputStream): Int {
        val entries = logEntryDao.getAll()
        val categories = categoryDao.getAllList()

        val categoryMap = categories.associate { it.id to it.name }
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        val sorted = entries.sortedWith(
            compareBy<com.easylogger.app.data.local.entity.LogEntry> { categoryMap[it.categoryId] ?: "" }
                .thenBy { it.timestamp }
        )

        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            // UTF-8 BOM for Excel compatibility
            outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            writer.write("category,timestamp,created_at\n")
            for (entry in sorted) {
                val categoryName = categoryMap[entry.categoryId] ?: "Unknown"
                val timestamp = isoFormat.format(Date(entry.timestamp))
                val createdAt = isoFormat.format(Date(entry.createdAt))
                writer.write("${escapeCsv(categoryName)},${timestamp},${createdAt}\n")
            }
        }

        return entries.size
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
