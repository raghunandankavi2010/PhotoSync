package com.photosync.domain.usecase

import com.photosync.domain.model.SyncItem
import com.photosync.domain.model.SyncStatus
import com.photosync.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to get all sync items
 */
class GetSyncItemsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<List<SyncItem>> {
        return syncRepository.getAllSyncItemsFlow()
    }
}

/**
 * Use case to get sync items by status
 */
class GetSyncItemsByStatusUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(status: SyncStatus): Flow<List<SyncItem>> {
        return when (status) {
            SyncStatus.PENDING -> syncRepository.getPendingItemsFlow()
            SyncStatus.UPLOADING -> syncRepository.getUploadingItemsFlow()
            SyncStatus.SYNCED -> syncRepository.getSyncedItemsFlow()
            SyncStatus.FAILED -> syncRepository.getFailedItemsFlow()
            else -> syncRepository.getAllSyncItemsFlow()
        }
    }
}

/**
 * Use case to get pending and paused items (for network recovery)
 */
class GetPendingAndPausedItemsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): List<SyncItem> {
        return syncRepository.getPendingAndPausedItems()
    }
}

/**
 * Use case to get sync statistics
 */
class GetSyncStatsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    operator fun invoke(): Flow<com.photosync.domain.model.SyncStats> {
        return syncRepository.getSyncStatsFlow()
    }
}

/**
 * Use case to retry failed uploads
 */
class RetryFailedUploadsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return syncRepository.retryFailedItems()
    }
}

/**
 * Use case to clear synced items from local queue
 */
class ClearSyncedItemsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return syncRepository.clearSyncedItems()
    }
}

/**
 * Use case to pause all active uploads
 */
class PauseAllUploadsUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return syncRepository.pauseActiveUploads()
    }
}

/**
 * Use case to check if item exists by checksum (deduplication)
 */
class CheckDuplicateUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(checksum: String): Boolean {
        return syncRepository.existsByChecksum(checksum)
    }
}
