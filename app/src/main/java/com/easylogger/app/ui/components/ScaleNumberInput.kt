package com.easylogger.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * Number input field for scale questions with a large range (>20 values).
 *
 * Two modes:
 * - Submit mode (onSubmit): shows a text field + Submit button. Used when the answer
 *   is submitted immediately (QuestionDetailScreen, BlockRunScreen).
 * - Inline mode (onValueChange): shows only a text field. Used inside dialogs where
 *   the parent handles confirmation (EditAnswerDialog).
 */
@Composable
fun ScaleNumberInput(
    scaleMin: Int,
    scaleMax: Int,
    onSubmit: ((String) -> Unit)? = null,
    onValueChange: ((String) -> Unit)? = null,
    initialValue: String = "",
    buttonShape: Shape = RoundedCornerShape(8.dp)
) {
    var text by remember { mutableStateOf(initialValue) }
    val number = text.toIntOrNull()
    val isValid = number != null && number in scaleMin..scaleMax

    val submit = {
        if (isValid && onSubmit != null) {
            onSubmit(text)
            text = ""
        }
    }

    if (onSubmit != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { value ->
                    if (value.all { it.isDigit() || it == '-' }) {
                        text = value
                    }
                },
                label = { Text("$scaleMin – $scaleMax") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = { submit() },
                enabled = isValid,
                shape = buttonShape,
                modifier = Modifier.heightIn(min = 56.dp)
            ) {
                Text("Submit", style = MaterialTheme.typography.titleMedium)
            }
        }
    } else {
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                if (value.all { it.isDigit() || it == '-' }) {
                    text = value
                    onValueChange?.invoke(value)
                }
            },
            label = { Text("$scaleMin – $scaleMax") },
            singleLine = true,
            isError = text.isNotEmpty() && !isValid,
            supportingText = if (text.isNotEmpty() && !isValid) {
                { Text("Must be between $scaleMin and $scaleMax") }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
