package com.pandeyganesha.kaamsutra.ui.components.habits

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pandeyganesha.kaamsutra.MyApp
import com.pandeyganesha.kaamsutra.Screen
import com.pandeyganesha.kaamsutra.data.Habit
import com.pandeyganesha.kaamsutra.data.HabitLog
import com.pandeyganesha.kaamsutra.data.Status
import com.pandeyganesha.kaamsutra.periodStartDateFor
import com.pandeyganesha.kaamsutra.ui.components.DeleteTaskDialog
import com.pandeyganesha.kaamsutra.ui.components.EmptyState
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.DayOfWeek
import java.time.LocalDate

enum class RepeatTypeChip(val displayName: String) {
    ALL("All"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

@Composable
fun HabitScreen(
    isActive: Boolean,
    registerFabAction: (() -> Unit) -> Unit,
    modifier: Modifier
) {
    // Dependencies
    val db = (LocalContext.current.applicationContext as MyApp).db
    val habitDao = db.habitDao()
    val habitLogDao = db.habitLogDao()
    val coroutineScope = rememberCoroutineScope()

    // Data
    val activeHabits by habitDao.getHabits(Status.ACTIVE).collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val relevantPeriodDates = remember(today) {
        listOf(
            today.toString(),
            today.with(DayOfWeek.MONDAY).toString(),
            today.withDayOfMonth(1).toString(),
            today.withDayOfYear(1).toString()
        )
    }
    val allHabitLogsForCurrentPeriods by habitLogDao.getLogsForDates(relevantPeriodDates)
        .collectAsState(initial = emptyList())
    val existingHabits = activeHabits.map { it.name }.toSet()

    val heatMapSince = remember(today) { today.minusDays(364).toString() }
    val habitLogsForHeatMap by habitLogDao.getLogsSince(heatMapSince).collectAsState(initial = emptyList())
    val logsByHabit = remember(habitLogsForHeatMap) {
        habitLogsForHeatMap.groupBy { it.habitId }
    }

    // UI state
    var showDialog by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(RepeatTypeChip.ALL) }
    var habitBeingEdited by remember { mutableStateOf<Habit?>(null) }
    var habitBeingDeleted by remember { mutableStateOf<Habit?>(null) }
    val lazyListState = rememberLazyListState()
    val filteredHabitsForRepeatType = filterHabitsByRepeatType(activeHabits, selected)
    var habitsWithLogs by remember(filteredHabitsForRepeatType, allHabitLogsForCurrentPeriods) {
        mutableStateOf(filteredHabitsForRepeatType)
    }
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        habitsWithLogs = habitsWithLogs.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    LaunchedEffect(isActive) {
        if (isActive) registerFabAction { showDialog = true }
    }

    // Actions
    val onCheckedChange: (Boolean, Habit) -> Unit = { checked, habit ->
        coroutineScope.launch {
            habitLogDao.upsertLog(
                HabitLog(
                    habitId = habit.id,
                    habitDate = periodStartDateFor(habit, LocalDate.now()).toString(),
                    completed = checked
                )
            )
        }
    }
    val onEditClicked: (Habit) -> Unit = { habit -> habitBeingEdited = habit }
    val onDeleteClicked: (Habit) -> Unit = { habit -> habitBeingDeleted = habit }
    val onSortOrderUpdate: (List<Habit>) -> Unit = { reorderedList ->
        coroutineScope.launch {
            habitDao.updateHabits(reorderedList)
        }
    }
    val onDragStopped: () -> Unit = {
        val size = habitsWithLogs.size
        val reordered = habitsWithLogs.mapIndexed { index, h ->
            h.copy(sortOrder = size - 1 - index)
        }
        habitsWithLogs = reordered
        onSortOrderUpdate(reordered)
    }

    if (activeHabits.isEmpty()) {
        EmptyState(pageName = Screen.TODO, onClick = { showDialog = true })
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            RepeatTypeFilterRow(selected = selected, onSelect = { selected = it })
            HabitList(
                habits = habitsWithLogs,
                logs = allHabitLogsForCurrentPeriods,
                logsByHabit = logsByHabit,
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

    habitBeingDeleted?.let { habit ->
        DeleteTaskDialog(
            taskName = habit.name.substringBefore('\n'),
            onDismiss = { habitBeingDeleted = null },
            onConfirm = {
                coroutineScope.launch {
                    habitDao.softDeleteHabit(habit.id)
                    habitBeingDeleted = null
                }
            }
        )
    }
    habitBeingEdited?.let { habit ->
        AddHabitDialog(
            habit = habit,
            existingHabitNames = existingHabits,
            currentScreen = Screen.HABITS,
            onDismiss = {
                habitBeingEdited = null
            },
            onConfirm = { habit ->
                coroutineScope.launch {
                    habitDao.updateHabit(habit.copy(name = habit.name.replaceFirstChar { it.uppercase() }))
                    habitBeingEdited = null
                }
            },
            onDeleteClick = {
                habitBeingEdited = null
                habitBeingDeleted = habit
            }
        )
    }
    if (showDialog) {
        AddHabitDialog(
            existingHabitNames = existingHabits,
            currentScreen = Screen.HABITS,
            onDismiss = { showDialog = false },
            onConfirm = { habit ->
                coroutineScope.launch {
                    habitDao.createHabit(habit.copy(name = habit.name.replaceFirstChar { it.uppercase() }))
                }
                showDialog = false
            })
    }
}

@Composable
private fun RepeatTypeFilterRow(
    selected: RepeatTypeChip,
    onSelect: (RepeatTypeChip) -> Unit,
    modifier: Modifier = Modifier
) {
    val chipsScrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 14.dp)
            .nestedScroll(object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset = available
            })
            .horizontalScroll(chipsScrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RepeatTypeChip.entries.forEach { repeatTypeChip ->
            FilterChip(
                selected = selected == repeatTypeChip,
                onClick = { onSelect(repeatTypeChip) },
                label = { Text(repeatTypeChip.displayName) }
            )
        }
    }
}

@Composable
private fun HabitList(
    habits: List<Habit>,
    logs: List<HabitLog>,
    logsByHabit: Map<String, List<HabitLog>>,
    lazyListState: LazyListState,
    reorderableState: ReorderableLazyListState,
    onCheckedChange: (Boolean, Habit) -> Unit,
    onEditClick: (Habit) -> Unit,
    onDeleteClick: (Habit) -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(state = lazyListState, modifier = modifier) {
        items(habits, key = { it.id }) { habit ->
            ReorderableItem(reorderableState, key = habit.id) {
                HabitRow(
                    habit = habit,
                    habitLogs = logsByHabit[habit.id] ?: emptyList(),
                    isChecked = logs.any {
                        it.habitId == habit.id && it.completed &&
                                it.habitDate == periodStartDateFor(
                            habit,
                            LocalDate.now()
                        ).toString()
                    },
                    onCheckedChange = { checked -> onCheckedChange(checked, habit) },
                    onEditClick = { onEditClick(habit) },
                    onDeleteClick = { onDeleteClick(habit) },
                    modifier = Modifier.longPressDraggableHandle(
                        onDragStopped = { onDragStopped() }
                    )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(75.dp))
        }
    }
}

private fun filterHabitsByRepeatType(
    habits: List<Habit>,
    repeatTypeChip: RepeatTypeChip
): List<Habit> {
    if (repeatTypeChip == RepeatTypeChip.ALL) {
        return habits
    }

    return habits.filter {
        it.repeatType.displayName.equals(
            repeatTypeChip.displayName,
            ignoreCase = true
        )
    }
}
