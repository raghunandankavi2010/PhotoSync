package com.photosync.data.remote.api

import com.photosync.domain.model.ServerPhoto
import com.photosync.domain.repository.UploadUrlResponse
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit API service for sync operations
 */
interface SyncApiService {

    /**
     * Request a presigned upload URL
     */
    @POST("v1/photos/upload-url")
    suspend fun requestUploadUrl(
        @Body request: UploadUrlRequest
    ): Response<UploadUrlResponseDto>

    /**
     * Confirm upload completion
     */
    @POST("v1/photos/{serverFileId}/confirm")
    suspend fun confirmUpload(
        @Path("serverFileId") serverFileId: String
    ): Response<Unit>

    /**
     * Get all photos for the user (restore flow)
     */
    @GET("v1/photos")
    suspend fun getPhotos(): Response<List<ServerPhotoDto>>

    /**
     * Delete a photo
     */
    @POST("v1/photos/{serverFileId}/delete")
    suspend fun deletePhoto(
        @Path("serverFileId") serverFileId: String
    ): Response<Unit>
}

/**
 * DTO for upload URL request
 */
@Serializable
data class UploadUrlRequest(
    val checksum: String,
    val mimeType: String,
    val sizeBytes: Long,
    val fileName: String
)

/**
 * DTO for upload URL response
 */
@Serializable
data class UploadUrlResponseDto(
    val alreadyExists: Boolean,
    val serverFileId: String,
    val uploadUrl: String,
    val uploadId: String? = null,
    val expiresAt: Long? = null
)

/**
 * DTO for server photo
 */
@Serializable
data class ServerPhotoDto(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val dateTaken: Long,
    val downloadUrl: String,
    val thumbnailUrl: String? = null,
    val exifJson: String? = null
)

/**
 * Extension to convert DTO to domain model
 */
fun UploadUrlResponseDto.toDomainModel(): UploadUrlResponse {
    return UploadUrlResponse(
        alreadyExists = alreadyExists,
        serverFileId = serverFileId,
        uploadUrl = uploadUrl,
        uploadId = uploadId,
        expiresAt = expiresAt
    )
}

/**
 * Extension to convert DTO to domain model
 */
fun ServerPhotoDto.toDomainModel(): ServerPhoto {
    return ServerPhoto(
        id = id,
        fileName = fileName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        dateTaken = dateTaken,
        downloadUrl = downloadUrl,
        thumbnailUrl = thumbnailUrl,
        exifJson = exifJson
    )
}
