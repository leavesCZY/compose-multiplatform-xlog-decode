import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

kotlin {
    jvmToolchain(18)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_18)
    }
}

dependencies {
    implementation(files("libs/bcprov-jdk18on-1.78.1.jar"))
//    implementation(libs.bouncycastle.bcprov.jdk18on)
    implementation(libs.luben.zstd.jni)
}