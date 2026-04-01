package com.easylogger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.easylogger.app.export.CsvExporter
import com.easylogger.app.ui.navigation.AppNavHost
import com.easylogger.app.ui.theme.EasyLoggerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var csvExporter: CsvExporter

    private val _exportResult = MutableSharedFlow<Result<Pair<Int, String>>>()
    val exportResult = _exportResult.asSharedFlow()

    private val _answerExportResult = MutableSharedFlow<Result<Pair<Int, String>>>()
    val answerExportResult = _answerExportResult.asSharedFlow()

    val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                try {
                    val outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        val count = csvExporter.export(outputStream)
                        val filename = uri.lastPathSegment ?: "export.csv"
                        _exportResult.emit(Result.success(Pair(count, filename)))
                    }
                } catch (e: Exception) {
                    _exportResult.emit(Result.failure(e))
                }
            }
        }
    }

    val createAnswerDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                try {
                    val outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        val count = csvExporter.exportAnswers(outputStream)
                        val filename = uri.lastPathSegment ?: "answers_export.csv"
                        _answerExportResult.emit(Result.success(Pair(count, filename)))
                    }
                } catch (e: Exception) {
                    _answerExportResult.emit(Result.failure(e))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyLoggerTheme {
                AppNavHost(activity = this)
            }
        }
    }
}
