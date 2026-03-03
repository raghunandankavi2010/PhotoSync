package com.photosync.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.photosync.data.datasource.MediaStoreDataSource
import com.photosync.data.local.dao.SyncMetadataDao
import com.photosync.data.local.dao.SyncQueueDao
import com.photosync.data.local.entity.SyncMetadataKeys
import com.photosync.data.local.entity.toDomainModel
import com.photosync.data.local.entity.toEntity
import com.photosync.data.remote.api.SyncApiService
import com.photosync.data.remote.api.toDomainModel
import com.photosync.data.remote.upload.ChunkedUploader
import com.photosync.domain.model.ServerPhoto
import com.photosync.domain.model.SyncItem
import com.photosync.domain.model.SyncPreferences
import com.photosync.domain.model.SyncStats
import com.photosync.domain.model.SyncStatus
import com.photosync.domain.model.UploadStrategy
import com.photosync.domain.repository.SyncRepository
import com.photosync.domain.repository.UploadUrlResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SyncRepository
 */
@Singleton
class SyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncQueueDao: SyncQueueDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val syncApiService: SyncApiService,
    private val chunkedUploader: ChunkedUploader
) : SyncRepository {

    // Queue Management
    override suspend fun addToSyncQueue(item: SyncItem): Result<Unit> {
        return runCatching {
            // Check for duplicates
            if (syncQueueDao.existsByChecksum(item.checksum)) {
                // Mark as duplicate if already exists
                val existing = syncQueueDao.getByChecksum(item.checksum)
                if (existing != null && existing.mediaStoreId != item.mediaStoreId) {
                    syncQueueDao.insert(item.copy(status = SyncStatus.DUPLICATE).toEntity())
                }
            } else {
                syncQueueDao.insert(item.toEntity())
            }
        }
    }

    override suspend fun removeFromQueue(mediaStoreId: Long): Result<Unit> {
        return runCatching {
            syncQueueDao.deleteById(mediaStoreId)
        }
    }

    override suspend fun updateSyncItem(item: SyncItem): Result<Unit> {
        return runCatching {
            syncQueueDao.update(item.toEntity())
        }
    }

    override suspend fun getSyncItem(mediaStoreId: Long): SyncItem? {
        return syncQueueDao.getById(mediaStoreId)?.toDomainModel()
    }

    override fun getAllSyncItemsFlow(): Flow<List<SyncItem>> {
        return syncQueueDao.getAllFlow().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getPendingItemsFlow(): Flow<List<SyncItem>> {
        return syncQueueDao.getPendingFlow().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getUploadingItemsFlow(): Flow<List<SyncItem>> {
        return syncQueueDao.getUploadingFlow().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getSyncedItemsFlow(): Flow<List<SyncItem>> {
        return syncQueueDao.getSyncedFlow().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getFailedItemsFlow(): Flow<List<SyncItem>> {
        return syncQueueDao.getFailedFlow().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun getPendingAndPausedItems(): List<SyncItem> {
        return syncQueueDao.getPendingAndPaused().map { it.toDomainModel() }
    }

    override suspend fun getStalledUploads(staleCutoff: Long): List<SyncItem> {
        return syncQueueDao.getStalledUploads(staleCutoff).map { it.toDomainModel() }
    }

    // Sync Operations
    override suspend fun markAsUploading(mediaStoreId: Long): Result<Unit> {
        return runCatching {
            syncQueueDao.markAsUploading(mediaStoreId)
        }
    }

    override suspend fun markAsSynced(mediaStoreId: Long, serverFileId: String): Result<Unit> {
        return runCatching {
            syncQueueDao.markAsSynced(mediaStoreId, serverFileId)
        }
    }

    override suspend fun markAsFailed(mediaStoreId: Long, error: String?): Result<Unit> {
        return runCatching {
            syncQueueDao.markAsFailed(mediaStoreId, error)
        }
    }

    override suspend fun markAsPaused(mediaStoreId: Long): Result<Unit> {
        return runCatching {
            syncQueueDao.markAsPaused(mediaStoreId)
        }
    }

    override suspend fun markAsDuplicate(mediaStoreId: Long): Result<Unit> {
        return runCatching {
            syncQueueDao.markAsDuplicate(mediaStoreId)
        }
    }

    override suspend fun updateProgress(mediaStoreId: Long, uploadedBytes: Long): Result<Unit> {
        return runCatching {
            syncQueueDao.updateProgress(mediaStoreId, uploadedBytes)
        }
    }

    override suspend fun pauseActiveUploads(): Result<Unit> {
        return runCatching {
            syncQueueDao.pauseActiveUploads()
        }
    }

    // Deduplication
    override suspend fun existsByChecksum(checksum: String): Boolean {
        return syncQueueDao.existsByChecksum(checksum)
    }

    override suspend fun getItemByChecksum(checksum: String): SyncItem? {
        return syncQueueDao.getByChecksum(checksum)?.toDomainModel()
    }

    // Stats
    override fun getSyncStatsFlow(): Flow<SyncStats> {
        return combine(
            syncQueueDao.getPendingFlow(),
            syncQueueDao.getUploadingFlow(),
            syncQueueDao.getSyncedFlow(),
            syncQueueDao.getFailedFlow()
        ) { pending, uploading, synced, failed ->
            val totalBytes = pending.sumOf { it.sizeBytes } +
                    uploading.sumOf { it.sizeBytes } +
                    synced.sumOf { it.sizeBytes } +
                    failed.sumOf { it.sizeBytes }
            
            val uploadedBytes = pending.sumOf { it.uploadedBytes } +
                    uploading.sumOf { it.uploadedBytes } +
                    synced.sumOf { it.sizeBytes } + // Synced items are fully uploaded
                    failed.sumOf { it.uploadedBytes }

            SyncStats(
                pending = pending.size,
                uploading = uploading.size,
                synced = synced.size,
                failed = failed.size,
                duplicate = syncQueueDao.getDuplicateCount(),
                totalBytes = totalBytes,
                uploadedBytes = uploadedBytes
            )
        }
    }

    override suspend fun getLastSyncTimestamp(): Long {
        return syncMetadataDao.getLongValue(SyncMetadataKeys.LAST_SYNC_TIMESTAMP, 0L)
    }

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        syncMetadataDao.setLongValue(SyncMetadataKeys.LAST_SYNC_TIMESTAMP, timestamp)
    }

    // Preferences
    override fun getSyncPreferencesFlow(): Flow<SyncPreferences> {
        return combine(
            syncMetadataDao.getByKeyFlow(SyncMetadataKeys.WIFI_ONLY),
            syncMetadataDao.getByKeyFlow(SyncMetadataKeys.AUTO_SYNC),
            syncMetadataDao.getByKeyFlow(SyncMetadataKeys.DATA_SAVER),
            syncMetadataDao.getByKeyFlow(SyncMetadataKeys.CHARGING_ONLY)
        ) { wifiOnly, autoSync, dataSaver, chargingOnly ->
            SyncPreferences(
                wifiOnly = wifiOnly?.booleanValue ?: true,
                autoSync = autoSync?.booleanValue ?: true,
                dataSaver = dataSaver?.booleanValue ?: false,
                chargingOnly = chargingOnly?.booleanValue ?: false
            )
        }
    }

    override suspend fun getSyncPreferences(): SyncPreferences {
        return SyncPreferences(
            wifiOnly = syncMetadataDao.getBooleanValue(SyncMetadataKeys.WIFI_ONLY, true),
            autoSync = syncMetadataDao.getBooleanValue(SyncMetadataKeys.AUTO_SYNC, true),
            dataSaver = syncMetadataDao.getBooleanValue(SyncMetadataKeys.DATA_SAVER, false),
            chargingOnly = syncMetadataDao.getBooleanValue(SyncMetadataKeys.CHARGING_ONLY, false)
        )
    }

    override suspend fun updateSyncPreferences(preferences: SyncPreferences) {
        syncMetadataDao.setBooleanValue(SyncMetadataKeys.WIFI_ONLY, preferences.wifiOnly)
        syncMetadataDao.setBooleanValue(SyncMetadataKeys.AUTO_SYNC, preferences.autoSync)
        syncMetadataDao.setBooleanValue(SyncMetadataKeys.DATA_SAVER, preferences.dataSaver)
        syncMetadataDao.setBooleanValue(SyncMetadataKeys.CHARGING_ONLY, preferences.chargingOnly)
    }

    // MediaStore Operations
    override suspend fun scanMediaStore(sinceTimestamp: Long): List<SyncItem> {
        return mediaStoreDataSource.queryPhotosSince(sinceTimestamp)
    }

    override suspend fun queryMediaStoreByUri(uri: Uri): SyncItem? {
        return mediaStoreDataSource.queryPhotoByUri(uri)
    }

    // Upload Operations
    override suspend fun requestUploadUrl(
        checksum: String,
        mimeType: String,
        sizeBytes: Long
    ): Result<UploadUrlResponse> {
        return runCatching {
            val response = syncApiService.requestUploadUrl(
                com.photosync.data.remote.api.UploadUrlRequest(
                    checksum = checksum,
                    mimeType = mimeType,
                    sizeBytes = sizeBytes,
                    fileName = "photo_$checksum"
                )
            )
            
            if (response.isSuccessful) {
                response.body()?.toDomainModel()
                    ?: throw IOException("Empty response body")
            } else {
                throw HttpException(response)
            }
        }
    }

    override suspend fun confirmUpload(serverFileId: String): Result<Unit> {
        return runCatching {
            val response = syncApiService.confirmUpload(serverFileId)
            if (!response.isSuccessful) {
                throw HttpException(response)
            }
        }
    }

    // Restore Operations
    override suspend fun getServerPhotos(): Result<List<ServerPhoto>> {
        return runCatching {
            val response = syncApiService.getPhotos()
            if (response.isSuccessful) {
                response.body()?.map { it.toDomainModel() } ?: emptyList()
            } else {
                throw HttpException(response)
            }
        }
    }

    override suspend fun downloadAndSavePhoto(photo: ServerPhoto): Result<Unit> {
        return runCatching {
            // Insert as pending to MediaStore
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, photo.fileName)
                put(MediaStore.Images.Media.MIME_TYPE, photo.mimeType)
                put(MediaStore.Images.Media.DATE_TAKEN, photo.dateTaken)
                put(MediaStore.Images.Media.IS_PENDING, 1) // Hidden during write
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: throw IOException("MediaStore insert failed")

            try {
                // Download and save
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    // TODO: Implement actual download using OkHttp
                    // For now, this is a placeholder
                    val url = java.net.URL(photo.downloadUrl)
                    url.openStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Mark as visible
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)

            } catch (e: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw e
            }
        }
    }

    // Cleanup
    override suspend fun clearSyncedItems(): Result<Unit> {
        return runCatching {
            syncQueueDao.deleteSyncedItems()
        }
    }

    override suspend fun clearFailedItems(): Result<Unit> {
        return runCatching {
            syncQueueDao.deleteFailedItems()
        }
    }

    override suspend fun retryFailedItems(): Result<Unit> {
        return runCatching {
            syncQueueDao.retryFailedItems()
        }
    }
}
