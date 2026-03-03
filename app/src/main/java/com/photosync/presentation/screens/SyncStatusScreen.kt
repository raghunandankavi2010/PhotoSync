package com.photosync.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.photosync.domain.model.SyncItem
import com.photosync.domain.model.SyncStats
import com.photosync.domain.model.SyncStatus
import com.photosync.presentation.components.SyncItemCard
import com.photosync.presentation.intent.SyncIntent
import com.photosync.presentation.viewmodel.SyncStatusViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusScreen(
    viewModel: SyncStatusViewModel,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRestore: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val syncItems by viewModel.syncItems.collectAsState()
    val syncStats by viewModel.syncStats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Photo Sync") },
                actions = {
                    IconButton(onClick = { viewModel.processIntent(SyncIntent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Sync, contentDescription = null) },
                    label = { Text("Status") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                    label = { Text("Gallery") },
                    selected = false,
                    onClick = onNavigateToGallery
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
                    label = { Text("Restore") },
                    selected = false,
                    onClick = onNavigateToRestore
                )
            }
        },
        floatingActionButton = {
            if (syncStats?.failed ?: 0 > 0) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.processIntent(SyncIntent.RetryFailed) },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    text = { Text("Retry Failed") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Stats Card
            syncStats?.let { stats ->
                StatsCard(stats = stats)
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("All") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Pending") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Uploading") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Synced") }
                )
            }

            // Content
            val filteredItems = when (selectedTab) {
                0 -> syncItems
                1 -> syncItems.filter { it.status == SyncStatus.PENDING }
                2 -> syncItems.filter { it.status == SyncStatus.UPLOADING }
                3 -> syncItems.filter { it.status == SyncStatus.SYNCED }
                else -> syncItems
            }

            if (isLoading && syncItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.mediaStoreId }) { item ->
                        SyncItemCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.mediaStoreId) },
                            onRetry = {
                                viewModel.processIntent(SyncIntent.RetryItem(item.mediaStoreId))
                            },
                            onCancel = {
                                viewModel.processIntent(SyncIntent.CancelItem(item.mediaStoreId))
                            }
                        )
                    }
                }
            }
        }

        // Error Snackbar
        errorMessage?.let { message ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(message)
            }
        }
    }
}

@Composable
fun StatsCard(stats: SyncStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sync Statistics",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = stats.totalProgress,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${formatBytes(stats.uploadedBytes)} / ${formatBytes(stats.totalBytes)}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Pending", stats.pending)
                StatItem("Uploading", stats.uploading)
                StatItem("Synced", stats.synced)
                StatItem("Failed", stats.failed)
            }
        }
    }
}

@Composable
fun StatItem(label: String, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
