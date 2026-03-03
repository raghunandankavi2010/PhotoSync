package com.photosync.data.remote.upload

import android.content.ContentResolver
import android.net.Uri
import com.photosync.data.local.dao.SyncQueueDao
import com.photosync.domain.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chunked uploader with resume capability for S3 multipart uploads
 */
@Singleton
class ChunkedUploader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val contentResolver: ContentResolver,
    private val syncQueueDao: SyncQueueDao
) {
    companion object {
        const val CHUNK_SIZE = 5 * 1024 * 1024L // 5 MB - minimum for S3 multipart
        const val MAX_RETRIES_PER_CHUNK = 3
    }

    /**
     * Upload a file in chunks with resume capability
     */
    suspend fun upload(
        localUri: Uri,
        uploadUrl: String,
        mediaStoreId: Long,
        startByte: Long = 0,
        onProgress: (uploaded: Long, total: Long) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = contentResolver.openInputStream(localUri)
                ?: throw IOException("Cannot open file: $localUri")

            stream.use { inputStream ->
                val totalSize = getFileSize(localUri)
                val skipped = inputStream.skip(startByte)
                
                if (skipped != startByte) {
                    throw IOException("Failed to skip to offset $startByte")
                }

                var bytesUploaded = startByte
                val buffer = ByteArray(CHUNK_SIZE.toInt())

                while (isActive) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    // Retry logic for each chunk
                    var chunkRetries = 0
                    var chunkUploaded = false

                    while (chunkRetries < MAX_RETRIES_PER_CHUNK && !chunkUploaded) {
                        try {
                            putChunk(
                                uploadUrl = uploadUrl,
                                buffer = buffer,
                                bytesRead = bytesRead,
                                bytesUploaded = bytesUploaded,
                                totalSize = totalSize
                            )
                            chunkUploaded = true
                        } catch (e: IOException) {
                            chunkRetries++
                            if (chunkRetries >= MAX_RETRIES_PER_CHUNK) {
                                throw IOException("Failed to upload chunk after $MAX_RETRIES_PER_CHUNK retries", e)
                            }
                            // Exponential backoff
                            kotlinx.coroutines.delay(1000L * chunkRetries)
                        }
                    }

                    bytesUploaded += bytesRead
                    onProgress(bytesUploaded, totalSize)

                    // Persist progress AFTER successful chunk upload
                    syncQueueDao.updateProgress(mediaStoreId, bytesUploaded)
                }
            }
        }
    }

    /**
     * Upload a single chunk to S3
     */
    private fun putChunk(
        uploadUrl: String,
        buffer: ByteArray,
        bytesRead: Int,
        bytesUploaded: Long,
        totalSize: Long
    ) {
        val endByte = bytesUploaded + bytesRead - 1

        // Only use the actual bytes read, not the entire buffer
        val chunkData = buffer.copyOfRange(0, bytesRead)

        val request = Request.Builder()
            .url(uploadUrl)
            .put(chunkData.toRequestBody("application/octet-stream".toMediaType()))
            .addHeader("Content-Range", "bytes $bytesUploaded-$endByte/$totalSize")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 308) { // 308 is resume incomplete
                throw IOException("Chunk upload failed: HTTP ${response.code}")
            }
        }
    }

    /**
     * Get file size from URI
     */
    private fun getFileSize(uri: Uri): Long {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                cursor.getLong(sizeIndex)
            } else {
                // Fallback: read stream
                contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
            }
        } ?: 0L
    }
}
