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

// BouncyCastle Maven jars are signed. ProGuard rewrites class bytes even when kept,
// which breaks META-INF signature digests at runtime. Strip signatures after download.
val bouncyCastleSigned = configurations.create("bouncyCastleSigned") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val unsignedBouncyCastleJar = tasks.register<Jar>("unsignedBouncyCastleJar") {
    description = ""
    dependsOn(bouncyCastleSigned)
    from({
        zipTree(bouncyCastleSigned.singleFile)
    }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.RSA")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.EC")
    }
    archiveBaseName.set("bcprov-jdk18on-unsigned")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
}

dependencies {
    implementation(libs.luben.zstd.jni)
    implementation(files(unsignedBouncyCastleJar))
    bouncyCastleSigned(libs.bouncycastle.bcprov.jdk18on)
}