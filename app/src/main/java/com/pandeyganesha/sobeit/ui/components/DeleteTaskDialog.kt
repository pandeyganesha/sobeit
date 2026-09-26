package com.pandeyganesha.kaamsutra.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

@Composable
fun DeleteTaskDialog(
    taskName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete '${taskName.trim()}' ?") },
        confirmButton = {

            TextButton(onClick = { onConfirm()}) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nooooo!")
            }
        }
    )
}