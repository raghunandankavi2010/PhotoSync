package com.photosync

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.photosync.data.datasource.MediaStoreDataSource
import com.photosync.data.local.dao.SyncQueueDao
import com.photosync.data.local.entity.toEntity
import com.photosync.domain.usecase.ScheduleUploadUseCase
import com.photosync.domain.usecase.StartNetworkMonitoringUseCase
import com.photosync.worker.MediaScanWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application class for PhotoSync
 */
@HiltAndroidApp
class PhotoSyncApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var mediaStoreDataSource: MediaStoreDataSource

    @Inject
    lateinit var syncQueueDao: SyncQueueDao

    @Inject
    lateinit var scheduleUploadUseCase: ScheduleUploadUseCase

    @Inject
    lateinit var workManager: WorkManager
    
    @Inject
    lateinit var startNetworkMonitoringUseCase: StartNetworkMonitoringUseCase

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Start network monitoring
        startNetworkMonitoringUseCase()

        // Start MediaStore observation
        observeMediaStoreChanges()

        // Schedule periodic media scan
        schedulePeriodicMediaScan()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    /**
     * Observe MediaStore changes and enqueue uploads
     */
    private fun observeMediaStoreChanges() {
        mediaStoreDataSource.observeMediaStoreChanges()
            .onEach { uri ->
                applicationScope.launch {
                    // Query the new photo
                    val item = mediaStoreDataSource.queryPhotoByUri(uri)
                    item?.let { photo ->
                        // Check for duplicates
                        if (!syncQueueDao.existsByChecksum(photo.checksum)) {
                            // Add to queue
                            syncQueueDao.insert(photo.toEntity())
                            // Schedule upload
                            scheduleUploadUseCase(photo.mediaStoreId)
                        }
                    }
                }
            }
            .launchIn(applicationScope)
    }

    /**
     * Schedule periodic media scan worker
     */
    private fun schedulePeriodicMediaScan() {
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

        workManager.enqueueUniquePeriodicWork(
            MediaScanWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            mediaScanWork
        )
    }
}
