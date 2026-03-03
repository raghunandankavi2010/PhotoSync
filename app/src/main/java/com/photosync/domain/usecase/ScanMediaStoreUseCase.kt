package com.photosync.domain.usecase

import com.photosync.domain.model.SyncItem
import com.photosync.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Use case to scan MediaStore for new photos
 */
class ScanMediaStoreUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(): List<SyncItem> {
        val lastSyncTimestamp = syncRepository.getLastSyncTimestamp()
        return syncRepository.scanMediaStore(lastSyncTimestamp)
    }
}

/**
 * Use case to scan MediaStore since a specific timestamp
 */
class ScanMediaStoreSinceUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(sinceTimestamp: Long): List<SyncItem> {
        return syncRepository.scanMediaStore(sinceTimestamp)
    }
}

/**
 * Use case to query a specific MediaStore URI
 */
class QueryMediaStoreUriUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(uri: android.net.Uri): SyncItem? {
        return syncRepository.queryMediaStoreByUri(uri)
    }
}

/**
 * Use case to update last sync timestamp
 */
class UpdateLastSyncTimestampUseCase @Inject constructor(
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(timestamp: Long) {
        syncRepository.updateLastSyncTimestamp(timestamp)
    }
}
