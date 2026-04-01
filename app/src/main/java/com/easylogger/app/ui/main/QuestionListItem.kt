package com.easylogger.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.easylogger.app.R
import com.easylogger.app.data.local.entity.QuestionWithLastAnswer

@Composable
fun QuestionListItem(
    question: QuestionWithLastAnswer,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveFromFolder: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    val subtitle = when {
        question.lastAnswerValue != null && question.lastAnswerTimestamp != null ->
            "${question.lastAnswerValue} — ${formatTimestamp(question.lastAnswerTimestamp)}"
        else -> stringResource(R.string.no_answers_yet)
    }

    val typeBadge = if (question.answerType == "SCALE") {
        "${question.scaleMin}-${question.scaleMax}"
    } else {
        question.textOptions?.split(",")?.size?.let { "${it} options" } ?: "Text"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = question.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = typeBadge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onRemoveFromFolder != null) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more_options)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.remove_from_folder)) },
                        onClick = {
                            showMenu = false
                            onRemoveFromFolder()
                        }
                    )
                }
            }
        } else {
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.edit)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete)
                )
            }
        }
    }
}
