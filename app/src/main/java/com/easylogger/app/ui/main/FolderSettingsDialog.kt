package com.easylogger.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.easylogger.app.R

@Composable
fun FolderSettingsDialog(
    initialAudioEnabled: Boolean,
    initialAutoNextEnabled: Boolean,
    initialRestDurationSeconds: Int?,
    onDismiss: () -> Unit,
    onSave: (audioEnabled: Boolean, autoNextEnabled: Boolean, restDurationSeconds: Int?) -> Unit
) {
    var audioEnabled by remember { mutableStateOf(initialAudioEnabled) }
    var autoNextEnabled by remember { mutableStateOf(initialAutoNextEnabled) }
    var restDuration by remember {
        mutableStateOf(initialRestDurationSeconds?.toString() ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_settings)) },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.audio_enabled),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = audioEnabled, onCheckedChange = { audioEnabled = it })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.auto_next_enabled),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = autoNextEnabled, onCheckedChange = { autoNextEnabled = it })
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = restDuration,
                    onValueChange = { restDuration = it.filter { c -> c.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.rest_duration_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val rest = restDuration.toIntOrNull()?.takeIf { it > 0 }
                    onSave(audioEnabled, autoNextEnabled, rest)
                }
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
