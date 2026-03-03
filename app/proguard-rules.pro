# ProGuard rules for PhotoSync App

# Keep Room entities
-keep class com.photosync.data.local.entity.** { *; }

# Keep model classes for serialization
-keep class com.photosync.domain.model.** { *; }
-keep class com.photosync.data.remote.api.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Kotlin Serialization
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Hilt
-keep class * extends dagger.hilt.** { *; }
