-ignorewarnings
-optimizationpasses 6
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn kotlinx.datetime.**
-keep class org.bouncycastle.** { *; }
-keep class org.lwjgl.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}