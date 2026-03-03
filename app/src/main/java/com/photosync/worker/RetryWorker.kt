package com.photosync.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photosync.data.local.dao.SyncQueueDao
import com.photosync.domain.model.SyncStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker to retry failed uploads
 */
@HiltWorker
class RetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncQueueDao: SyncQueueDao
) : CoroutineWorker(context.applicationContext, params) {

    companion object {
        const val WORK_NAME = "retry_worker"
        const val KEY_MAX_RETRIES = "max_retries"
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Get failed items
                val failedItems = syncQueueDao.getByStatus(SyncStatus.FAILED)

                failedItems.forEach { item ->
                    // Reset to pending for retry
                    if (item.retryCount < (inputData.getInt(KEY_MAX_RETRIES, 3))) {
                        syncQueueDao.update(item.copy(status = SyncStatus.PENDING))
                        
                        // Schedule upload
                        // This would typically call ScheduleUploadUseCase
                        // But to avoid circular dependencies, we just update the DB
                        // and let the UI or observer trigger the upload
                    }
                }

                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }
}
