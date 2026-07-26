-ignorewarnings
-optimizationpasses 10
-dontwarn **

-keep class github.leavesczy.xlog.decode.** { *; }
-keep class compose_multiplatform_xlog_decode.generated.** { *; }

# Compose/Skiko bytecode must not be rewritten (VerifyError: Bad return type).
-keep,includecode class androidx.compose.** { *; }
-keep,includecode class org.jetbrains.compose.** { *; }
-keep,includecode class org.jetbrains.skiko.** { *; }
-keep,includecode class org.jetbrains.skia.** { *; }
-keep,includecode class androidx.lifecycle.** { *; }

# Classical BC for secp256k1 ECDH (exclude PQC by omission).
-keep class org.bouncycastle.jce.** { *; }
-keep class org.bouncycastle.jcajce.** { *; }
-keep class org.bouncycastle.math.** { *; }
-keep class org.bouncycastle.asn1.** { *; }
-keep class org.bouncycastle.crypto.** { *; }
-keep class org.bouncycastle.util.** { *; }
-keep class org.bouncycastle.internal.** { *; }

-keep,allowshrinking class com.github.luben.zstd.** { *; }

# JNA looks up fromNative/toNative via JNI; shrink/optimize breaks UnsatisfiedLinkError.
-keep,includecode class com.sun.jna.** { *; }
-keep,includecode class * implements com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** {
    <fields>;
    <methods>;
}

# FileKit Windows dialogs depend on intact JNA Structure/GUID bindings.
-keep,includecode class io.github.vinceglb.filekit.** { *; }
-keep,allowshrinking class org.freedesktop.dbus.** { *; }

-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}