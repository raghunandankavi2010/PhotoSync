package com.photosync.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.photosync.data.local.dao.SyncMetadataDao
import com.photosync.data.local.dao.SyncQueueDao
import com.photosync.data.remote.api.SyncApiService
import com.photosync.data.remote.upload.ChunkedUploader
import com.photosync.data.datasource.MediaStoreDataSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom WorkerFactory for creating workers with dependencies
 * Note: When using HiltWorker, this is not needed as Hilt handles injection
 * This is kept for reference and fallback scenarios
 */
@Singleton
class SyncWorkerFactory @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val syncApiService: SyncApiService,
    private val chunkedUploader: ChunkedUploader,
    private val mediaStoreDataSource: MediaStoreDataSource
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            UploadWorker::class.java.name -> {
                UploadWorker(
                    appContext,
                    workerParameters,
                    syncQueueDao,
                    syncApiService,
                    chunkedUploader
                )
            }
            MediaScanWorker::class.java.name -> {
                MediaScanWorker(
                    appContext,
                    workerParameters,
                    mediaStoreDataSource,
                    syncQueueDao,
                    syncMetadataDao
                )
            }
            RetryWorker::class.java.name -> {
                RetryWorker(
                    appContext,
                    workerParameters,
                    syncQueueDao
                )
            }
            else -> null
        }
    }
}
