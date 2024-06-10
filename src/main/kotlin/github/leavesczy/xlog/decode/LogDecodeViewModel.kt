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
import java.text.SimpleDateFormat
import java.util.*

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
            logPath = "",
            runtimeLog = "",
            logScrollState = ScrollState(initial = 0),
            onInputPrivateKey = ::onInputPrivateKey,
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
            autOpenFileWhenParsingIsSuccessful = false,
            switchTheme = ::switchTheme,
            updateAutOpenFileWhenParsingIsSuccessful = ::updateAutOpenFileWhenParsingIsSuccessful
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
        val autOpenFileWhenParsingIsSuccessful = DataStoreManager.autOpenFileWhenParsingIsSuccessful().first()
        if (mainPageViewState.privateKey != privateKey) {
            mainPageViewState = mainPageViewState.copy(privateKey = privateKey)
        }
        if (settingsPageViewState.theme != theme || settingsPageViewState.autOpenFileWhenParsingIsSuccessful != autOpenFileWhenParsingIsSuccessful) {
            settingsPageViewState = settingsPageViewState.copy(
                theme = theme,
                autOpenFileWhenParsingIsSuccessful = autOpenFileWhenParsingIsSuccessful
            )
        }
    }

    private fun onInputPrivateKey(privateKey: String) {
        mainPageViewState = mainPageViewState.copy(privateKey = privateKey)
        viewModelScope.launch {
            DataStoreManager.updatePrivateKey(privateKey = privateKey)
        }
    }

    private fun onInputLogFilePath(logPath: String) {
        mainPageViewState = mainPageViewState.copy(logPath = logPath)
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
                    "解密成功，文件路径：" + outFile.absolutePath
                }
                autoOpenFileIfNeed(file = outFile)
                outFile
            } catch (throwable: Throwable) {
                outFile.delete()
                appendLog {
                    val stringWriter = StringWriter()
                    throwable.printStackTrace(PrintWriter(stringWriter, true))
                    stringWriter.buffer.toString()
                }
                null
            }
        }
    }

    private fun buildOutFile(logFile: File): File {
        val logFileName = logFile.nameWithoutExtension
        val outFileName = logFileName + "_" + SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date()) + ".txt"
        return File(logFile.parentFile, outFileName)
    }

    private fun appendLog(log: () -> Any) {
        val mLog = log().toString()
        if (mLog.isNotBlank()) {
            mainPageViewState = mainPageViewState.copy(runtimeLog = mainPageViewState.runtimeLog + mLog + "\n\n")
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
        if (settingsPageViewState.autOpenFileWhenParsingIsSuccessful) {
            openFile(file = file)
        }
    }

    private fun generateKeyPair() {
        viewModelScope.launch {
            val keyPair = DecryptUtils.generateKeyPair()
            cryptKeyPageViewState = cryptKeyPageViewState.copy(
                privateKey = keyPair.privateKey,
                publicKey = keyPair.publicKey
            )
        }
    }

    private fun switchPage(page: Page) {
        mainPageViewState = mainPageViewState.copy(page = page)
    }

    private fun switchTheme(theme: Theme) {
        settingsPageViewState = settingsPageViewState.copy(theme = theme)
        viewModelScope.launch {
            DataStoreManager.updateTheme(theme = theme.type)
        }
    }

    private fun updateAutOpenFileWhenParsingIsSuccessful(autoOpen: Boolean) {
        settingsPageViewState = settingsPageViewState.copy(autOpenFileWhenParsingIsSuccessful = autoOpen)
        viewModelScope.launch {
            DataStoreManager.autOpenFileWhenParsingIsSuccessful(autoOpen = autoOpen)
        }
    }

}