package com.easylogger.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.easylogger.app.R
import com.easylogger.app.data.local.entity.LogEntry
import com.easylogger.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogDetailViewModel = hiltViewModel()
) {
    val category by viewModel.category.collectAsState()
    val cooldownActive by viewModel.cooldownActive.collectAsState()
    val openEntry by viewModel.openEntry.collectAsState()
    val pagingItems = viewModel.entries.collectAsLazyPagingItems()
    val snackbarHostState = remember { SnackbarHostState() }
    var showManualPicker by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<LogEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<LogEntry?>(null) }

    val hasOpenWindow = openEntry != null
    val loggedAtFormat = stringResource(R.string.logged_at)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DetailEvent.LoggedAt -> {
                    snackbarHostState.showSnackbar(loggedAtFormat.format(event.formattedTime))
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(category?.name ?: "") },
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
            // Top half — log entries
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (pagingItems.itemCount == 0) {
                    EmptyState(message = stringResource(R.string.empty_detail_message))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(pagingItems.itemCount) { index ->
                            pagingItems[index]?.let { entry ->
                                LogEntryItem(
                                    entry = entry,
                                    onEdit = { editingEntry = entry },
                                    onDelete = { deletingEntry = entry }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom half — action buttons
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.logNow() },
                    enabled = !cooldownActive,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (cooldownActive) stringResource(R.string.cooldown_active)
                        else stringResource(R.string.log_now)
                    )
                }
                Button(
                    onClick = {
                        if (hasOpenWindow) viewModel.logStop() else viewModel.logStart()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (hasOpenWindow) stringResource(R.string.log_stop)
                        else stringResource(R.string.log_start)
                    )
                }
                Button(
                    onClick = { showManualPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.log_manual))
                }
            }
        }
    }

    if (showManualPicker) {
        DateTimePickerDialog(
            onDismiss = { showManualPicker = false },
            onConfirm = { startTime, endTime ->
                viewModel.logManual(startTime, endTime)
                showManualPicker = false
            }
        )
    }

    editingEntry?.let { entry ->
        DateTimePickerDialog(
            initialStartTime = entry.startTime,
            initialEndTime = entry.endTime,
            onDismiss = { editingEntry = null },
            onConfirm = { startTime, endTime ->
                viewModel.updateEntry(entry, startTime, endTime)
                editingEntry = null
            }
        )
    }

    deletingEntry?.let { entry ->
        DeleteEntryDialog(
            onDismiss = { deletingEntry = null },
            onConfirm = {
                viewModel.deleteEntry(entry)
                deletingEntry = null
            }
        )
    }
}
