package com.photosync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.photosync.data.local.entity.SyncItemEntity
import com.photosync.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for sync queue operations
 */
@Dao
interface SyncQueueDao {

    // Insert operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncItemEntity>)

    // Query operations
    @Query("SELECT * FROM sync_queue ORDER BY dateAdded DESC")
    fun getAllFlow(): Flow<List<SyncItemEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY dateAdded DESC")
    fun getByStatusFlow(status: SyncStatus): Flow<List<SyncItemEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY dateAdded DESC")
    fun getPendingFlow(): Flow<List<SyncItemEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'UPLOADING' ORDER BY dateAdded DESC")
    fun getUploadingFlow(): Flow<List<SyncItemEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'SYNCED' ORDER BY dateAdded DESC")
    fun getSyncedFlow(): Flow<List<SyncItemEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'FAILED' ORDER BY dateAdded DESC")
    fun getFailedFlow(): Flow<List<SyncItemEntity>>

    @Query("SELECT * FROM sync_queue WHERE mediaStoreId = :mediaStoreId")
    suspend fun getById(mediaStoreId: Long): SyncItemEntity?

    @Query("SELECT * FROM sync_queue WHERE checksum = :checksum")
    suspend fun getByChecksum(checksum: String): SyncItemEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM sync_queue WHERE checksum = :checksum)")
    suspend fun existsByChecksum(checksum: String): Boolean

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'PAUSED') ORDER BY dateAdded ASC")
    suspend fun getPendingAndPaused(): List<SyncItemEntity>

    @Query("SELECT * FROM sync_queue WHERE status = 'UPLOADING' AND (lastAttemptAt IS NULL OR lastAttemptAt < :staleCutoff)")
    suspend fun getStalledUploads(staleCutoff: Long): List<SyncItemEntity>

    // Update operations
    @Update
    suspend fun update(item: SyncItemEntity)

    @Query("UPDATE sync_queue SET status = 'UPLOADING', lastAttemptAt = :timestamp WHERE mediaStoreId = :mediaStoreId")
    suspend fun markAsUploading(mediaStoreId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sync_queue SET status = 'SYNCED', serverFileId = :serverFileId WHERE mediaStoreId = :mediaStoreId")
    suspend fun markAsSynced(mediaStoreId: Long, serverFileId: String)

    @Query("UPDATE sync_queue SET status = 'FAILED', errorMessage = :errorMessage, retryCount = retryCount + 1 WHERE mediaStoreId = :mediaStoreId")
    suspend fun markAsFailed(mediaStoreId: Long, errorMessage: String?)

    @Query("UPDATE sync_queue SET status = 'PAUSED' WHERE mediaStoreId = :mediaStoreId")
    suspend fun markAsPaused(mediaStoreId: Long)

    @Query("UPDATE sync_queue SET status = 'DUPLICATE' WHERE mediaStoreId = :mediaStoreId")
    suspend fun markAsDuplicate(mediaStoreId: Long)

    @Query("UPDATE sync_queue SET uploadedBytes = :uploadedBytes, status = 'UPLOADING' WHERE mediaStoreId = :mediaStoreId")
    suspend fun updateProgress(mediaStoreId: Long, uploadedBytes: Long)

    @Query("UPDATE sync_queue SET status = 'PAUSED' WHERE status = 'UPLOADING'")
    suspend fun pauseActiveUploads()

    @Query("UPDATE sync_queue SET status = 'PENDING', retryCount = 0 WHERE status = 'FAILED'")
    suspend fun retryFailedItems()

    @Query("SELECT * FROM sync_queue WHERE status = :status ORDER BY dateAdded DESC")
    suspend fun getByStatus(status: SyncStatus): List<SyncItemEntity>

    // Delete operations
    @Query("DELETE FROM sync_queue WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteById(mediaStoreId: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun deleteSyncedItems()

    @Query("DELETE FROM sync_queue WHERE status = 'FAILED'")
    suspend fun deleteFailedItems()

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()

    // Statistics
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'UPLOADING'")
    suspend fun getUploadingCount(): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun getSyncedCount(): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'FAILED'")
    suspend fun getFailedCount(): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'DUPLICATE'")
    suspend fun getDuplicateCount(): Int

    @Query("SELECT COALESCE(SUM(sizeBytes), 0) FROM sync_queue")
    suspend fun getTotalBytes(): Long

    @Query("SELECT COALESCE(SUM(uploadedBytes), 0) FROM sync_queue WHERE status IN ('UPLOADING', 'SYNCED')")
    suspend fun getUploadedBytes(): Long

    // Transaction helpers
    @Transaction
    suspend fun insertOrUpdate(item: SyncItemEntity) {
        val existing = getById(item.mediaStoreId)
        if (existing == null) {
            insert(item)
        } else {
            update(item.copy(retryCount = existing.retryCount))
        }
    }
}
