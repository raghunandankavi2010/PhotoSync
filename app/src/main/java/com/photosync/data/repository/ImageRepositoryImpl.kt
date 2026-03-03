package com.photosync.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import com.photosync.domain.repository.ImageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageRepository {

    override suspend fun compressImage(imageUri: Uri): Uri = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, 1280, 1280)

        options.inJustDecodeBounds = false
        val bitmap = context.contentResolver.openInputStream(imageUri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: throw IllegalStateException("Could not decode bitmap")

        val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(compressedFile).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
        }

        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", compressedFile)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
