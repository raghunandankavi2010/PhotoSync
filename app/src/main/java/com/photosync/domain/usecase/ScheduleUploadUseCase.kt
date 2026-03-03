package com.photosync.domain.usecase

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.photosync.domain.model.SyncItem
import com.photosync.domain.repository.SyncRepository
import com.photosync.worker.UploadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Use case to schedule an upload worker for a sync item
 */
class ScheduleUploadUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncRepository: SyncRepository
) {
    operator fun invoke(mediaStoreId: Long) {
        val preferences = syncRepository.getSyncPreferences()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (preferences.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .setRequiresBatteryNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(UploadWorker.KEY_MEDIA_STORE_ID to mediaStoreId))
            .build()

        // One job per photo - second enqueue for same ID is silently ignored
        WorkManager.getInstance(context).enqueueUniqueWork(
            "upload_$mediaStoreId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}

/**
 * Use case to schedule upload for a sync item object
 */
class ScheduleSyncItemUploadUseCase @Inject constructor(
    private val scheduleUploadUseCase: ScheduleUploadUseCase
) {
    operator fun invoke(item: SyncItem) {
        scheduleUploadUseCase(item.mediaStoreId)
    }
}

/**
 * Use case to schedule multiple uploads
 */
class ScheduleMultipleUploadsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncRepository: SyncRepository
) {
    operator fun invoke(mediaStoreIds: List<Long>) {
        val preferences = syncRepository.getSyncPreferences()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (preferences.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .setRequiresBatteryNotLow(true)
            .build()

        mediaStoreIds.forEach { mediaStoreId ->
            val request = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(UploadWorker.KEY_MEDIA_STORE_ID to mediaStoreId))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "upload_$mediaStoreId",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
