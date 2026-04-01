package com.easylogger.app.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.easylogger.app.R
import com.easylogger.app.data.local.entity.Answer
import com.easylogger.app.ui.main.formatTimestamp

@Composable
fun AnswerItem(
    answer: Answer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = answer.value,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = formatTimestamp(answer.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
