package com.photosync.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.photosync.R
import com.photosync.data.local.dao.SyncQueueDao
import com.photosync.data.remote.api.SyncApiService
import com.photosync.data.remote.upload.ChunkedUploader
import com.photosync.domain.model.SyncStatus
import com.photosync.domain.model.UploadQuality
import com.photosync.domain.repository.ImageRepository
import com.photosync.domain.usecase.GetUploadQualityUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import androidx.core.net.toUri

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val syncQueueDao: SyncQueueDao,
    private val syncApiService: SyncApiService,
    private val chunkedUploader: ChunkedUploader,
    private val getUploadQualityUseCase: GetUploadQualityUseCase,
    private val imageRepository: ImageRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_MEDIA_STORE_ID = "media_store_id"
        const val MAX_RETRIES = 3
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "sync_channel"
    }

    override suspend fun doWork(): Result {
        val mediaStoreId = inputData.getLong(KEY_MEDIA_STORE_ID, -1)
        if (mediaStoreId == -1L) {
            return Result.failure()
        }

        setForeground(createForegroundInfo("Starting sync..."))

        return withContext(Dispatchers.IO) {
            try {
                val item = syncQueueDao.getById(mediaStoreId) ?: return@withContext Result.failure()

                if (item.status == SyncStatus.SYNCED || item.status == SyncStatus.DUPLICATE) {
                    return@withContext Result.success()
                }

                val quality = getUploadQualityUseCase()
                val (uploadUri, fileSize) = if (quality == UploadQuality.COMPRESSED) {
                    val compressedUri = imageRepository.compressImage(item.localUri.toUri())
                    val size = context.contentResolver.openAssetFileDescriptor(compressedUri, "r")?.use { it.length } ?: -1L
                    Pair(compressedUri, size)
                } else {
                    Pair(item.localUri.toUri(), item.sizeBytes)
                }

                syncQueueDao.markAsUploading(mediaStoreId)
                setForeground(createForegroundInfo("Uploading: ${item.displayName}"))

                val uploadUrlResponse = try {
                    val response = syncApiService.requestUploadUrl(
                        com.photosync.data.remote.api.UploadUrlRequest(
                            checksum = item.checksum,
                            mimeType = item.mimeType,
                            sizeBytes = fileSize,
                            fileName = item.displayName
                        )
                    )
                    if (!response.isSuccessful) throw HttpException(response)
                    response.body()
                } catch (e: Exception) {
                    handleFailure(mediaStoreId, e.message)
                    return@withContext Result.retry()
                }

                if (uploadUrlResponse?.alreadyExists == true) {
                    syncQueueDao.markAsDuplicate(mediaStoreId)
                    return@withContext Result.success()
                }

                val uploadUrl = uploadUrlResponse?.uploadUrl ?: return@withContext handleFailure(mediaStoreId, "Empty upload URL")

                val uploadResult = chunkedUploader.upload(
                    localUri = uploadUri,
                    uploadUrl = uploadUrl,
                    mediaStoreId = mediaStoreId,
                    startByte = item.uploadedBytes
                ) { uploaded, total ->
                    val progress = ((uploaded.toDouble() / total) * 100).toInt()
                    val notification = createForegroundInfo("Uploading: ${item.displayName}", progress)
                    setForeground(notification)
                }

                if (quality == UploadQuality.COMPRESSED) {
                    context.contentResolver.delete(uploadUri, null, null)
                }

                if (uploadResult.isFailure) {
                    return@withContext handleFailure(mediaStoreId, uploadResult.exceptionOrNull()?.message)
                }

                val serverFileId = uploadUrlResponse.serverFileId
                try {
                    val confirmResponse = syncApiService.confirmUpload(serverFileId)
                    if (!confirmResponse.isSuccessful) throw HttpException(confirmResponse)
                } catch (e: Exception) {
                    return@withContext handleFailure(mediaStoreId, "Upload confirmation failed: ${e.message}")
                }

                syncQueueDao.markAsSynced(mediaStoreId, serverFileId)
                Result.success()
            } catch (e: Exception) {
                handleFailure(mediaStoreId, e.message)
            }
        }
    }

    private suspend fun handleFailure(mediaStoreId: Long, error: String?): Result {
        syncQueueDao.markAsFailed(mediaStoreId, error)
        return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
    }

    private fun createForegroundInfo(text: String, progress: Int = -1): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("PhotoSync")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .apply {
                if (progress >= 0) {
                    setProgress(100, progress, false)
                }
            }
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Syncing"
            val descriptionText = "Photo synchronization status"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
