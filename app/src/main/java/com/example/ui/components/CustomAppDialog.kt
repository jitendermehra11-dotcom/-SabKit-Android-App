package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun CustomAppDialog(
    showDialog: Boolean,
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = title) },
            text = { Text(text = message) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("आगे बढ़ें")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
