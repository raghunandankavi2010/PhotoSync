package com.photosync.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

/**
 * Worker for uploading a single photo
 */
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
    }

    override suspend fun doWork(): Result {
        val mediaStoreId = inputData.getLong(KEY_MEDIA_STORE_ID, -1)
        if (mediaStoreId == -1L) {
            return Result.failure()
        }

        return withContext(Dispatchers.IO) {
            try {
                // Get sync item from database
                val item = syncQueueDao.getById(mediaStoreId)
                    ?: return@withContext Result.failure()

                // Skip if already synced or duplicate
                if (item.status == SyncStatus.SYNCED || item.status == SyncStatus.DUPLICATE) {
                    return@withContext Result.success()
                }

                // Determine upload quality
                val quality = getUploadQualityUseCase()
                val uploadUri = if (quality == UploadQuality.COMPRESSED) {
                    imageRepository.compressImage(Uri.parse(item.localUri))
                } else {
                    Uri.parse(item.localUri)
                }

                // Get size of the file to be uploaded
                val fileSize = if (quality == UploadQuality.COMPRESSED) {
                    context.contentResolver.openAssetFileDescriptor(uploadUri, "r")?.use {
                        it.length
                    } ?: item.sizeBytes
                } else {
                    item.sizeBytes
                }

                // Mark as uploading
                syncQueueDao.markAsUploading(mediaStoreId)

                // Request upload URL
                val uploadUrlResponse = try {
                    val response = syncApiService.requestUploadUrl(
                        com.photosync.data.remote.api.UploadUrlRequest(
                            checksum = item.checksum,
                            mimeType = item.mimeType,
                            sizeBytes = fileSize,
                            fileName = item.displayName
                        )
                    )

                    if (!response.isSuccessful) {
                        throw HttpException(response)
                    }

                    response.body()
                } catch (e: Exception) {
                    syncQueueDao.markAsFailed(mediaStoreId, e.message)
                    return@withContext if (runAttemptCount < MAX_RETRIES) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }

                // Check if already exists (deduplication)
                if (uploadUrlResponse?.alreadyExists == true) {
                    syncQueueDao.markAsDuplicate(mediaStoreId)
                    return@withContext Result.success()
                }

                val uploadUrl = uploadUrlResponse?.uploadUrl
                    ?: return@withContext Result.failure()

                // Perform chunked upload
                val uploadResult = chunkedUploader.upload(
                    localUri = uploadUri,
                    uploadUrl = uploadUrl,
                    mediaStoreId = mediaStoreId,
                    startByte = item.uploadedBytes
                ) { uploaded, total ->
                    // Progress is already persisted in ChunkedUploader
                }

                // Clean up compressed file if it was created
                if (quality == UploadQuality.COMPRESSED) {
                    try {
                        context.contentResolver.delete(uploadUri, null, null)
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                }

                if (uploadResult.isFailure) {
                    val error = uploadResult.exceptionOrNull()
                    syncQueueDao.markAsFailed(mediaStoreId, error?.message)
                    return@withContext if (runAttemptCount < MAX_RETRIES) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }

                // Confirm upload completion
                val serverFileId = uploadUrlResponse?.serverFileId ?: ""
                try {
                    val confirmResponse = syncApiService.confirmUpload(serverFileId)
                    if (!confirmResponse.isSuccessful) {
                        throw HttpException(confirmResponse)
                    }
                } catch (e: Exception) {
                    // Upload succeeded but confirmation failed - will be retried
                    syncQueueDao.markAsFailed(mediaStoreId, e.message)
                    return@withContext if (runAttemptCount < MAX_RETRIES) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }

                // Mark as synced
                syncQueueDao.markAsSynced(mediaStoreId, serverFileId)
                Result.success()

            } catch (e: Exception) {
                // Unexpected error
                try {
                    syncQueueDao.markAsFailed(mediaStoreId, e.message)
                } catch (_: Exception) {
                    // Ignore DB errors
                }
                if (runAttemptCount < MAX_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }
}
