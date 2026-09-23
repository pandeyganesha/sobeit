package com.pandeyganesha.kaamsutra.ui.components.todos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pandeyganesha.kaamsutra.MyApp
import com.pandeyganesha.kaamsutra.Screen
import com.pandeyganesha.kaamsutra.data.Status
import com.pandeyganesha.kaamsutra.data.Tag
import com.pandeyganesha.kaamsutra.data.Todo
import com.pandeyganesha.kaamsutra.data.TodoTag
import com.pandeyganesha.kaamsutra.ui.components.CollapsibleSectionHeader
import com.pandeyganesha.kaamsutra.ui.components.DeleteTaskDialog
import com.pandeyganesha.kaamsutra.ui.components.EmptyState
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun TodoScreen(
    isActive: Boolean,
    registerFabAction: (() -> Unit) -> Unit,
    modifier: Modifier
) {
    // Dependencies
    val db = (LocalContext.current.applicationContext as MyApp).db
    val todoDao = db.todoDao()
    val tagDao = db.tagDao()
    val todoTagDao = db.todoTagDao()
    val coroutineScope = rememberCoroutineScope()
    val viewModel: TodoViewModel = viewModel()

    // Data
    val activeTodos by todoDao.getTodos(Status.ACTIVE).collectAsState(initial = emptyList())
    val tags by tagDao.getTags().collectAsState(initial = emptyList())
    val allTodoTagRows by todoTagDao.getAllTodoTags().collectAsState(initial = emptyList())
    val todoTagsMap = remember(allTodoTagRows) {
        allTodoTagRows.groupBy({ it.todoId }, { it.tag })
    }
    val existingTodos = activeTodos.map { it.name }.toSet()

    // Tag filter + done/not-done split
    val allTag = remember { Tag(id = ALL_TAG_ID, name = "All") }
    val selected = tags.find { it.id == viewModel.selectedTagId } ?: allTag
    val filteredTodos = remember(activeTodos, selected, todoTagsMap) {
        if (selected == allTag) activeTodos
        else activeTodos.filter { todo -> todoTagsMap[todo.id]?.contains(selected) == true }
    }
    var todosNotDone by remember(filteredTodos) {
        mutableStateOf(filteredTodos.filter { !it.completed })
    }
    val todosDone = filteredTodos.filter { it.completed }
    var tagsList by remember(tags) { mutableStateOf(tags) }

    // UI state
    var showDialog by remember { mutableStateOf(false) }
    var showTagInputField by remember { mutableStateOf(false) }
    var doneExpanded by remember { mutableStateOf(false) }
    var todoTagsBeingEdited by remember { mutableStateOf<List<Tag>>(emptyList()) }
    var todoBeingEdited by remember { mutableStateOf<Todo?>(null) }
    var todoBeingDeleted by remember { mutableStateOf<Todo?>(null) }
    val lazyListState = rememberLazyListState()
    val lazyTagState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        todosNotDone = todosNotDone.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }
    val reorderableStateForTags = rememberReorderableLazyListState(lazyTagState) { from, to ->
        tagsList = tagsList.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) registerFabAction { showDialog = true }
    }

    // Actions
    val onEditClicked: (Todo) -> Unit = { todo ->
        coroutineScope.launch {
            todoTagsBeingEdited = todoTagDao.getTagsForTodo(todo.id)
            todoBeingEdited = todo
        }
    }
    val onDeleteClicked: (Todo) -> Unit = { todo -> todoBeingDeleted = todo }
    val onCheckedChange: (Boolean, Todo) -> Unit = { checked, todo ->
        coroutineScope.launch {
            todoDao.updateTodo(todo.copy(completed = checked))
        }
    }
    val onSortOrderUpdate: (List<Todo>) -> Unit = { reorderedList ->
        coroutineScope.launch {
            todoDao.updateTodos(reorderedList)
        }
    }
    val onSortOrderUpdateForTag: (List<Tag>) -> Unit = { reorderedList ->
        coroutineScope.launch {
            tagDao.updateTags(reorderedList)
        }
    }
    val onDragStopped: () -> Unit = {
        val size = todosNotDone.size
        val reordered = todosNotDone.mapIndexed { index, t ->
            t.copy(sortOrder = size - 1 - index)
        }
        todosNotDone = reordered
        onSortOrderUpdate(reordered)
    }

    val onDragTagStopped: () -> Unit = {
        val size = tagsList.size
        val reordered = tagsList.mapIndexed { index, t ->
            t.copy(sortOrder = size - 1 - index)
        }
        tagsList = reordered
        onSortOrderUpdateForTag(reordered)
    }

    if (activeTodos.isEmpty()) {
        EmptyState(pageName = Screen.TODO, onClick = { showDialog = true })
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            TodoFilterRow(
                allTag = allTag,
                tags = tagsList,
                selected = selected,
                onSelect = { viewModel.selectTag(it.id) },
                reorderableState = reorderableStateForTags,
                onAddTagClick = { showTagInputField = true },
                onDragStopped = onDragTagStopped
            )
            TodoList(
                todosNotDone = todosNotDone,
                todosDone = todosDone,
                todoTagsMap = todoTagsMap,
                showTags = selected == allTag,
                doneExpanded = doneExpanded,
                onDoneToggle = { doneExpanded = !doneExpanded },
                lazyListState = lazyListState,
                reorderableState = reorderableState,
                onCheckedChange = onCheckedChange,
                onEditClick = onEditClicked,
                onDeleteClick = onDeleteClicked,
                onDragStopped = onDragStopped,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showTagInputField) {
        EditTagDialog(
            tags = tags,
            onDismiss = { showTagInputField = false },
            onConfirm = { createdTags, deletedTags ->
                coroutineScope.launch {
                    createdTags.forEach { tagDao.createTag(it) }
                    deletedTags.forEach { tagDao.deleteTag(it) }
                }
                showTagInputField = false
            }
        )
    }
    todoBeingDeleted?.let { todo ->
        DeleteTaskDialog(
            taskName = todo.name.substringBefore('\n'),
            onDismiss = { todoBeingDeleted = null },
            onConfirm = {
                coroutineScope.launch {
                    todoDao.softDeleteTodo(todo.id)
                    todoBeingDeleted = null
                }
            }
        )
    }
    todoBeingEdited?.let { todo ->
        AddTodoDialog(
            todo = todo,
            tags = tags,
            todoTags = todoTagsBeingEdited,
            existingTodoNames = existingTodos,
            onDismiss = {
                todoBeingEdited = null
            },
            onConfirm = { updatedTodo, selectedTags ->
                coroutineScope.launch {
                    todoDao.updateTodo(updatedTodo.copy(name = updatedTodo.name.replaceFirstChar { it.uppercase() }))
                    todoTagDao.deleteAllForTodo(updatedTodo.id)
                    selectedTags.forEach { tag ->
                        todoTagDao.insert(TodoTag(todoId = updatedTodo.id, tagId = tag.id))
                    }
                    todoBeingEdited = null
                }
            }
        )
    }
    if (showDialog) {
        AddTodoDialog(
            tags = tags,
            todoTags = if (selected != allTag) listOf(selected) else emptyList(),
            existingTodoNames = existingTodos,
            onDismiss = { showDialog = false },
            onConfirm = { todo, selectedTags ->
                coroutineScope.launch {
                    todoDao.createTodo(todo.copy(name = todo.name.replaceFirstChar { it.uppercase() }))
                    selectedTags.forEach { tag ->
                        todoTagDao.insert(TodoTag(todoId = todo.id, tagId = tag.id))
                    }
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun TodoFilterRow(
    allTag: Tag,
    tags: List<Tag>,
    selected: Tag,
    reorderableState: ReorderableLazyListState,
    onSelect: (Tag) -> Unit,
    onAddTagClick: () -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end=14.dp, top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .nestedScroll(object : NestedScrollConnection {
                    override fun onPostScroll(
                        consumed: Offset,
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset = available
                }),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selected == allTag,
                    onClick = { onSelect(allTag) },
                    label = { Text(allTag.name) },
                )
            }
            items(tags, key = { it.id }) { tag ->
                ReorderableItem(reorderableState, tag.id) {
                    FilterChip(
                        selected = selected == tag,
                        onClick = { onSelect(tag) },
                        label = { Text(tag.name) },
                        modifier = Modifier.longPressDraggableHandle(
                            onDragStopped = { onDragStopped() }
                        )
                    )
                }
            }
        }
        IconButton(
            onClick = onAddTagClick,
            modifier = Modifier
                .size(25.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add tag")
        }
    }
}

@Composable
private fun TodoList(
    todosNotDone: List<Todo>,
    todosDone: List<Todo>,
    todoTagsMap: Map<String, List<Tag>>,
    showTags: Boolean,
    doneExpanded: Boolean,
    onDoneToggle: () -> Unit,
    lazyListState: LazyListState,
    reorderableState: ReorderableLazyListState,
    onCheckedChange: (Boolean, Todo) -> Unit,
    onEditClick: (Todo) -> Unit,
    onDeleteClick: (Todo) -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(todosNotDone, key = { it.id }) { todo ->
            ReorderableItem(reorderableState, key = todo.id) {
                TodoListItem(
                    todo = todo,
                    todoTags = todoTagsMap[todo.id] ?: emptyList(),
                    showTags = showTags,
                    onCheckedChange = onCheckedChange,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    modifier = Modifier.longPressDraggableHandle(
                        onDragStopped = { onDragStopped() }
                    )
                )
            }
        }
        if (todosDone.isNotEmpty()) {
            item {
                CollapsibleSectionHeader(
                    title = "Done (${todosDone.size})",
                    expanded = doneExpanded,
                    onToggle = onDoneToggle
                )
            }
            if (doneExpanded) {
                items(todosDone, key = { it.id }) { todo ->
                    TodoListItem(
                        todo = todo,
                        todoTags = todoTagsMap[todo.id] ?: emptyList(),
                        showTags = showTags,
                        onCheckedChange = onCheckedChange,
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(75.dp))
            }
        }
    }
}

@Composable
private fun TodoListItem(
    todo: Todo,
    todoTags: List<Tag>,
    showTags: Boolean,
    onCheckedChange: (Boolean, Todo) -> Unit,
    onEditClick: (Todo) -> Unit,
    onDeleteClick: (Todo) -> Unit,
    modifier: Modifier = Modifier
) {
    TodoRow(
        todo = todo,
        todoTags = todoTags,
        showTags = showTags,
        isChecked = todo.completed,
        onCheckedChange = { checked -> onCheckedChange(checked, todo) },
        onEditClick = { onEditClick(todo) },
        onDeleteClick = { onDeleteClick(todo) },
        modifier = modifier
    )
}
