package com.photosync.domain.model

import android.net.Uri

/**
 * Domain model representing a photo item in the sync queue.
 * This is the core entity used throughout the domain layer.
 */
data class SyncItem(
    val mediaStoreId: Long,
    val localUri: Uri,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val checksum: String,
    val displayName: String,
    val serverFileId: String? = null,
    val uploadedBytes: Long = 0,
    val uploadUrl: String? = null,
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null
) {
    val progress: Float
        get() = if (sizeBytes > 0) uploadedBytes.toFloat() / sizeBytes else 0f

    val isPending: Boolean
        get() = status == SyncStatus.PENDING

    val isUploading: Boolean
        get() = status == SyncStatus.UPLOADING

    val isSynced: Boolean
        get() = status == SyncStatus.SYNCED

    val isFailed: Boolean
        get() = status == SyncStatus.FAILED

    val isPaused: Boolean
        get() = status == SyncStatus.PAUSED

    val isDuplicate: Boolean
        get() = status == SyncStatus.DUPLICATE
}

enum class SyncStatus {
    PENDING,
    UPLOADING,
    PAUSED,
    SYNCED,
    FAILED,
    DUPLICATE
}

/**
 * Upload strategy for handling different network conditions
 */
sealed class UploadStrategy {
    data object FullQuality : UploadStrategy()
    data object PreviewOnly : UploadStrategy()
    data class AdaptiveQuality(
        val previewQuality: Int = 75,
        val uploadOriginalOnWifi: Boolean = true
    ) : UploadStrategy()
}

/**
 * User preferences for sync behavior
 */
data class SyncPreferences(
    val wifiOnly: Boolean = true,
    val chargingOnly: Boolean = false,
    val dataSaver: Boolean = false,
    val autoSync: Boolean = true,
    val folderFilters: List<String> = emptyList(),
    val stripExif: Boolean = false
)

/**
 * Network state for determining upload strategy
 */
data class NetworkState(
    val isConnected: Boolean,
    val isWifi: Boolean,
    val isMetered: Boolean,
    val isRoaming: Boolean
)

/**
 * Upload progress for UI updates
 */
data class UploadProgress(
    val mediaStoreId: Long,
    val uploadedBytes: Long,
    val totalBytes: Long,
    val status: SyncStatus
)

/**
 * Sync statistics for dashboard
 */
data class SyncStats(
    val pending: Int,
    val uploading: Int,
    val synced: Int,
    val failed: Int,
    val duplicate: Int,
    val totalBytes: Long,
    val uploadedBytes: Long
) {
    val totalProgress: Float
        get() = if (totalBytes > 0) uploadedBytes.toFloat() / totalBytes else 0f
}

/**
 * Server photo metadata for restore flow
 */
data class ServerPhoto(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateTaken: Long,
    val downloadUrl: String,
    val thumbnailUrl: String?,
    val exifJson: String?
)
