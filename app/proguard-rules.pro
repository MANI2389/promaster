# PROMASTER Production ProGuard / R8 Rules

# 1. Strip sensitive debug and verbose logs in Release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# 2. Firebase & Cloud Firestore Models (Prevent property renaming on reflection serialization)
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
-keepclassmembers class com.example.promaster.data.firebase.model.** {
    <fields>;
    <init>(...);
}
-keep class com.example.promaster.data.firebase.model.** { *; }

# 3. Domain Models
-keepclassmembers class com.example.promaster.domain.model.** {
    <fields>;
    <init>(...);
}
-keep class com.example.promaster.domain.model.** { *; }

# 4. Kotlin Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-keepattributes *Annotation*, SourceFile, LineNumberTable

# 5. Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
