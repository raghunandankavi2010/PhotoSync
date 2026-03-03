package com.photosync.di

import android.content.ContentResolver
import android.content.Context
import androidx.work.WorkManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.photosync.BuildConfig
import com.photosync.data.datasource.MediaStoreDataSource
import com.photosync.data.local.dao.SyncMetadataDao
import com.photosync.data.local.dao.SyncQueueDao
import com.photosync.data.local.database.SyncDatabase
import com.photosync.data.remote.api.SyncApiService
import com.photosync.data.remote.upload.ChunkedUploader
import com.photosync.data.repository.ImageRepositoryImpl
import com.photosync.data.repository.NetworkRepositoryImpl
import com.photosync.data.repository.SyncRepositoryImpl
import com.photosync.domain.repository.ImageRepository
import com.photosync.domain.repository.NetworkRepository
import com.photosync.domain.repository.SyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Context
    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    // Database
    @Provides
    @Singleton
    fun provideSyncDatabase(@ApplicationContext context: Context): SyncDatabase {
        return SyncDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSyncQueueDao(database: SyncDatabase): SyncQueueDao {
        return database.syncQueueDao()
    }

    @Provides
    @Singleton
    fun provideSyncMetadataDao(database: SyncDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }

    // MediaStore DataSource
    @Provides
    @Singleton
    fun provideMediaStoreDataSource(@ApplicationContext context: Context): MediaStoreDataSource {
        return MediaStoreDataSource(context)
    }

    // OkHttp
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideSyncApiService(retrofit: Retrofit): SyncApiService {
        return retrofit.create(SyncApiService::class.java)
    }

    // ChunkedUploader
    @Provides
    @Singleton
    fun provideChunkedUploader(
        okHttpClient: OkHttpClient,
        contentResolver: ContentResolver,
        syncQueueDao: SyncQueueDao
    ): ChunkedUploader {
        return ChunkedUploader(okHttpClient, contentResolver, syncQueueDao)
    }

    // Repositories
    @Provides
    @Singleton
    fun provideSyncRepository(
        @ApplicationContext context: Context,
        syncQueueDao: SyncQueueDao,
        syncMetadataDao: SyncMetadataDao,
        mediaStoreDataSource: MediaStoreDataSource,
        syncApiService: SyncApiService,
        chunkedUploader: ChunkedUploader
    ): SyncRepository {
        return SyncRepositoryImpl(
            context,
            syncQueueDao,
            syncMetadataDao,
            mediaStoreDataSource,
            syncApiService,
            chunkedUploader
        )
    }

    @Provides
    @Singleton
    fun provideNetworkRepository(@ApplicationContext context: Context): NetworkRepository {
        return NetworkRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideImageRepository(@ApplicationContext context: Context): ImageRepository {
        return ImageRepositoryImpl(context)
    }
}
