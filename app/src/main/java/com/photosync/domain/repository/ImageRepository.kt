package com.photosync.domain.repository

import android.net.Uri

interface ImageRepository {
    suspend fun compressImage(imageUri: Uri): Uri
}
