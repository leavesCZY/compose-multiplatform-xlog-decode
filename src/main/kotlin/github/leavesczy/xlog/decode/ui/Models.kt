package github.leavesczy.xlog.decode.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File
import java.nio.file.Path

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:15
 * @Desc:
 */
enum class Page(
    val title: String,
    val icon: ImageVector
) {
    Main(
        title = "Log",
        icon = Icons.Outlined.Loop
    ),
    CryptKey(
        title = "密钥",
        icon = Icons.Outlined.Key
    ),
    Settings(
        title = "设置",
        icon = Icons.Outlined.Settings
    )
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
    val onInputPrivateKey: (String) -> Unit,
    val openFileDialog: suspend () -> Unit,
    val onInputLogFilePath: (Path) -> Unit,
    val decodeLog: suspend () -> File?,
    val openFile: suspend (File) -> Unit,
    val switchPage: (Page) -> Unit
)

@Stable
data class CryptKeyPageViewState(
    val privateKey: String,
    val publicKey: String,
    val generateKeyPair: () -> Unit
)

@Stable
data class SettingsPageViewState(
    val theme: Theme,
    val switchTheme: (Theme) -> Unit,
    val autoOpenFileWhenParsingIsSuccessful: Boolean,
    val updateAutoOpenFileWhenParsingIsSuccessful: (Boolean) -> Unit
)