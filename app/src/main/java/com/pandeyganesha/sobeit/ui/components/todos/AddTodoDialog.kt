package com.pandeyganesha.kaamsutra.ui.components.todos

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.pandeyganesha.kaamsutra.data.Todo
import androidx.compose.ui.unit.dp
import com.pandeyganesha.kaamsutra.Screen
import com.pandeyganesha.kaamsutra.data.Tag
import com.pandeyganesha.kaamsutra.ui.components.utils.TaskInputField


@Composable
fun AddTodoDialog(
    todo: Todo? = null,
    todoTags: List<Tag> = emptyList(),
    tags: List<Tag> = emptyList(),
    existingTodoNames: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (todo: Todo, selectedTags: List<Tag>) -> Unit,
) {

    var todoNameField by remember { mutableStateOf(todo?.name ?: "") }

    val isDuplicate = todoNameField in existingTodoNames - todo?.name
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var selectedTags by remember { mutableStateOf(todoTags) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${Screen.TODO.singular}")},
        text = {
            Column {
                TaskInputField(
                    text = todoNameField,
                    onTextChange = { todoNameField = it },
                    focusRequester = focusRequester,
                )
                if (tags.isNotEmpty()) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = tag in selectedTags,
                                onClick = {
                                    selectedTags = if (tag in selectedTags)
                                        selectedTags - tag
                                    else
                                        selectedTags + tag
                                },
                                label = { Text(tag.name) },
                                modifier = Modifier
                                    .height(32.dp)
                            )
                        }
                    }
                }
                if (isDuplicate) {
                    Text(
                        text = "${Screen.TODO.singular} already exists",
                        color = Color.Red
                    )
                }

            }
        },
        confirmButton = {

            TextButton(onClick = {
                onConfirm(
                    todo?.copy(name = todoNameField) ?: Todo(name = todoNameField),
                    selectedTags
                )
            },
                enabled = !isDuplicate && todoNameField.substringBefore('\n').isNotBlank()
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