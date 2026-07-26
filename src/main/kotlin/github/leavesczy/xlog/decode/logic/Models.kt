package github.leavesczy.xlog.decode.logic

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Stable
import java.io.File

enum class Page {
    Decode,
    SecretKey,
    Settings
}

enum class ThemeMode(val id: Int) {
    System(id = 0),
    Light(id = 1),
    Dark(id = 2);

    companion object {
        fun fromId(id: Int): ThemeMode? {
            return entries.find { it.id == id }
        }
    }
}

data class RuntimeLogEntry(
    val id: Long,
    val message: String
)

@Stable
data class MainPageViewState(
    val page: Page,
    val onPageSelected: (Page) -> Unit
)

@Stable
data class DecodePageViewState(
    val privateKey: String,
    val selectedLogFiles: List<String>,
    val runtimeLogs: List<RuntimeLogEntry>,
    val lazyListState: LazyListState,
    val onPrivateKeyChanged: (String) -> Unit,
    val onLogFilesSelected: (List<String>) -> Unit,
    val onDecodeLogs: suspend () -> List<File>,
    val onOpenFiles: suspend (List<File>) -> Unit
)

@Stable
data class SecretKeyPageViewState(
    val privateKey: String,
    val publicKey: String,
    val onGenerateKeyPair: () -> Unit
)

@Stable
data class SettingsPageViewState(
    val themeMode: ThemeMode,
    val onThemeModeSelected: (ThemeMode) -> Unit,
    val autoOpenOnSuccess: Boolean,
    val onAutoOpenOnSuccessChanged: (Boolean) -> Unit
)
