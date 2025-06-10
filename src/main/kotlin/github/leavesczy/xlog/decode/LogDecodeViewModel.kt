package github.leavesczy.xlog.decode

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import compose_multiplatform_xlog_decode.generated.resources.Res
import compose_multiplatform_xlog_decode.generated.resources.parsing_successful_file_path
import github.leavesczy.xlog.decode.core.DecryptUtils
import github.leavesczy.xlog.decode.core.LogDecode
import github.leavesczy.xlog.decode.core.Logger
import github.leavesczy.xlog.decode.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.getString
import java.awt.Desktop
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.pathString

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:16
 * @Desc:
 */
class LogDecodeViewModel : ViewModel(viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)) {

    var mainPageViewState by mutableStateOf(
        value = MainPageViewState(
            page = Page.Main,
            privateKey = "",
            openDialog = DialogState(),
            logPath = "",
            runtimeLog = "",
            logScrollState = ScrollState(initial = 0),
            onInputPrivateKeyChange = ::onInputPrivateKeyChange,
            openFileDialog = ::openFileDialog,
            onInputLogFilePathChange = ::onInputLogFilePathChange,
            decodeLog = ::decodeLog,
            openFile = ::openFile,
            switchPage = ::switchPage
        )
    )
        private set

    var secretKeyPageViewState by mutableStateOf(
        value = SecretKeyPageViewState(
            privateKey = "",
            publicKey = "",
            generateTheKeyPair = ::generateTheKeyPair
        )
    )
        private set

    var settingsPageViewState by mutableStateOf(
        value = SettingsPageViewState(
            theme = Theme.Light,
            switchTheme = ::switchTheme,
            autoOpenFileWhenParsingIsSuccessful = false,
            updateAutoOpenFileWhenParsingIsSuccessful = ::updateAutoOpenFileWhenParsingIsSuccessful
        )
    )
        private set

    private val logDecode = LogDecode(logger = object : Logger {
        override fun debug(log: () -> Any) {
            appendLog(log = log)
        }

        override fun error(log: () -> Any) {
            appendLog(log = log)
        }
    })

    init {
        viewModelScope.launch {
            initView()
        }
    }

    private suspend fun initView() {
        val privateKey = DataStoreManager.privateKeyFlow().first()
        val themeType = DataStoreManager.themeFlow().first()
        val theme = Theme.entries.find { it.type == themeType } ?: settingsPageViewState.theme
        val autOpenFileWhenParsingIsSuccessful = DataStoreManager.autoOpenFileWhenParsingIsSuccessful().first()
        mainPageViewState = mainPageViewState.copy(privateKey = privateKey)
        settingsPageViewState = settingsPageViewState.copy(
            theme = theme,
            autoOpenFileWhenParsingIsSuccessful = autOpenFileWhenParsingIsSuccessful
        )
    }

    private fun onInputPrivateKeyChange(privateKey: String) {
        mainPageViewState = mainPageViewState.copy(privateKey = privateKey)
        viewModelScope.launch {
            DataStoreManager.updatePrivateKey(privateKey = privateKey)
        }
    }

    private fun onInputLogFilePathChange(logPath: Path) {
        mainPageViewState = mainPageViewState.copy(logPath = logPath.pathString)
    }

    private suspend fun openFileDialog() {
        mainPageViewState.openDialog.awaitResult()
    }

    private suspend fun decodeLog(): File? {
        return withContext(context = Dispatchers.Default) {
            val logPath = mainPageViewState.logPath
            val logFile = File(logPath)
            val outFile = buildOutFile(logFile = logFile)
            try {
                logDecode.decodeFile(
                    privateKey = mainPageViewState.privateKey,
                    logFile = logFile,
                    outFile = outFile
                )
                val filePathLog = getString(resource = Res.string.parsing_successful_file_path, outFile.absolutePath)
                appendLog {
                    filePathLog
                }
                autoOpenFileIfNeed(file = outFile)
                outFile
            } catch (throwable: Throwable) {
                outFile.delete()
                appendLog {
                    val stringWriter = StringWriter()
                    throwable.printStackTrace(PrintWriter(stringWriter, true))
                    stringWriter.toString()
                }
                null
            }
        }
    }

    private fun buildOutFile(logFile: File): File {
        return File(
            logFile.parentFile,
            logFile.nameWithoutExtension + "_" + SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(Date()) + ".txt"
        )
    }

    private fun appendLog(log: () -> Any) {
        val mLog = log().toString()
        if (mLog.isNotBlank()) {
            mainPageViewState =
                mainPageViewState.copy(runtimeLog = mainPageViewState.runtimeLog + mLog + "\n\n")
        }
    }

    private suspend fun openFile(file: File) {
        withContext(context = Dispatchers.IO) {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(file)
                }
            }
        }
    }

    private suspend fun autoOpenFileIfNeed(file: File) {
        if (settingsPageViewState.autoOpenFileWhenParsingIsSuccessful) {
            openFile(file = file)
        }
    }

    private fun generateTheKeyPair() {
        viewModelScope.launch {
            withContext(context = Dispatchers.Default) {
                val keyPair = DecryptUtils.generateKeyPair()
                secretKeyPageViewState = secretKeyPageViewState.copy(
                    privateKey = keyPair.privateKey,
                    publicKey = keyPair.publicKey
                )
            }
        }
    }

    private fun switchPage(page: Page) {
        mainPageViewState = mainPageViewState.copy(page = page)
    }

    private fun switchTheme(theme: Theme) {
        viewModelScope.launch {
            settingsPageViewState = settingsPageViewState.copy(theme = theme)
            DataStoreManager.updateTheme(theme = theme.type)
        }
    }

    private fun updateAutoOpenFileWhenParsingIsSuccessful(autoOpen: Boolean) {
        viewModelScope.launch {
            settingsPageViewState = settingsPageViewState.copy(autoOpenFileWhenParsingIsSuccessful = autoOpen)
            DataStoreManager.autoOpenFileWhenParsingIsSuccessful(autoOpen = autoOpen)
        }
    }

}