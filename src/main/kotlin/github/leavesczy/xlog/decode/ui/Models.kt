package github.leavesczy.xlog.decode.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import java.io.File
import java.nio.file.Path

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:15
 * @Desc:
 */
enum class Page {
    Main,
    SecretKey,
    Settings
}

enum class Theme(val type: Int) {
    System(type = 0),
    Light(type = 1),
    Dark(type = 2)
}

@Stable
data class MainPageViewState(
    val page: Page,
    val privateKey: String,
    val openDialog: DialogState<Path?>,
    val logPath: String,
    val runtimeLog: String,
    val logScrollState: ScrollState,
    val onInputPrivateKeyChange: (String) -> Unit,
    val openFileDialog: suspend () -> Unit,
    val onInputLogFilePathChange: (Path) -> Unit,
    val decodeLog: suspend () -> File?,
    val openFile: suspend (File) -> Unit,
    val switchPage: (Page) -> Unit
)

@Stable
data class SecretKeyPageViewState(
    val privateKey: String,
    val publicKey: String,
    val generateTheKeyPair: () -> Unit
)

@Stable
data class SettingsPageViewState(
    val theme: Theme,
    val switchTheme: (Theme) -> Unit,
    val autoOpenFileWhenParsingIsSuccessful: Boolean,
    val updateAutoOpenFileWhenParsingIsSuccessful: (Boolean) -> Unit
)