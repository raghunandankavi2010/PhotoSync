package com.photosync.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.photosync.presentation.intent.GalleryIntent
import com.photosync.presentation.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit
) {
    val galleryItems by viewModel.galleryItems.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val isSelectionMode = selectedItems.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text("${selectedItems.size} selected")
                    } else {
                        Text("Gallery")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.processIntent(GalleryIntent.SelectAll) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.processIntent(GalleryIntent.DeselectAll) }) {
                            Icon(Icons.Default.Deselect, contentDescription = "Deselect All")
                        }
                        IconButton(onClick = { viewModel.processIntent(GalleryIntent.SyncSelected) }) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Sync Selected")
                        }
                    } else {
                        IconButton(onClick = { viewModel.processIntent(GalleryIntent.Refresh) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
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
            if (isLoading && galleryItems.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (galleryItems.isEmpty()) {
                Text(
                    text = "No photos found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(galleryItems, key = { it.mediaStoreId }) { item ->
                        GalleryGridItem(
                            item = item,
                            isSelected = selectedItems.contains(item.mediaStoreId),
                            onClick = {
                                if (isSelectionMode) {
                                    if (selectedItems.contains(item.mediaStoreId)) {
                                        viewModel.processIntent(GalleryIntent.DeselectItem(item.mediaStoreId))
                                    } else {
                                        viewModel.processIntent(GalleryIntent.SelectItem(item.mediaStoreId))
                                    }
                                } else {
                                    onNavigateToDetail(item.mediaStoreId)
                                }
                            },
                            onLongClick = {
                                if (selectedItems.contains(item.mediaStoreId)) {
                                    viewModel.processIntent(GalleryIntent.DeselectItem(item.mediaStoreId))
                                } else {
                                    viewModel.processIntent(GalleryIntent.SelectItem(item.mediaStoreId))
                                }
                            }
                        )
                    }
                }
            }

            // Error Snackbar
            errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryGridItem(
    item: SyncItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.localUri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Selection indicator
            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Status indicator
            val statusIcon = when (item.status) {
                com.photosync.domain.model.SyncStatus.SYNCED -> Icons.Default.CloudDone
                com.photosync.domain.model.SyncStatus.UPLOADING -> Icons.Default.CloudUpload
                com.photosync.domain.model.SyncStatus.FAILED -> Icons.Default.Error
                else -> null
            }

            statusIcon?.let { icon ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.status.name,
                        modifier = Modifier.padding(4.dp),
                        tint = when (item.status) {
                            com.photosync.domain.model.SyncStatus.SYNCED -> MaterialTheme.colorScheme.primary
                            com.photosync.domain.model.SyncStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}
