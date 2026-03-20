package com.easylogger.app.ui.main

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.easylogger.app.MainActivity
import com.easylogger.app.R
import com.easylogger.app.data.local.entity.Category
import com.easylogger.app.data.local.entity.CategoryWithLastLog
import com.easylogger.app.data.repository.UserPreferenceRepository
import com.easylogger.app.ui.components.EmptyState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onCategoryClick: (Long) -> Unit,
    activity: MainActivity,
    viewModel: CategoryListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryWithLastLog?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryWithLastLog?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val exportEmptyMsg = stringResource(R.string.export_empty)
    val exportSuccessMsg = stringResource(R.string.export_success)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainScreenEvent.ExportReady -> {
                    activity.createDocumentLauncher.launch(event.suggestedFilename)
                }
                is MainScreenEvent.ExportEmpty -> {
                    snackbarHostState.showSnackbar(exportEmptyMsg)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        activity.exportResult.collect { result ->
            result.onSuccess { (count, filename) ->
                snackbarHostState.showSnackbar(
                    exportSuccessMsg.format(count, filename)
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (state.viewMode == UserPreferenceRepository.VIEW_MODE_LIST) {
                                Icons.Default.GridView
                            } else {
                                Icons.AutoMirrored.Filled.ViewList
                            },
                            contentDescription = if (state.viewMode == UserPreferenceRepository.VIEW_MODE_LIST) {
                                stringResource(R.string.switch_to_grid)
                            } else {
                                stringResource(R.string.switch_to_list)
                            }
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.more_options)
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_csv)) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.requestExport()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
            }
        }
    ) { padding ->
        Crossfade(
            targetState = state.viewMode,
            modifier = Modifier.padding(padding),
            label = "viewMode"
        ) { viewMode ->
            if (state.categories.isEmpty() && !state.isLoading) {
                EmptyState(message = stringResource(R.string.empty_state_message))
            } else if (viewMode == UserPreferenceRepository.VIEW_MODE_LIST) {
                val lazyListState = rememberLazyListState()
                val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    viewModel.onReorder(from.index, to.index)
                }

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        ReorderableItem(reorderableState, key = category.id) { isDragging ->
                            val elevation by animateDpAsState(
                                if (isDragging) 8.dp else 0.dp,
                                label = "dragElevation"
                            )
                            CategoryListItem(
                                category = category,
                                onClick = { onCategoryClick(category.id) },
                                onEdit = { editingCategory = category },
                                onDelete = { deletingCategory = category },
                                modifier = Modifier
                                    .shadow(elevation)
                                    .longPressDraggableHandle(
                                        onDragStopped = { viewModel.onReorderConfirmed() }
                                    )
                            )
                        }
                    }
                }
            } else {
                val lazyGridState = rememberLazyGridState()
                val reorderableGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
                    viewModel.onReorder(from.index, to.index)
                }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    state = lazyGridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        ReorderableItem(reorderableGridState, key = category.id) { isDragging ->
                            val elevation by animateDpAsState(
                                if (isDragging) 8.dp else 1.dp,
                                label = "dragElevation"
                            )
                            CategoryGridCard(
                                category = category,
                                onClick = { onCategoryClick(category.id) },
                                onEdit = { editingCategory = category },
                                onDelete = { deletingCategory = category },
                                modifier = Modifier
                                    .shadow(elevation, shape = MaterialTheme.shapes.medium)
                                    .longPressDraggableHandle(
                                        onDragStopped = { viewModel.onReorderConfirmed() }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditCategoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name ->
                viewModel.addCategory(name)
                showAddDialog = false
            }
        )
    }

    editingCategory?.let { category ->
        AddEditCategoryDialog(
            initialName = category.name,
            onDismiss = { editingCategory = null },
            onSave = { name ->
                viewModel.updateCategory(
                    Category(
                        id = category.id,
                        name = name,
                        sortOrder = category.sortOrder,
                        createdAt = category.createdAt
                    )
                )
                editingCategory = null
            }
        )
    }

    deletingCategory?.let { category ->
        DeleteCategoryDialog(
            categoryName = category.name,
            onDismiss = { deletingCategory = null },
            onConfirm = {
                viewModel.deleteCategory(category)
                deletingCategory = null
            }
        )
    }
}
