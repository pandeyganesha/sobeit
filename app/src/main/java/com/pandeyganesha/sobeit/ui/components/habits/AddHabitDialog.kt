package com.pandeyganesha.kaamsutra.ui.components.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandeyganesha.kaamsutra.Screen
import com.pandeyganesha.kaamsutra.data.Habit
import com.pandeyganesha.kaamsutra.ui.components.utils.TaskInputField

enum class RepeatType(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

@Composable
fun AddHabitDialog(
    habit: Habit? = null,
    existingHabitNames: Set<String>,
    currentScreen: Screen,
    onDismiss: () -> Unit,
    onConfirm: (habit: Habit) -> Unit,
    onDeleteClick: (() -> Unit)? = null,
) {
    val existingHabitNamesExcludingItself = existingHabitNames - habit?.name
    var repeatDays by remember { mutableStateOf(habit?.repeatDays?.toString() ?: "") }
    var habitNameField by remember { mutableStateOf(habit?.name ?: "") }
    val isDuplicate = habitNameField in existingHabitNamesExcludingItself
    val nameRequester = remember { FocusRequester() }
    var selected by remember {
        mutableStateOf(habit?.repeatType ?: RepeatType.DAILY)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (habit == null) "Add ${currentScreen.singular}" else "Edit ${currentScreen.singular}")
                if (onDeleteClick != null) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        text = {
            Column {
                TaskInputField(
                    text = habitNameField,
                    onTextChange = { habitNameField = it },
                    focusRequester = nameRequester,
                )
                if (isDuplicate) {
                    Text(
                        text = "${currentScreen.singular} already exists",
                        color = Color.Red
                    )
                }
                Text(
                    text = "Repeat",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    RepeatType.entries.forEach { repeatType ->
                        FilterChip(
                            selected = selected == repeatType,
                            onClick = {
                                selected = repeatType
                            },
                            label = {
                                Text(repeatType.displayName)
                            },
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {

            TextButton(onClick = {
                onConfirm(
                    habit?.copy(
                        name = habitNameField,
                        repeatType = selected,
                        repeatDays = repeatDays.toIntOrNull()
                    ) ?: Habit(
                        name = habitNameField,
                        repeatType = selected,
                        repeatDays = repeatDays.toIntOrNull()
                    )
                )
            },
                enabled = !isDuplicate && habitNameField.substringBefore('\n').isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
