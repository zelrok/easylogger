package com.easylogger.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.easylogger.app.ui.components.ScaleNumberInput
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.easylogger.app.R
import com.easylogger.app.data.local.entity.Answer
import com.easylogger.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuestionDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuestionDetailViewModel = hiltViewModel()
) {
    val question by viewModel.question.collectAsState()
    val pagingItems = viewModel.entries.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    var editingAnswer by remember { mutableStateOf<Answer?>(null) }
    var deletingAnswer by remember { mutableStateOf<Answer?>(null) }

    val answeredAtFormat = stringResource(R.string.answered_at)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is QuestionDetailEvent.AnsweredAt -> {
                    snackbarHostState.showSnackbar(answeredAtFormat.format(event.formattedTime))
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(question?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top half — answer history
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (pagingItems.itemCount == 0) {
                    EmptyState(message = stringResource(R.string.empty_answers_message))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(pagingItems.itemCount) { index ->
                            pagingItems[index]?.let { answer ->
                                AnswerItem(
                                    answer = answer,
                                    onEdit = { editingAnswer = answer },
                                    onDelete = { deletingAnswer = answer }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom half — answer input
            val q = question
            if (q != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (q.answerType == "TEXT" && q.textOptions != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            q.textOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { option ->
                                Button(
                                    onClick = { viewModel.submitAnswer(option) },
                                    modifier = Modifier
                                ) {
                                    Text(option)
                                }
                            }
                        }
                    } else if (q.scaleMax - q.scaleMin + 1 <= 20) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in q.scaleMin..q.scaleMax) {
                                OutlinedButton(
                                    onClick = { viewModel.submitAnswer(i.toString()) }
                                ) {
                                    Text(i.toString())
                                }
                            }
                        }
                    } else {
                        ScaleNumberInput(
                            scaleMin = q.scaleMin,
                            scaleMax = q.scaleMax,
                            onSubmit = { viewModel.submitAnswer(it) }
                        )
                    }
                }
            }
        }
    }

    editingAnswer?.let { answer ->
        val q = question
        if (q != null) {
            EditAnswerDialog(
                currentValue = answer.value,
                answerType = q.answerType,
                textOptions = q.textOptions,
                scaleMin = q.scaleMin,
                scaleMax = q.scaleMax,
                onDismiss = { editingAnswer = null },
                onConfirm = { newValue ->
                    viewModel.updateAnswer(answer, newValue)
                    editingAnswer = null
                }
            )
        }
    }

    deletingAnswer?.let { answer ->
        DeleteEntryDialog(
            onDismiss = { deletingAnswer = null },
            onConfirm = {
                viewModel.deleteAnswer(answer)
                deletingAnswer = null
            }
        )
    }
}
