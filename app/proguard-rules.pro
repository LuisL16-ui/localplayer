# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line number information for debugging stack traces in release
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

-keep class com.cvc953.localplayer.** { *; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

-keep class androidx.media.** { *; }
-dontwarn androidx.media.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Media3 (ExoPlayer)
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil image loading
-keep class coil.** { *; }
-dontwarn coil.**

# Navigation Compose
-keep class androidx.navigation.** { *; }

# Palette
-keep class androidx.palette.** { *; }

# Lifecycle
-keep class androidx.lifecycle.** { *; }

# WorkManager is initialized by AndroidX Startup before MainActivity and builds
# WorkDatabase through reflective Room/protobuf code in release builds.
-keep class androidx.startup.** { *; }
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }
-keep class androidx.sqlite.** { *; }
-keep class androidx.datastore.preferences.protobuf.** { *; }
-keep class androidx.datastore.** { *; }
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes AnnotationDefault

# jaudiotagger — tag reading/writing
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-ignorewarnings