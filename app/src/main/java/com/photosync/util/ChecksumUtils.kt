package com.photosync.util

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Utility class for computing file checksums
 */
object ChecksumUtils {

    private const val BUFFER_SIZE = 8192

    /**
     * Compute SHA-256 checksum for a file
     */
    suspend fun computeSHA256(
        contentResolver: ContentResolver,
        uri: Uri
    ): String = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }

                digest.digest().joinToString("") { "%02x".format(it) }
            } ?: ""
        }.getOrDefault(uri.toString().hashCode().toString())
    }

    /**
     * Compute MD5 checksum (faster but less secure)
     */
    suspend fun computeMD5(
        contentResolver: ContentResolver,
        uri: Uri
    ): String = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = MessageDigest.getInstance("MD5")
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }

                digest.digest().joinToString("") { "%02x".format(it) }
            } ?: ""
        }.getOrDefault(uri.toString().hashCode().toString())
    }

    /**
     * Compute checksum using specified algorithm
     */
    suspend fun computeChecksum(
        contentResolver: ContentResolver,
        uri: Uri,
        algorithm: String = "SHA-256"
    ): String = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = MessageDigest.getInstance(algorithm)
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }

                digest.digest().joinToString("") { "%02x".format(it) }
            } ?: ""
        }.getOrDefault(uri.toString().hashCode().toString())
    }
}
