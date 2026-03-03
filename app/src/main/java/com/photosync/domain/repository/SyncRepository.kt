package com.photosync.domain.repository

import com.photosync.domain.model.ServerPhoto
import com.photosync.domain.model.SyncItem
import com.photosync.domain.model.SyncPreferences
import com.photosync.domain.model.SyncStats
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for sync operations.
 * Defines the contract for data operations between domain and data layers.
 */
interface SyncRepository {

    // Queue Management
    suspend fun addToSyncQueue(item: SyncItem): Result<Unit>
    suspend fun removeFromQueue(mediaStoreId: Long): Result<Unit>
    suspend fun updateSyncItem(item: SyncItem): Result<Unit>
    suspend fun getSyncItem(mediaStoreId: Long): SyncItem?
    fun getAllSyncItemsFlow(): Flow<List<SyncItem>>
    fun getPendingItemsFlow(): Flow<List<SyncItem>>
    fun getUploadingItemsFlow(): Flow<List<SyncItem>>
    fun getSyncedItemsFlow(): Flow<List<SyncItem>>
    fun getFailedItemsFlow(): Flow<List<SyncItem>>
    suspend fun getPendingAndPausedItems(): List<SyncItem>
    suspend fun getStalledUploads(staleCutoff: Long): List<SyncItem>

    // Sync Operations
    suspend fun markAsUploading(mediaStoreId: Long): Result<Unit>
    suspend fun markAsSynced(mediaStoreId: Long, serverFileId: String): Result<Unit>
    suspend fun markAsFailed(mediaStoreId: Long, error: String?): Result<Unit>
    suspend fun markAsPaused(mediaStoreId: Long): Result<Unit>
    suspend fun markAsDuplicate(mediaStoreId: Long): Result<Unit>
    suspend fun updateProgress(mediaStoreId: Long, uploadedBytes: Long): Result<Unit>
    suspend fun pauseActiveUploads(): Result<Unit>

    // Deduplication
    suspend fun existsByChecksum(checksum: String): Boolean
    suspend fun getItemByChecksum(checksum: String): SyncItem?

    // Stats
    fun getSyncStatsFlow(): Flow<SyncStats>
    suspend fun getLastSyncTimestamp(): Long
    suspend fun updateLastSyncTimestamp(timestamp: Long)

    // Preferences
    fun getSyncPreferencesFlow(): Flow<SyncPreferences>
    suspend fun getSyncPreferences(): SyncPreferences
    suspend fun updateSyncPreferences(preferences: SyncPreferences)

    // MediaStore Operations
    suspend fun scanMediaStore(sinceTimestamp: Long): List<SyncItem>
    suspend fun queryMediaStoreByUri(uri: android.net.Uri): SyncItem?

    // Upload Operations
    suspend fun requestUploadUrl(
        checksum: String,
        mimeType: String,
        sizeBytes: Long
    ): Result<UploadUrlResponse>

    suspend fun confirmUpload(serverFileId: String): Result<Unit>

    // Restore Operations
    suspend fun getServerPhotos(): Result<List<ServerPhoto>>
    suspend fun downloadAndSavePhoto(photo: ServerPhoto): Result<Unit>

    // Cleanup
    suspend fun clearSyncedItems(): Result<Unit>
    suspend fun clearFailedItems(): Result<Unit>
    suspend fun retryFailedItems(): Result<Unit>
}

/**
 * Response from upload URL request
 */
data class UploadUrlResponse(
    val alreadyExists: Boolean,
    val serverFileId: String,
    val uploadUrl: String,
    val uploadId: String? = null,
    val expiresAt: Long? = null
)
