package com.photosync.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.photosync.domain.model.SyncItem
import com.photosync.domain.model.SyncStatus
import com.photosync.presentation.intent.SyncDetailIntent
import com.photosync.presentation.state.UiState
import com.photosync.presentation.viewmodel.SyncDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDetailScreen(
    viewModel: SyncDetailViewModel,
    mediaStoreId: Long,
    onNavigateBack: () -> Unit
) {
    val syncItemState by viewModel.syncItem.collectAsState()

    LaunchedEffect(mediaStoreId) {
        viewModel.processIntent(SyncDetailIntent.Load)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    when (val state = syncItemState) {
                        is UiState.Success -> {
                            val item = state.data
                            if (item.status == SyncStatus.FAILED) {
                                IconButton(onClick = {
                                    viewModel.processIntent(SyncDetailIntent.Retry)
                                }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                                }
                            }
                            IconButton(onClick = {
                                viewModel.processIntent(SyncDetailIntent.Share)
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                viewModel.processIntent(SyncDetailIntent.Delete)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        else -> {}
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = syncItemState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                is UiState.Success -> {
                    SyncDetailContent(
                        item = state.data,
                        onRetry = { viewModel.processIntent(SyncDetailIntent.Retry) },
                        onCancel = { viewModel.processIntent(SyncDetailIntent.Cancel) }
                    )
                }
            }
        }
    }
}

@Composable
fun SyncDetailContent(
    item: SyncItem,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Image preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AsyncImage(
                model = item.localUri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Details
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                DetailRow("Status", item.status.name)
                DetailRow("Size", formatBytes(item.sizeBytes))
                DetailRow("Uploaded", formatBytes(item.uploadedBytes))
                DetailRow("MIME Type", item.mimeType)
                DetailRow("Date Added", formatTimestamp(item.dateAdded))
                DetailRow("Checksum", item.checksum.take(16) + "...")

                if (item.serverFileId != null) {
                    DetailRow("Server ID", item.serverFileId)
                }

                if (item.retryCount > 0) {
                    DetailRow("Retry Count", item.retryCount.toString())
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress
        if (item.status == SyncStatus.UPLOADING) {
            LinearProgressIndicator(
                progress = item.progress,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(item.progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel Upload")
            }
        }

        // Actions
        if (item.status == SyncStatus.FAILED) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry Upload")
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
