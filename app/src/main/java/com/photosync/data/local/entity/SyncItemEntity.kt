package com.photosync.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.photosync.domain.model.SyncItem
import com.photosync.domain.model.SyncStatus

/**
 * Room entity for sync queue items
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["checksum"], unique = true),
        Index(value = ["status"]),
        Index(value = ["dateAdded"])
    ]
)
data class SyncItemEntity(
    @PrimaryKey
    val mediaStoreId: Long,
    val localUri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateAdded: Long,
    val displayName: String,
    val checksum: String,
    val serverFileId: String? = null,
    val uploadedBytes: Long = 0,
    val uploadUrl: String? = null,
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val errorMessage: String? = null
)

/**
 * Extension function to convert Entity to Domain model
 */
fun SyncItemEntity.toDomainModel(): SyncItem {
    return SyncItem(
        mediaStoreId = mediaStoreId,
        localUri = android.net.Uri.parse(localUri),
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        dateAdded = dateAdded,
        checksum = checksum,
        displayName = displayName,
        serverFileId = serverFileId,
        uploadedBytes = uploadedBytes,
        uploadUrl = uploadUrl,
        status = status,
        retryCount = retryCount,
        lastAttemptAt = lastAttemptAt
    )
}

/**
 * Extension function to convert Domain model to Entity
 */
fun SyncItem.toEntity(): SyncItemEntity {
    return SyncItemEntity(
        mediaStoreId = mediaStoreId,
        localUri = localUri.toString(),
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        dateAdded = dateAdded,
        displayName = displayName,
        checksum = checksum,
        serverFileId = serverFileId,
        uploadedBytes = uploadedBytes,
        uploadUrl = uploadUrl,
        status = status,
        retryCount = retryCount,
        lastAttemptAt = lastAttemptAt
    )
}
