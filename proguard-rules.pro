-ignorewarnings
-optimizationpasses 10
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn kotlinx.datetime.**
-keep class org.bouncycastle.** { *; }
-keep class org.lwjgl.** { *; }
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Required on JVM for JNA-based integrations.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
# Required when using FileKit Dialogs on Linux (XDG Desktop Portal / DBus).
-keep class org.freedesktop.dbus.** { *; }
-keep class io.github.vinceglb.filekit.dialogs.platform.xdg.** { *; }
-keepattributes Signature,InnerClasses,RuntimeVisibleAnnotations

-keep class github.leavesczy.xlog.decode.** { *; }