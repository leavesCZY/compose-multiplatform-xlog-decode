import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.jetbrains.compose.compiler)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        optIn.set(
            setOf(
                "androidx.compose.ui.ExperimentalComposeUiApi"
            )
        )
    }
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.components.resources)
    implementation(libs.jetbrains.compose.material.icons.core)
    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
    implementation(libs.jetbrains.kotlinx.coroutines.swing)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.filekit.dialogs)
}

// ProGuard 7.8 ships kotlin-metadata that only reads ≤2.3; Kotlin 2.4 writes 2.4.0 modules.
// Drop *.kotlin_module from jars so ProGuard can process release inputs.
tasks.withType<Jar>().configureEach {
    exclude("META-INF/*.kotlin_module")
}

subprojects {
    tasks.withType<Jar>().configureEach {
        exclude("META-INF/*.kotlin_module")
    }
}

enum class OS {
    Linux,
    Windows,
    MacOs;
}

val currentOS: OS by lazy {
    val os = System.getProperty("os.name").orEmpty()
    when {
        os.equals("Mac OS X", ignoreCase = true) -> OS.MacOs
        os.startsWith("Win", ignoreCase = true) -> OS.Windows
        os.startsWith("Linux", ignoreCase = true) -> OS.Linux
        else -> error("Unknown OS name: $os")
    }
}

compose.desktop {
    application {
        mainClass = "github.leavesczy.xlog.decode.MainKt"
        val appPackageName = "compose-multiplatform-xlog-decode"
        nativeDistributions {
            includeAllModules = false
            modules = arrayListOf("jdk.unsupported", "java.desktop", "java.logging")
            when (currentOS) {
                OS.Windows -> {
                    targetFormats(TargetFormat.AppImage, TargetFormat.Exe)
                }

                OS.MacOs -> {
                    targetFormats(TargetFormat.Dmg)
                }

                OS.Linux -> {
                    targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
                }
            }
            packageName = appPackageName
            packageVersion = "1.0.0"
            description = "compose multiplatform xlog decode"
            copyright = "© 2024 leavesCZY. All rights reserved."
            vendor = "leavesCZY"
            val packagingIconsDir = project.file("packaging/icons")
            windows {
                menuGroup = packageName
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menu = true
                upgradeUuid = "D542171E-5CDC-428E-BF21-68FBAD85369F"
                iconFile.set(packagingIconsDir.resolve("windows_launch_icon.ico"))
                installationPath = packageName
            }
            macOS {
                bundleID = appPackageName
                setDockNameSameAsPackageName = true
                appStore = false
                iconFile.set(packagingIconsDir.resolve("macos_launch_icon.icns"))
            }
            linux {
                shortcut = true
                menuGroup = appPackageName
                iconFile.set(packagingIconsDir.resolve("linux_launch_icon.png"))
                modules("jdk.security.auth")
            }
        }
        buildTypes.release {
            proguard {
                // Kotlin 2.4 metadata needs ProGuard ≥7.9.x (Compose default 7.8 maxes at 2.3).
                version.set("7.9.1")
                isEnabled.set(true)
                // Obfuscate/shrink dependencies where safe; business code kept in proguard-rules.
                obfuscate.set(true)
                // Optimize other deps; Compose/Skiko protected via -keep,includecode.
                optimize.set(true)
                // Must stay off: merging jars breaks BouncyCastle digests / META-INF.
                joinOutputJars.set(false)
                configurationFiles.from("proguard-rules.pro")
            }
        }
    }
}