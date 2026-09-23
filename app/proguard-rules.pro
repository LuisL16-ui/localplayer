-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Jaudiotagger
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**

# Glance AppWidget & DataStore
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# WorkManager & Room (used by Glance)
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# AndroidX Startup Initializers
-keep class * extends androidx.startup.Initializer { *; }
-keep class androidx.startup.** { *; }

# Media & Media3
-keep class androidx.media.** { *; }
-dontwarn androidx.media.**
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-ignorewarnings