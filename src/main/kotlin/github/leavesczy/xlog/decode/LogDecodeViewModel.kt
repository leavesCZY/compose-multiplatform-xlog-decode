package github.leavesczy.xlog.decode

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.xlog.decode.core.DecryptUtils
import github.leavesczy.xlog.decode.core.LogDecode
import github.leavesczy.xlog.decode.core.Logger
import github.leavesczy.xlog.decode.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
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
            onInputPrivateKey = ::onInputPrivateKey,
            openFileDialog = ::openFileDialog,
            onInputLogFilePath = ::onInputLogFilePath,
            decodeLog = ::decodeLog,
            openFile = ::openFile,
            switchPage = ::switchPage
        )
    )
        private set

    var cryptKeyPageViewState by mutableStateOf(
        value = CryptKeyPageViewState(
            privateKey = "",
            publicKey = "",
            generateKeyPair = ::generateKeyPair
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
        val autOpenFileWhenParsingIsSuccessful =
            DataStoreManager.autoOpenFileWhenParsingIsSuccessful().first()
        if (mainPageViewState.privateKey != privateKey) {
            mainPageViewState = mainPageViewState.copy(privateKey = privateKey)
        }
        if (settingsPageViewState.theme != theme || settingsPageViewState.autoOpenFileWhenParsingIsSuccessful != autOpenFileWhenParsingIsSuccessful) {
            settingsPageViewState = settingsPageViewState.copy(
                theme = theme,
                autoOpenFileWhenParsingIsSuccessful = autOpenFileWhenParsingIsSuccessful
            )
        }
    }

    private fun onInputPrivateKey(privateKey: String) {
        mainPageViewState = mainPageViewState.copy(privateKey = privateKey)
        viewModelScope.launch {
            DataStoreManager.updatePrivateKey(privateKey = privateKey)
        }
    }

    private fun onInputLogFilePath(logPath: Path) {
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
                appendLog {
                    "解析成功，文件路径：" + outFile.absolutePath + "\n\n"
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
        val logFileName = logFile.nameWithoutExtension
        val outFileName =
            logFileName + "_" + SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(Date()) + ".txt"
        return File(logFile.parentFile, outFileName)
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

    private fun generateKeyPair() {
        viewModelScope.launch {
            withContext(context = Dispatchers.Default) {
                val keyPair = DecryptUtils.generateKeyPair()
                cryptKeyPageViewState = cryptKeyPageViewState.copy(
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
            settingsPageViewState =
                settingsPageViewState.copy(autoOpenFileWhenParsingIsSuccessful = autoOpen)
            DataStoreManager.autoOpenFileWhenParsingIsSuccessful(autoOpen = autoOpen)
        }
    }

}