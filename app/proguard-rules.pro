-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**

-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

-keep class androidx.media.** { *; }
-dontwarn androidx.media.**

-ignorewarnings