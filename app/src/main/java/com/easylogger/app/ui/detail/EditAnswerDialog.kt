package com.easylogger.app.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import com.easylogger.app.ui.components.ScaleNumberInput

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditAnswerDialog(
    currentValue: String,
    answerType: String,
    textOptions: String?,
    scaleMin: Int,
    scaleMax: Int,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedValue by remember { mutableStateOf(currentValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit)) },
        text = {
            Column {
                if (answerType == "TEXT" && textOptions != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        textOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { option ->
                            if (option == selectedValue) {
                                Button(onClick = { selectedValue = option }) {
                                    Text(option)
                                }
                            } else {
                                OutlinedButton(onClick = { selectedValue = option }) {
                                    Text(option)
                                }
                            }
                        }
                    }
                } else if (scaleMax - scaleMin + 1 <= 20) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in scaleMin..scaleMax) {
                            val value = i.toString()
                            if (value == selectedValue) {
                                Button(onClick = { selectedValue = value }) {
                                    Text(value)
                                }
                            } else {
                                OutlinedButton(onClick = { selectedValue = value }) {
                                    Text(value)
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    ScaleNumberInput(
                        scaleMin = scaleMin,
                        scaleMax = scaleMax,
                        initialValue = selectedValue,
                        onValueChange = { selectedValue = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedValue) },
                enabled = selectedValue.isNotBlank()
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
