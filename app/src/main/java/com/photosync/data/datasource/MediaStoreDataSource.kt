package com.photosync.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.photosync.domain.model.SyncItem
import com.photosync.domain.model.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for MediaStore operations and observation
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val contentResolver: ContentResolver = context.contentResolver

    companion object {
        val EXTERNAL_CONTENT_URI: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        
        val PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
    }

    /**
     * Observe MediaStore changes as a Flow
     */
    fun observeMediaStoreChanges(): Flow<Uri> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                uri?.let { 
                    launch { send(it) }
                }
            }
        }

        contentResolver.registerContentObserver(
            EXTERNAL_CONTENT_URI,
            true, // observe subtree
            observer
        )

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.Main)

    /**
     * Query MediaStore for photos added since timestamp
     */
    fun queryPhotosSince(timestamp: Long): List<SyncItem> {
        val photos = mutableListOf<SyncItem>()
        
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ?"
        val selectionArgs = arrayOf((timestamp / 1000).toString()) // MediaStore uses seconds

        contentResolver.query(
            EXTERNAL_CONTENT_URI,
            PROJECTION,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, id)
                
                val item = SyncItem(
                    mediaStoreId = id,
                    localUri = contentUri,
                    mimeType = cursor.getString(mimeTypeColumn) ?: "image/jpeg",
                    sizeBytes = cursor.getLong(sizeColumn),
                    dateAdded = cursor.getLong(dateAddedColumn) * 1000, // Convert to milliseconds
                    checksum = computeChecksum(contentUri), // Compute SHA-256
                    displayName = cursor.getString(nameColumn) ?: "unknown",
                    status = SyncStatus.PENDING
                )
                
                photos.add(item)
            }
        }

        return photos
    }

    /**
     * Query a specific URI from MediaStore
     */
    fun queryPhotoByUri(uri: Uri): SyncItem? {
        contentResolver.query(
            uri,
            PROJECTION,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, id)

                return SyncItem(
                    mediaStoreId = id,
                    localUri = contentUri,
                    mimeType = cursor.getString(mimeTypeColumn) ?: "image/jpeg",
                    sizeBytes = cursor.getLong(sizeColumn),
                    dateAdded = cursor.getLong(dateAddedColumn) * 1000,
                    checksum = computeChecksum(contentUri),
                    displayName = cursor.getString(nameColumn) ?: "unknown",
                    status = SyncStatus.PENDING
                )
            }
        }
        return null
    }

    /**
     * Query all photos from MediaStore
     */
    fun queryAllPhotos(): List<SyncItem> {
        return queryPhotosSince(0)
    }

    /**
     * Compute SHA-256 checksum for a file
     */
    private fun computeChecksum(uri: Uri): String {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var bytesRead: Int
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                
                digest.digest().joinToString("") { "%02x".format(it) }
            } ?: ""
        } catch (e: Exception) {
            // Fallback to URI-based hash if file can't be read
            uri.toString().hashCode().toString()
        }
    }

    /**
     * Check if URI is an image
     */
    fun isImageUri(uri: Uri): Boolean {
        return uri.toString().startsWith(EXTERNAL_CONTENT_URI.toString())
    }
}
