package com.example.deliveryapp.ui.common

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@Composable
fun InfoDialog(
    message: String,
    onDismiss: () -> Unit,
    title: String = "Thông báo",
    confirmText: String = "OK"
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(confirmText)
            }
        }
    )
}