package com.easylogger.app.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
fun AddEditQuestionDialog(
    initialName: String = "",
    initialAnswerType: String = "TEXT",
    initialTextOptions: String = "",
    initialScaleMin: Int = 1,
    initialScaleMax: Int = 5,
    initialDurationSeconds: Int? = null,
    showDuration: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, answerType: String, textOptions: String?, scaleMin: Int, scaleMax: Int, durationSeconds: Int?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var answerType by remember { mutableStateOf(initialAnswerType) }
    var textOptions by remember { mutableStateOf(initialTextOptions) }
    var scaleMin by remember { mutableStateOf(initialScaleMin.toString()) }
    var scaleMax by remember { mutableStateOf(initialScaleMax.toString()) }
    var duration by remember {
        mutableStateOf(initialDurationSeconds?.toString() ?: "")
    }

    val isValid = name.isNotBlank() && when (answerType) {
        "TEXT" -> textOptions.isNotBlank() && textOptions.split(",").any { it.trim().isNotEmpty() }
        "SCALE" -> {
            val min = scaleMin.toIntOrNull()
            val max = scaleMax.toIntOrNull()
            min != null && max != null && min < max
        }
        else -> false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialName.isEmpty()) stringResource(R.string.add_question)
                else stringResource(R.string.edit_question)
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 200) name = it },
                    label = { Text(stringResource(R.string.question_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    FilterChip(
                        selected = answerType == "TEXT",
                        onClick = { answerType = "TEXT" },
                        label = { Text(stringResource(R.string.answer_type_text)) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = answerType == "SCALE",
                        onClick = { answerType = "SCALE" },
                        label = { Text(stringResource(R.string.answer_type_scale)) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (answerType == "TEXT") {
                    OutlinedTextField(
                        value = textOptions,
                        onValueChange = { textOptions = it },
                        label = { Text(stringResource(R.string.text_options_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row {
                        OutlinedTextField(
                            value = scaleMin,
                            onValueChange = { scaleMin = it.filter { c -> c.isDigit() || c == '-' }.take(4) },
                            label = { Text(stringResource(R.string.scale_min_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = scaleMax,
                            onValueChange = { scaleMax = it.filter { c -> c.isDigit() || c == '-' }.take(4) },
                            label = { Text(stringResource(R.string.scale_max_hint)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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
                    onSave(
                        name.trim(),
                        answerType,
                        if (answerType == "TEXT") textOptions.trim() else null,
                        scaleMin.toIntOrNull() ?: 1,
                        scaleMax.toIntOrNull() ?: 5,
                        dur
                    )
                },
                enabled = isValid
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
