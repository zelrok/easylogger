package com.easylogger.app.ui.detail

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.easylogger.app.R
import java.util.Calendar
import java.util.TimeZone

private enum class PickerStep { START_DATE, START_TIME, END_DATE, END_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initialStartTime: Long? = null,
    initialEndTime: Long? = null,
    onDismiss: () -> Unit,
    onConfirm: (startTime: Long, endTime: Long) -> Unit
) {
    val startCalendar = remember {
        Calendar.getInstance().apply {
            if (initialStartTime != null) timeInMillis = initialStartTime
        }
    }
    val endCalendar = remember {
        Calendar.getInstance().apply {
            if (initialEndTime != null) timeInMillis = initialEndTime
            else if (initialStartTime != null) timeInMillis = initialStartTime
        }
    }

    val startDateMillisUtc = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(
                startCalendar.get(Calendar.YEAR),
                startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endDateMillisUtc = remember {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(
                endCalendar.get(Calendar.YEAR),
                endCalendar.get(Calendar.MONTH),
                endCalendar.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var stepOrdinal by remember { mutableIntStateOf(0) }
    val step = PickerStep.entries[stepOrdinal]

    val startDateState = rememberDatePickerState(initialSelectedDateMillis = startDateMillisUtc)
    val startTimeState = rememberTimePickerState(
        initialHour = startCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = startCalendar.get(Calendar.MINUTE)
    )
    val endDateState = rememberDatePickerState(initialSelectedDateMillis = endDateMillisUtc)
    val endTimeState = rememberTimePickerState(
        initialHour = endCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = endCalendar.get(Calendar.MINUTE)
    )

    fun buildTimestamp(
        dateMillisUtc: Long?,
        hour: Int,
        minute: Int
    ): Long {
        val utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = dateMillisUtc ?: System.currentTimeMillis()
        }
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, utcDate.get(Calendar.YEAR))
            set(Calendar.MONTH, utcDate.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utcDate.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun confirmBoth() {
        val start = buildTimestamp(
            startDateState.selectedDateMillis,
            startTimeState.hour,
            startTimeState.minute
        )
        val end = buildTimestamp(
            endDateState.selectedDateMillis,
            endTimeState.hour,
            endTimeState.minute
        )
        onConfirm(start, end)
    }

    when (step) {
        PickerStep.START_DATE -> {
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = { stepOrdinal = PickerStep.START_TIME.ordinal }) {
                        Text(stringResource(R.string.select_start_time))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                DatePicker(state = startDateState)
            }
        }
        PickerStep.START_TIME -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.select_start_time)) },
                text = { TimePicker(state = startTimeState) },
                confirmButton = {
                    TextButton(onClick = { stepOrdinal = PickerStep.END_DATE.ordinal }) {
                        Text(stringResource(R.string.select_end_date))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        PickerStep.END_DATE -> {
            DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = { stepOrdinal = PickerStep.END_TIME.ordinal }) {
                        Text(stringResource(R.string.select_end_time))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        // "Same as start" shortcut — copy start values to end and confirm
                        confirmBoth()
                    }) {
                        Text(stringResource(R.string.same_as_start))
                    }
                }
            ) {
                DatePicker(state = endDateState)
            }
        }
        PickerStep.END_TIME -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(stringResource(R.string.select_end_time)) },
                text = { TimePicker(state = endTimeState) },
                confirmButton = {
                    TextButton(onClick = { confirmBoth() }) {
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
    }
}
