# Photo Sync - Android Photo Auto-Sync System

A production-ready Android photo auto-sync system built with Clean Architecture, MVI pattern, Jetpack Compose, WorkManager, and Room. This implementation follows the system design principles outlined in the blog article by Karishma Agrawal.

## Features

### Core Features
- **Automatic Photo Detection**: Uses ContentObserver for instant detection + WorkManager periodic scan for reliability
- **Chunked Upload with Resume**: 5MB chunks with exact-byte resume capability for S3 multipart uploads
- **Deduplication**: SHA-256 checksum-based deduplication to prevent duplicate uploads
- **Offline-First Design**: Outbox pattern with Room DB - photos are persisted before network touch
- **Battery Optimization**: Doze-compliant WorkManager with adaptive constraints

### Sync Features
- **Multiple Sync Triggers**:
  - ContentObserver (instant, while app alive)
  - MediaScanWorker (periodic, catches missed photos)
  - NetworkMonitor (gap-filler for app-killed-while-offline scenario)
  - WorkManager auto-reschedule (crash recovery)

- **User Preferences**:
  - Wi-Fi only mode
  - Charging only mode
  - Data saver mode
  - Auto sync toggle
  - EXIF stripping option

### UI Features
- **Jetpack Compose UI**: Modern declarative UI with Material 3
- **MVI Pattern**: Unidirectional data flow with ViewModels
- **Navigation Component**: Type-safe navigation with Compose
- **Real-time Updates**: Flow-based reactive UI
- **Gallery View**: Grid view with selection and batch sync
- **Sync Status Dashboard**: Statistics and progress tracking
- **Settings Screen**: User-configurable sync preferences

## Architecture

### Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                     PRESENTATION                            │
│   Compose UI ◄──── ViewModels (MVI) ◄──── UseCases         │
│        │                                                    │
│   SyncStatusScreen, GalleryScreen, SettingsScreen          │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                       DOMAIN                                │
│   Models, Repository Interfaces, Use Cases                 │
│                                                             │
│   SyncItem, SyncStatus, SyncRepository, etc.               │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                        DATA                                 │
│   Repository Implementations, Data Sources, Database       │
│                                                             │
│   SyncRepositoryImpl, Room DB, MediaStore, Retrofit        │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                  BACKGROUND WORK                            │
│   WorkManager Workers, Services, Receivers                 │
│                                                             │
│   UploadWorker, MediaScanWorker, BootReceiver              │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

#### 1. ContentObserver + WorkManager Pattern
```kotlin
// Layer 1: ContentObserver (instant detection)
mediaStoreDataSource.observeMediaStoreChanges()
    .onEach { uri ->
        // Add to queue and schedule upload
    }

// Layer 2: MediaScanWorker (reliable fallback)
PeriodicWorkRequestBuilder<MediaScanWorker>(15, TimeUnit.MINUTES)
```

#### 2. Chunked Upload with Resume
```kotlin
class ChunkedUploader {
    suspend fun upload(
        localUri: Uri,
        uploadUrl: String,
        mediaStoreId: Long,
        startByte: Long = 0,  // Resume from here
        onProgress: (uploaded: Long, total: Long) -> Unit
    )
}
```

#### 3. Outbox Pattern (Room DB)
```kotlin
@Entity(tableName = "sync_queue")
data class SyncItemEntity(
    val mediaStoreId: Long,
    val checksum: String,       // For deduplication
    val uploadedBytes: Long,    // For resume
    val status: SyncStatus,
    // ...
)
```

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture + MVI
- **DI**: Hilt
- **Background**: WorkManager
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Serialization**: Kotlinx Serialization
- **Image Loading**: Coil
- **Navigation**: Navigation Component for Compose

## Project Structure

```
app/src/main/java/com/photosync/
├── PhotoSyncApplication.kt          # Application class with Hilt
├── MainActivity.kt                  # Main entry point
├── di/
│   ├── AppModule.kt                 # Hilt modules
│   └── WorkManagerModule.kt
├── domain/
│   ├── model/
│   │   └── SyncItem.kt              # Domain models
│   ├── repository/
│   │   ├── SyncRepository.kt        # Repository interfaces
│   │   └── NetworkRepository.kt
│   └── usecase/
│       └── *.kt                     # Use cases
├── data/
│   ├── local/
│   │   ├── database/
│   │   │   └── SyncDatabase.kt      # Room database
│   │   ├── dao/
│   │   │   ├── SyncQueueDao.kt      # DAO interfaces
│   │   │   └── SyncMetadataDao.kt
│   │   └── entity/
│   │       ├── SyncItemEntity.kt    # Room entities
│   │       └── SyncMetadataEntity.kt
│   ├── remote/
│   │   ├── api/
│   │   │   └── SyncApiService.kt    # Retrofit API
│   │   └── upload/
│   │       └── ChunkedUploader.kt   # S3 chunked upload
│   ├── datasource/
│   │   └── MediaStoreDataSource.kt  # MediaStore wrapper
│   └── repository/
│       ├── SyncRepositoryImpl.kt    # Repository implementations
│       └── NetworkRepositoryImpl.kt
├── presentation/
│   ├── navigation/
│   │   └── SyncNavigation.kt        # Navigation graph
│   ├── screens/
│   │   ├── SyncStatusScreen.kt      # UI screens
│   │   ├── GalleryScreen.kt
│   │   ├── SyncDetailScreen.kt
│   │   ├── SettingsScreen.kt
│   │   └── RestoreScreen.kt
│   ├── components/
│   │   └── SyncItemCard.kt          # Reusable components
│   ├── viewmodel/
│   │   ├── SyncStatusViewModel.kt   # ViewModels
│   │   ├── GalleryViewModel.kt
│   │   ├── SettingsViewModel.kt
│   │   └── SyncDetailViewModel.kt
│   ├── state/
│   │   └── UiState.kt               # UI state classes
│   └── intent/
│       └── SyncIntent.kt            # User intents (MVI)
├── worker/
│   ├── UploadWorker.kt              # WorkManager workers
│   ├── MediaScanWorker.kt
│   ├── RetryWorker.kt
│   └── SyncWorkerFactory.kt
├── service/
│   └── UploadForegroundService.kt   # Foreground service
├── receiver/
│   └── BootReceiver.kt              # Boot receiver
├── ui/theme/
│   └── *.kt                         # Material theme
└── util/
    ├── ChecksumUtils.kt             # Utilities
    ├── NetworkUtils.kt
    └── PermissionUtils.kt
```

## Setup

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or later
- Android SDK 34

### Build
1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Build and run

### Configuration
Update API endpoints in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://your-api.com/\"")
buildConfigField("String", "S3_BUCKET_URL", "\"https://your-bucket.s3.amazonaws.com/\"")
```

## Key Implementation Details

### 1. Four-Layer Safety Net
The system uses four independent mechanisms to ensure no photo is missed:

| Scenario | Component | Mechanism |
|----------|-----------|-----------|
| Photo taken, app alive | ContentObserver | DB + enqueue() |
| Photo taken, app dead | MediaScanWorker | Scan MediaStore |
| Process killed mid-upload | WorkManager | Auto-reschedule |
| App killed offline, net returns | NetworkMonitor | Read Room DB |

### 2. Chunked Upload Implementation
```kotlin
// 5MB chunks (minimum for S3 multipart)
private val CHUNK_SIZE = 5 * 1024 * 1024L

// Resume from last persisted byte
stream.skip(startByte)

// Persist progress AFTER each chunk
localDb.updateProgress(mediaStoreId, bytesUploaded)
```

### 3. Deduplication Flow
```
Photo detected
    │
    ▼
Compute SHA-256 checksum
    │
    ▼
Check if exists in DB ──Yes──► Mark as DUPLICATE
    │ No
    ▼
Check server (via API) ──Yes──► Mark as DUPLICATE
    │ No
    ▼
Proceed with upload
```

## License

MIT License

## Credits

Based on the system design article by Karishma Agrawal:
"Designing a Photo Auto-Sync System: Android System Design"
