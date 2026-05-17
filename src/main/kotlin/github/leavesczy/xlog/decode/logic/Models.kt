package github.leavesczy.xlog.decode.logic

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import java.io.File

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:15
 * @Desc:
 */
enum class Page {
    Decryption,
    SecretKey,
    Settings;
}

enum class Theme(val type: Int) {
    System(type = 0),
    Light(type = 1),
    Dark(type = 2);
}

@Stable
data class MainPageViewState(
    val page: Page,
    val switchPage: (Page) -> Unit
)

@Stable
data class DecryptionPageViewState(
    val privateKey: String,
    val selectedLogFiles: List<String>,
    val runtimeLogs: List<String>,
    val lazyListState: LazyListState,
    val onInputPrivateKey: (String) -> Unit,
    val onLogFileIsSelected: (List<String>) -> Unit,
    val decodeLog: suspend () -> List<File>?,
    val openFile: suspend (List<File>) -> Unit
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