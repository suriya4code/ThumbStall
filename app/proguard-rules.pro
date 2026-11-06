# ThumbStall ProGuard Rules

# Keep Kotlin metadata for reflection
-keepattributes RuntimeVisibleAnnotations

# Keep DataStore Preferences keys
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep data classes used in UI state
-keep class app.thumbstall.ui.IntensityOption { *; }
-keep class app.thumbstall.ui.PermissionState { *; }
-keep class app.thumbstall.ui.PermissionCardData { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Compose stability
-keep class androidx.compose.** { *; }

# Keep Room entities and DAOs
-keep class app.thumbstall.data.PauseEvent { *; }
-keep class app.thumbstall.data.EventDao { *; }
-keep class app.thumbstall.data.AppDatabase { *; }

# Keep core classes
-keep class app.thumbstall.core.** { *; }
