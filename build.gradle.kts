import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.jetbrains.compose.compiler)
}

kotlin {
    jvmToolchain(18)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_18)
        optIn.set(
            setOf(
                "androidx.compose.ui.ExperimentalComposeUiApi",
                "androidx.compose.foundation.ExperimentalFoundationApi",
                "androidx.compose.material3.ExperimentalMaterial3Api"
            )
        )
    }
}

tasks {
    withType<Jar> {
        exclude(
            "META-INF/*.MF",
            "META-INF/*.RSA",
            "META-INF/*.SF",
            "META-INF/*.EC",
            "META-INF/*.DSA",
            "META-INF/*.LIST",
            "META-INF/*.kotlin_module",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt"
        )
    }
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
}

enum class OS(val id: String) {
    Linux("linux"),
    Windows("windows"),
    MacOS("macos")
}

val currentOS: OS by lazy {
    val os = System.getProperty("os.name")
    when {
        os.equals("Mac OS X", ignoreCase = true) -> OS.MacOS
        os.startsWith("Win", ignoreCase = true) -> OS.Windows
        os.startsWith("Linux", ignoreCase = true) -> OS.Linux
        else -> error("Unknown OS name: $os")
    }
}

compose.desktop {
    application {
        mainClass = "github.leavesczy.xlog.decode.MainKt"
        val mPackageName = "compose-multiplatform-xlog-decode"
        nativeDistributions {
            includeAllModules = false
            modules = arrayListOf("jdk.unsupported", "java.desktop", "java.logging")
            when (currentOS) {
                OS.Windows -> {
                    targetFormats(TargetFormat.AppImage, TargetFormat.Exe)
                }

                OS.MacOS -> {
                    targetFormats(TargetFormat.Dmg)
                }

                OS.Linux -> {
                    targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
                }
            }
            packageName = mPackageName
            packageVersion = "1.1.1"
            description = "compose multiplatform xlog decode"
            copyright = "© 2024 leavesCZY. All rights reserved."
            vendor = "leavesCZY"
            val resourcesDir = project.file("src/main/resources")
            windows {
                menuGroup = packageName
                dirChooser = true
                perUserInstall = true
                shortcut = true
                menu = true
                upgradeUuid = "D542171E-5CDC-428E-BF21-68FBAD85369F"
                iconFile.set(resourcesDir.resolve("windows_launch_icon.ico"))
                installationPath = packageName
            }
            macOS {
                bundleID = mPackageName
                setDockNameSameAsPackageName = true
                appStore = true
                iconFile.set(resourcesDir.resolve("macos_launch_icon.icns"))
            }
            linux {
                shortcut = true
                menuGroup = mPackageName
                iconFile.set(resourcesDir.resolve("linux_launch_icon.png"))
            }
        }
        buildTypes.release {
            proguard {
                isEnabled.set(true)
                obfuscate.set(true)
                optimize.set(true)
                joinOutputJars.set(true)
                configurationFiles.from("proguard-rules.pro")
            }
        }
    }
}