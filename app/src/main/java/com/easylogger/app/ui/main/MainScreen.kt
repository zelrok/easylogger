package com.easylogger.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
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
import com.easylogger.app.data.local.entity.Folder
import com.easylogger.app.data.local.entity.FolderWithCount
import com.easylogger.app.data.local.entity.Question
import com.easylogger.app.data.local.entity.QuestionWithLastAnswer
import com.easylogger.app.data.repository.UserPreferenceRepository
import com.easylogger.app.ui.components.EmptyState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onCategoryClick: (Long) -> Unit,
    onQuestionClick: (Long) -> Unit,
    activity: MainActivity,
    viewModel: CategoryListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddQuestionDialog by remember { mutableStateOf(false) }
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryWithLastLog?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryWithLastLog?>(null) }
    var editingFolder by remember { mutableStateOf<FolderWithCount?>(null) }
    var deletingFolder by remember { mutableStateOf<FolderWithCount?>(null) }
    var editingQuestion by remember { mutableStateOf<QuestionWithLastAnswer?>(null) }
    var deletingQuestion by remember { mutableStateOf<QuestionWithLastAnswer?>(null) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val exportEmptyMsg = stringResource(R.string.export_empty)
    val exportSuccessMsg = stringResource(R.string.export_success)
    val answerExportEmptyMsg = stringResource(R.string.answer_export_empty)
    val answerExportSuccessMsg = stringResource(R.string.answer_export_success)

    // Handle back press when inside a folder
    if (state.isInsideFolder) {
        BackHandler { viewModel.exitFolder() }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainScreenEvent.ExportReady -> {
                    activity.createDocumentLauncher.launch(event.suggestedFilename)
                }
                is MainScreenEvent.ExportEmpty -> {
                    snackbarHostState.showSnackbar(exportEmptyMsg)
                }
                is MainScreenEvent.AnswerExportReady -> {
                    activity.createAnswerDocumentLauncher.launch(event.suggestedFilename)
                }
                is MainScreenEvent.AnswerExportEmpty -> {
                    snackbarHostState.showSnackbar(answerExportEmptyMsg)
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

    LaunchedEffect(Unit) {
        activity.answerExportResult.collect { result ->
            result.onSuccess { (count, filename) ->
                snackbarHostState.showSnackbar(
                    answerExportSuccessMsg.format(count, filename)
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (state.isInsideFolder) {
                        IconButton(onClick = { viewModel.exitFolder() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                title = {
                    if (state.isInsideFolder) {
                        Text(state.currentFolderName ?: "")
                    } else {
                        val context = LocalContext.current
                        val versionName = context.packageManager
                            .getPackageInfo(context.packageName, 0).versionName
                        Text("${stringResource(R.string.app_name)} v$versionName")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddFolderDialog = true }) {
                        Icon(
                            Icons.Default.CreateNewFolder,
                            contentDescription = stringResource(R.string.add_folder)
                        )
                    }
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_answers_csv)) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.requestAnswerExport()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_category)) },
                        onClick = {
                            showFabMenu = false
                            showAddCategoryDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_question)) },
                        onClick = {
                            showFabMenu = false
                            showAddQuestionDialog = true
                        }
                    )
                }
            }
        }
    ) { padding ->
        Crossfade(
            targetState = "${state.viewMode}_${state.folderStack.size}_${state.currentFolderId}",
            modifier = Modifier.padding(padding),
            label = "viewMode"
        ) { targetKey ->
            val isInsideFolder = state.isInsideFolder
            val isListMode = state.viewMode == UserPreferenceRepository.VIEW_MODE_LIST

            if (!isInsideFolder && state.topLevelItems.isEmpty() && !state.isLoading) {
                EmptyState(message = stringResource(R.string.empty_state_message))
            } else if (isInsideFolder && state.folderItems.isEmpty() && !state.isLoading) {
                EmptyState(message = stringResource(R.string.empty_folder_message))
            } else if (isInsideFolder) {
                // Inside a folder — show mixed sub-folders + categories + questions
                if (isListMode) {
                    FolderContentList(
                        items = state.folderItems,
                        dragOverFolderId = state.dragOverFolderId,
                        onCategoryClick = onCategoryClick,
                        onEditCategory = { editingCategory = it },
                        onDeleteCategory = { deletingCategory = it },
                        onRemoveCategoryFromFolder = { viewModel.removeCategoryFromFolder(it.id) },
                        onFolderClick = { viewModel.enterFolder(it.id, it.name) },
                        onEditFolder = { editingFolder = it },
                        onDeleteFolder = { deletingFolder = it },
                        onRemoveFolderFromParent = { viewModel.removeFolderFromParent(it.id) },
                        onQuestionClick = onQuestionClick,
                        onEditQuestion = { editingQuestion = it },
                        onDeleteQuestion = { deletingQuestion = it },
                        onRemoveQuestionFromFolder = { viewModel.removeQuestionFromFolder(it.id) },
                        onReorder = viewModel::onReorder,
                        onReorderConfirmed = viewModel::onReorderConfirmed
                    )
                } else {
                    FolderContentGrid(
                        items = state.folderItems,
                        dragOverFolderId = state.dragOverFolderId,
                        onCategoryClick = onCategoryClick,
                        onEditCategory = { editingCategory = it },
                        onDeleteCategory = { deletingCategory = it },
                        onRemoveCategoryFromFolder = { viewModel.removeCategoryFromFolder(it.id) },
                        onFolderClick = { viewModel.enterFolder(it.id, it.name) },
                        onEditFolder = { editingFolder = it },
                        onDeleteFolder = { deletingFolder = it },
                        onRemoveFolderFromParent = { viewModel.removeFolderFromParent(it.id) },
                        onQuestionClick = onQuestionClick,
                        onEditQuestion = { editingQuestion = it },
                        onDeleteQuestion = { deletingQuestion = it },
                        onRemoveQuestionFromFolder = { viewModel.removeQuestionFromFolder(it.id) },
                        onReorder = viewModel::onReorder,
                        onReorderConfirmed = viewModel::onReorderConfirmed
                    )
                }
            } else {
                // Top level — mixed folders + categories + questions
                if (isListMode) {
                    TopLevelList(
                        items = state.topLevelItems,
                        dragOverFolderId = state.dragOverFolderId,
                        onCategoryClick = onCategoryClick,
                        onEditCategory = { editingCategory = it },
                        onDeleteCategory = { deletingCategory = it },
                        onFolderClick = { viewModel.enterFolder(it.id, it.name) },
                        onEditFolder = { editingFolder = it },
                        onDeleteFolder = { deletingFolder = it },
                        onQuestionClick = onQuestionClick,
                        onEditQuestion = { editingQuestion = it },
                        onDeleteQuestion = { deletingQuestion = it },
                        onReorder = viewModel::onReorder,
                        onReorderConfirmed = viewModel::onReorderConfirmed
                    )
                } else {
                    TopLevelGrid(
                        items = state.topLevelItems,
                        dragOverFolderId = state.dragOverFolderId,
                        onCategoryClick = onCategoryClick,
                        onEditCategory = { editingCategory = it },
                        onDeleteCategory = { deletingCategory = it },
                        onFolderClick = { viewModel.enterFolder(it.id, it.name) },
                        onEditFolder = { editingFolder = it },
                        onDeleteFolder = { deletingFolder = it },
                        onQuestionClick = onQuestionClick,
                        onEditQuestion = { editingQuestion = it },
                        onDeleteQuestion = { deletingQuestion = it },
                        onReorder = viewModel::onReorder,
                        onReorderConfirmed = viewModel::onReorderConfirmed
                    )
                }
            }
        }
    }

    // --- Dialogs ---

    if (showAddCategoryDialog) {
        AddEditCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onSave = { name ->
                viewModel.addCategory(name)
                showAddCategoryDialog = false
            }
        )
    }

    if (showAddQuestionDialog) {
        AddEditQuestionDialog(
            onDismiss = { showAddQuestionDialog = false },
            onSave = { name, answerType, textOptions, scaleMin, scaleMax ->
                viewModel.addQuestion(name, answerType, textOptions, scaleMin, scaleMax)
                showAddQuestionDialog = false
            }
        )
    }

    if (showAddFolderDialog) {
        AddEditFolderDialog(
            onDismiss = { showAddFolderDialog = false },
            onSave = { name ->
                viewModel.addFolder(name)
                showAddFolderDialog = false
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
                        createdAt = category.createdAt,
                        folderId = category.folderId,
                        folderSortOrder = category.folderSortOrder
                    )
                )
                editingCategory = null
            }
        )
    }

    editingFolder?.let { folder ->
        AddEditFolderDialog(
            initialName = folder.name,
            onDismiss = { editingFolder = null },
            onSave = { name ->
                viewModel.updateFolder(
                    Folder(
                        id = folder.id,
                        name = name,
                        sortOrder = folder.sortOrder,
                        createdAt = folder.createdAt,
                        parentFolderId = folder.parentFolderId,
                        folderSortOrder = folder.folderSortOrder
                    )
                )
                editingFolder = null
            }
        )
    }

    editingQuestion?.let { question ->
        AddEditQuestionDialog(
            initialName = question.name,
            initialAnswerType = question.answerType,
            initialTextOptions = question.textOptions ?: "",
            initialScaleMin = question.scaleMin,
            initialScaleMax = question.scaleMax,
            onDismiss = { editingQuestion = null },
            onSave = { name, answerType, textOptions, scaleMin, scaleMax ->
                viewModel.updateQuestion(
                    Question(
                        id = question.id,
                        name = name,
                        answerType = answerType,
                        textOptions = textOptions,
                        scaleMin = scaleMin,
                        scaleMax = scaleMax,
                        sortOrder = question.sortOrder,
                        createdAt = question.createdAt,
                        folderId = question.folderId,
                        folderSortOrder = question.folderSortOrder
                    )
                )
                editingQuestion = null
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

    deletingFolder?.let { folder ->
        DeleteCategoryDialog(
            categoryName = folder.name,
            onDismiss = { deletingFolder = null },
            onConfirm = {
                viewModel.deleteFolder(folder)
                deletingFolder = null
            }
        )
    }

    deletingQuestion?.let { question ->
        DeleteCategoryDialog(
            categoryName = question.name,
            onDismiss = { deletingQuestion = null },
            onConfirm = {
                viewModel.deleteQuestion(question)
                deletingQuestion = null
            }
        )
    }
}

// --- Top-level list composable ---

@Composable
private fun TopLevelList(
    items: List<MainListItem>,
    dragOverFolderId: Long?,
    onCategoryClick: (Long) -> Unit,
    onEditCategory: (CategoryWithLastLog) -> Unit,
    onDeleteCategory: (CategoryWithLastLog) -> Unit,
    onFolderClick: (FolderWithCount) -> Unit,
    onEditFolder: (FolderWithCount) -> Unit,
    onDeleteFolder: (FolderWithCount) -> Unit,
    onQuestionClick: (Long) -> Unit,
    onEditQuestion: (QuestionWithLastAnswer) -> Unit,
    onDeleteQuestion: (QuestionWithLastAnswer) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onReorderConfirmed: () -> Unit
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items, key = { it.itemKey }) { item ->
            ReorderableItem(reorderableState, key = item.itemKey) { isDragging ->
                val elevation by animateDpAsState(
                    if (isDragging) 8.dp else 0.dp,
                    label = "dragElevation"
                )
                when (item) {
                    is MainListItem.CategoryItem -> {
                        CategoryListItem(
                            category = item.data,
                            onClick = { onCategoryClick(item.data.id) },
                            onEdit = { onEditCategory(item.data) },
                            onDelete = { onDeleteCategory(item.data) },
                            modifier = Modifier
                                .shadow(elevation)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                    is MainListItem.FolderItem -> {
                        val isDropTarget = dragOverFolderId == item.data.id
                        FolderListItem(
                            folder = item.data,
                            isDropTarget = isDropTarget,
                            onClick = { onFolderClick(item.data) },
                            onEdit = { onEditFolder(item.data) },
                            onDelete = { onDeleteFolder(item.data) },
                            modifier = Modifier
                                .shadow(elevation)
                                .then(
                                    if (isDropTarget) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                    is MainListItem.QuestionItem -> {
                        QuestionListItem(
                            question = item.data,
                            onClick = { onQuestionClick(item.data.id) },
                            onEdit = { onEditQuestion(item.data) },
                            onDelete = { onDeleteQuestion(item.data) },
                            modifier = Modifier
                                .shadow(elevation)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                }
            }
        }
    }
}

// --- Top-level grid composable ---

@Composable
private fun TopLevelGrid(
    items: List<MainListItem>,
    dragOverFolderId: Long?,
    onCategoryClick: (Long) -> Unit,
    onEditCategory: (CategoryWithLastLog) -> Unit,
    onDeleteCategory: (CategoryWithLastLog) -> Unit,
    onFolderClick: (FolderWithCount) -> Unit,
    onEditFolder: (FolderWithCount) -> Unit,
    onDeleteFolder: (FolderWithCount) -> Unit,
    onQuestionClick: (Long) -> Unit,
    onEditQuestion: (QuestionWithLastAnswer) -> Unit,
    onDeleteQuestion: (QuestionWithLastAnswer) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onReorderConfirmed: () -> Unit
) {
    val lazyGridState = rememberLazyGridState()
    val reorderableGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        state = lazyGridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(items, key = { it.itemKey }) { item ->
            ReorderableItem(reorderableGridState, key = item.itemKey) { isDragging ->
                val elevation by animateDpAsState(
                    if (isDragging) 8.dp else 1.dp,
                    label = "dragElevation"
                )
                when (item) {
                    is MainListItem.CategoryItem -> {
                        CategoryGridCard(
                            category = item.data,
                            onClick = { onCategoryClick(item.data.id) },
                            onEdit = { onEditCategory(item.data) },
                            onDelete = { onDeleteCategory(item.data) },
                            modifier = Modifier
                                .shadow(elevation, shape = MaterialTheme.shapes.medium)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                    is MainListItem.FolderItem -> {
                        val isDropTarget = dragOverFolderId == item.data.id
                        FolderGridCard(
                            folder = item.data,
                            isDropTarget = isDropTarget,
                            onClick = { onFolderClick(item.data) },
                            onEdit = { onEditFolder(item.data) },
                            onDelete = { onDeleteFolder(item.data) },
                            modifier = Modifier
                                .shadow(elevation, shape = MaterialTheme.shapes.medium)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                    is MainListItem.QuestionItem -> {
                        QuestionGridCard(
                            question = item.data,
                            onClick = { onQuestionClick(item.data.id) },
                            onEdit = { onEditQuestion(item.data) },
                            onDelete = { onDeleteQuestion(item.data) },
                            modifier = Modifier
                                .shadow(elevation, shape = MaterialTheme.shapes.medium)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                }
            }
        }
    }
}

// --- Folder-interior list composable (mixed sub-folders + categories + questions) ---

@Composable
private fun FolderContentList(
    items: List<MainListItem>,
    dragOverFolderId: Long?,
    onCategoryClick: (Long) -> Unit,
    onEditCategory: (CategoryWithLastLog) -> Unit,
    onDeleteCategory: (CategoryWithLastLog) -> Unit,
    onRemoveCategoryFromFolder: (CategoryWithLastLog) -> Unit,
    onFolderClick: (FolderWithCount) -> Unit,
    onEditFolder: (FolderWithCount) -> Unit,
    onDeleteFolder: (FolderWithCount) -> Unit,
    onRemoveFolderFromParent: (FolderWithCount) -> Unit,
    onQuestionClick: (Long) -> Unit,
    onEditQuestion: (QuestionWithLastAnswer) -> Unit,
    onDeleteQuestion: (QuestionWithLastAnswer) -> Unit,
    onRemoveQuestionFromFolder: (QuestionWithLastAnswer) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onReorderConfirmed: () -> Unit
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items, key = { it.itemKey }) { item ->
            ReorderableItem(reorderableState, key = item.itemKey) { isDragging ->
                val elevation by animateDpAsState(
                    if (isDragging) 8.dp else 0.dp,
                    label = "dragElevation"
                )
                when (item) {
                    is MainListItem.CategoryItem -> {
                        CategoryListItem(
                            category = item.data,
                            onClick = { onCategoryClick(item.data.id) },
                            onEdit = { onEditCategory(item.data) },
                            onDelete = { onDeleteCategory(item.data) },
                            onRemoveFromFolder = { onRemoveCategoryFromFolder(item.data) },
                            modifier = Modifier
                                .shadow(elevation)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                    is MainListItem.FolderItem -> {
                        val isDropTarget = dragOverFolderId == item.data.id
                        FolderListItem(
                            folder = item.data,
                            isDropTarget = isDropTarget,
                            onClick = { onFolderClick(item.data) },
                            onEdit = { onEditFolder(item.data) },
                            onDelete = { onDeleteFolder(item.data) },
                            onRemoveFromFolder = { onRemoveFolderFromParent(item.data) },
                            modifier = Modifier
                                .shadow(elevation)
                                .then(
                                    if (isDropTarget) {
                                        Modifier.background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                    is MainListItem.QuestionItem -> {
                        QuestionListItem(
                            question = item.data,
                            onClick = { onQuestionClick(item.data.id) },
                            onEdit = { onEditQuestion(item.data) },
                            onDelete = { onDeleteQuestion(item.data) },
                            onRemoveFromFolder = { onRemoveQuestionFromFolder(item.data) },
                            modifier = Modifier
                                .shadow(elevation)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                }
            }
        }
    }
}

// --- Folder-interior grid composable (mixed sub-folders + categories + questions) ---

@Composable
private fun FolderContentGrid(
    items: List<MainListItem>,
    dragOverFolderId: Long?,
    onCategoryClick: (Long) -> Unit,
    onEditCategory: (CategoryWithLastLog) -> Unit,
    onDeleteCategory: (CategoryWithLastLog) -> Unit,
    onRemoveCategoryFromFolder: (CategoryWithLastLog) -> Unit,
    onFolderClick: (FolderWithCount) -> Unit,
    onEditFolder: (FolderWithCount) -> Unit,
    onDeleteFolder: (FolderWithCount) -> Unit,
    onRemoveFolderFromParent: (FolderWithCount) -> Unit,
    onQuestionClick: (Long) -> Unit,
    onEditQuestion: (QuestionWithLastAnswer) -> Unit,
    onDeleteQuestion: (QuestionWithLastAnswer) -> Unit,
    onRemoveQuestionFromFolder: (QuestionWithLastAnswer) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onReorderConfirmed: () -> Unit
) {
    val lazyGridState = rememberLazyGridState()
    val reorderableGridState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        state = lazyGridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(items, key = { it.itemKey }) { item ->
            ReorderableItem(reorderableGridState, key = item.itemKey) { isDragging ->
                val elevation by animateDpAsState(
                    if (isDragging) 8.dp else 1.dp,
                    label = "dragElevation"
                )
                when (item) {
                    is MainListItem.CategoryItem -> {
                        CategoryGridCard(
                            category = item.data,
                            onClick = { onCategoryClick(item.data.id) },
                            onEdit = { onEditCategory(item.data) },
                            onDelete = { onDeleteCategory(item.data) },
                            onRemoveFromFolder = { onRemoveCategoryFromFolder(item.data) },
                            modifier = Modifier
                                .shadow(elevation, shape = MaterialTheme.shapes.medium)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                    is MainListItem.FolderItem -> {
                        val isDropTarget = dragOverFolderId == item.data.id
                        FolderGridCard(
                            folder = item.data,
                            isDropTarget = isDropTarget,
                            onClick = { onFolderClick(item.data) },
                            onEdit = { onEditFolder(item.data) },
                            onDelete = { onDeleteFolder(item.data) },
                            onRemoveFromFolder = { onRemoveFolderFromParent(item.data) },
                            modifier = Modifier
                                .shadow(elevation, shape = MaterialTheme.shapes.medium)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                    is MainListItem.QuestionItem -> {
                        QuestionGridCard(
                            question = item.data,
                            onClick = { onQuestionClick(item.data.id) },
                            onEdit = { onEditQuestion(item.data) },
                            onDelete = { onDeleteQuestion(item.data) },
                            onRemoveFromFolder = { onRemoveQuestionFromFolder(item.data) },
                            modifier = Modifier
                                .shadow(elevation, shape = MaterialTheme.shapes.medium)
                                .longPressDraggableHandle(
                                    onDragStopped = { onReorderConfirmed() }
                                )
                        )
                    }
                }
            }
        }
    }
}
