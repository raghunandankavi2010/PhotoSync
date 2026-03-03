package com.photosync.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photosync.domain.model.SyncItem
import com.photosync.domain.model.SyncStatus
import com.photosync.presentation.screens.formatBytes
import com.photosync.presentation.screens.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncItemCard(
    item: SyncItem,
    onClick: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.small
            ) {
                AsyncImage(
                    model = item.localUri,
                    contentDescription = item.displayName,
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusChip(status = item.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatBytes(item.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Progress bar for uploading items
                if (item.status == SyncStatus.UPLOADING) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = item.progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${formatBytes(item.uploadedBytes)} / ${formatBytes(item.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            when (item.status) {
                SyncStatus.FAILED -> {
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                SyncStatus.UPLOADING -> {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel"
                        )
                    }
                }
                else -> {
                    Icon(
                        imageVector = when (item.status) {
                            SyncStatus.SYNCED -> Icons.Default.CloudDone
                            SyncStatus.PENDING -> Icons.Default.Schedule
                            SyncStatus.PAUSED -> Icons.Default.PauseCircle
                            SyncStatus.DUPLICATE -> Icons.Default.ContentCopy
                            else -> Icons.Default.Help
                        },
                        contentDescription = item.status.name,
                        tint = when (item.status) {
                            SyncStatus.SYNCED -> MaterialTheme.colorScheme.primary
                            SyncStatus.DUPLICATE -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: SyncStatus) {
    val (containerColor, contentColor) = when (status) {
        SyncStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        SyncStatus.UPLOADING -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        SyncStatus.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        SyncStatus.SYNCED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        SyncStatus.FAILED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        SyncStatus.DUPLICATE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
