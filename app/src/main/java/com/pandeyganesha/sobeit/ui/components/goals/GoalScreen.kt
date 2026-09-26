package com.pandeyganesha.kaamsutra.ui.components.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pandeyganesha.kaamsutra.MyApp
import com.pandeyganesha.kaamsutra.Screen
import com.pandeyganesha.kaamsutra.data.Goal
import com.pandeyganesha.kaamsutra.data.Status
import com.pandeyganesha.kaamsutra.ui.components.CollapsibleSectionHeader
import com.pandeyganesha.kaamsutra.ui.components.DeleteTaskDialog
import com.pandeyganesha.kaamsutra.ui.components.EmptyState
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun GoalScreen(
    isActive: Boolean,
    registerFabAction: (() -> Unit) -> Unit,
    modifier: Modifier
) {
    // Dependencies
    val db = (LocalContext.current.applicationContext as MyApp).db
    val goalDao = db.goalDao()
    val coroutineScope = rememberCoroutineScope()

    // Data
    val activeGoals by goalDao.getGoals(Status.ACTIVE).collectAsState(initial = emptyList())
    val existingGoals = activeGoals.map { it.name }.toSet()
    val goalsDone = activeGoals.filter { it.completed }
    var goalsNotDone by remember(activeGoals) {
        mutableStateOf(activeGoals.filter { !it.completed })
    }

    // UI state
    var showDialog by remember { mutableStateOf(false) }
    var doneExpanded by remember { mutableStateOf(false) }
    var goalBeingEdited by remember { mutableStateOf<Goal?>(null) }
    var goalBeingDeleted by remember { mutableStateOf<Goal?>(null) }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        goalsNotDone = goalsNotDone.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) registerFabAction { showDialog = true }
    }

    // Actions
    val onCheckedChange: (Boolean, Goal) -> Unit = { checked, goal ->
        coroutineScope.launch {
            goalDao.updateGoal(goal.copy(completed = checked))
        }
    }
    val onEditClicked: (Goal) -> Unit = { goal -> goalBeingEdited = goal }
    val onDeleteClicked: (Goal) -> Unit = { goal -> goalBeingDeleted = goal }
    val onSortOrderUpdate: (List<Goal>) -> Unit = { reorderedList ->
        coroutineScope.launch {
            goalDao.updateGoals(reorderedList)
        }
    }
    val onDragStopped: () -> Unit = {
        val size = goalsNotDone.size
        val reordered = goalsNotDone.mapIndexed { index, goal ->
            goal.copy(sortOrder = size - 1 - index)
        }
        goalsNotDone = reordered
        onSortOrderUpdate(reordered)
    }

    if (activeGoals.isEmpty()) {
        EmptyState(pageName = Screen.TODO, onClick = { showDialog = true })
    } else {
        GoalList(
            goalsNotDone = goalsNotDone,
            goalsDone = goalsDone,
            doneExpanded = doneExpanded,
            onDoneToggle = { doneExpanded = !doneExpanded },
            lazyListState = lazyListState,
            reorderableState = reorderableState,
            onCheckedChange = onCheckedChange,
            onEditClick = onEditClicked,
            onDeleteClick = onDeleteClicked,
            onDragStopped = onDragStopped,
            modifier = modifier
        )
    }

    goalBeingDeleted?.let { goal ->
        DeleteTaskDialog(
            taskName = goal.name.substringBefore('\n'),
            onDismiss = { goalBeingDeleted = null },
            onConfirm = {
                coroutineScope.launch {
                    goalDao.softDeleteGoal(goal.id)
                    goalBeingDeleted = null
                }
            }
        )
    }
    goalBeingEdited?.let { goal ->
        AddGoalDialog(
            goal = goal,
            existingGoalNames = existingGoals,
            onDismiss = {
                goalBeingEdited = null
            },
            onConfirm = { updatedGoal ->
                coroutineScope.launch {
                    goalDao.updateGoal(updatedGoal.copy(name = updatedGoal.name.replaceFirstChar { it.uppercase() }))
                    goalBeingEdited = null
                }
            }
        )
    }
    if (showDialog) {
        AddGoalDialog(
            existingGoalNames = existingGoals,
            onDismiss = { showDialog = false },
            onConfirm = { goal ->
                coroutineScope.launch {
                    goalDao.createGoal(goal.copy(name = goal.name.replaceFirstChar { it.uppercase() }))
                }
                showDialog = false
            })
    }
}

@Composable
private fun GoalList(
    goalsNotDone: List<Goal>,
    goalsDone: List<Goal>,
    doneExpanded: Boolean,
    onDoneToggle: () -> Unit,
    lazyListState: LazyListState,
    reorderableState: ReorderableLazyListState,
    onCheckedChange: (Boolean, Goal) -> Unit,
    onEditClick: (Goal) -> Unit,
    onDeleteClick: (Goal) -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(goalsNotDone, key = { it.id }) { goal ->
            ReorderableItem(reorderableState, key = goal.id) {
                GoalListItem(
                    goal = goal,
                    onCheckedChange = onCheckedChange,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    modifier = Modifier.longPressDraggableHandle(
                        onDragStopped = { onDragStopped() }
                    )
                )
            }
        }
        if (goalsDone.isNotEmpty()) {
            item {
                CollapsibleSectionHeader(
                    title = "Done (${goalsDone.size}) ",
                    expanded = doneExpanded,
                    onToggle = onDoneToggle
                )
            }
            if (doneExpanded) {
                items(goalsDone, key = { it.id }) { goal ->
                    GoalListItem(
                        goal = goal,
                        onCheckedChange = onCheckedChange,
                        onEditClick = onEditClick,
                        onDeleteClick = onDeleteClick
                    )
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(75.dp))
        }
    }
}

@Composable
private fun GoalListItem(
    goal: Goal,
    onCheckedChange: (Boolean, Goal) -> Unit,
    onEditClick: (Goal) -> Unit,
    onDeleteClick: (Goal) -> Unit,
    modifier: Modifier = Modifier
) {
    GoalRow(
        goal = goal,
        isChecked = goal.completed,
        onCheckedChange = { checked -> onCheckedChange(checked, goal) },
        onEditClick = { onEditClick(goal) },
        onDeleteClick = { onDeleteClick(goal) },
        modifier = modifier
    )
}
