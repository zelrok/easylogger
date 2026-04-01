package com.easylogger.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.easylogger.app.R

@Composable
fun AddEditCategoryDialog(
    initialName: String = "",
    initialDurationSeconds: Int? = null,
    showDuration: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, durationSeconds: Int?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var duration by remember {
        mutableStateOf(initialDurationSeconds?.toString() ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialName.isEmpty()) stringResource(R.string.add_category)
                else stringResource(R.string.edit_category)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 100) name = it },
                    label = { Text(stringResource(R.string.category_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showDuration) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text(stringResource(R.string.duration_seconds_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val dur = if (showDuration) duration.toIntOrNull()?.takeIf { it > 0 } else initialDurationSeconds
                    onSave(name.trim(), dur)
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
