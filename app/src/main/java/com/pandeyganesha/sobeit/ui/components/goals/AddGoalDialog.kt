package com.pandeyganesha.kaamsutra.ui.components.goals

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import com.pandeyganesha.kaamsutra.data.Goal
import com.pandeyganesha.kaamsutra.ui.components.utils.DatePickerField
import com.pandeyganesha.kaamsutra.ui.components.utils.TaskInputField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import com.pandeyganesha.kaamsutra.Screen

@Composable
fun AddGoalDialog(
    goal: Goal? = null,
    existingGoalNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (goal: Goal) -> Unit,
) {
    val existingGoalNamesExcludingItself = existingGoalNames - goal?.name
    var goalNameField by remember { mutableStateOf(goal?.name ?: "") }
    val isDuplicate = goalNameField in existingGoalNamesExcludingItself
    val focusRequester = remember { FocusRequester() }
    var pickedDate by remember { mutableStateOf(goal?.dueDate) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${Screen.GOALS.singular}")},
        text = {
            Column {
                TaskInputField(
                    text = goalNameField,
                    onTextChange = { goalNameField = it },
                    focusRequester = focusRequester,
                )
                Spacer(modifier = Modifier.height(18.dp))
                DatePickerField(
                    defaultDate = pickedDate,
                    onDateSelected = { date ->
                        pickedDate = date
                    }
                )
                if (isDuplicate) {
                    Text(
                        text = "${Screen.GOALS.singular} already exists",
                        color = Color.Red
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    goal?.copy(
                        name = goalNameField,
                        dueDate = pickedDate
                    ) ?: Goal(
                        name = goalNameField,
                        dueDate = pickedDate
                    )
                )
            },
                enabled = !isDuplicate && goalNameField.substringBefore('\n').isNotBlank()
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
