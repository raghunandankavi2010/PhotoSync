package com.photosync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.photosync.data.datasource.MediaStoreDataSource
import com.photosync.data.local.dao.SyncMetadataDao
import com.photosync.data.local.dao.SyncQueueDao
import com.photosync.data.local.entity.SyncMetadataKeys
import com.photosync.domain.model.SyncStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Periodic worker to scan MediaStore for missed photos
 * This catches photos added while the app was dead
 */
@HiltWorker
class MediaScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val syncQueueDao: SyncQueueDao,
    private val syncMetadataDao: SyncMetadataDao
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "media_scan_worker"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val mediaScanWork = PeriodicWorkRequestBuilder<MediaScanWorker>(
                15, TimeUnit.MINUTES, // Repeat every 15 minutes
                5, TimeUnit.MINUTES  // Flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                mediaScanWork
            )
        }
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Get last sync timestamp
                val lastSyncTimestamp = syncMetadataDao.getLongValue(
                    SyncMetadataKeys.LAST_SYNC_TIMESTAMP,
                    0L
                )

                // Query MediaStore for new photos
                val newPhotos = mediaStoreDataSource.queryPhotosSince(lastSyncTimestamp)

                var maxTimestamp = lastSyncTimestamp

                newPhotos.forEach { photo ->
                    // Check for duplicates
                    if (!syncQueueDao.existsByChecksum(photo.checksum)) {
                        // Add to queue
                        syncQueueDao.insert(photo.toEntity())
                        
                        // Schedule upload
                        scheduleUpload(photo.mediaStoreId)
                    }

                    // Track max timestamp
                    if (photo.dateAdded > maxTimestamp) {
                        maxTimestamp = photo.dateAdded
                    }
                }

                // Update last sync timestamp
                syncMetadataDao.setLongValue(SyncMetadataKeys.LAST_SYNC_TIMESTAMP, maxTimestamp)

                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private fun scheduleUpload(mediaStoreId: Long) {
        // This will be called by the repository/use case layer
        // The worker just inserts to DB, the observer/scheduler handles the rest
    }
}
