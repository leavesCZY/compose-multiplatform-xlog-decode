import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(files("libs/bcprov-jdk18on-1.80.jar"))
//    implementation(libs.bouncycastle.bcprov.jdk18on)
    implementation(libs.luben.zstd.jni)
}