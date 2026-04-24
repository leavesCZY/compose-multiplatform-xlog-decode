package github.leavesczy.xlog.decode.logic

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.xlog.decode.core.DecryptUtils
import github.leavesczy.xlog.decode.core.LogDecode
import github.leavesczy.xlog.decode.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Desktop.Action
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:16
 * @Desc:
 */
class LogDecodeViewModel :
    ViewModel(viewModelScope = CoroutineScope(context = SupervisorJob() + Dispatchers.Default)) {

    var mainPageViewState by mutableStateOf(
        value = MainPageViewState(
            page = Page.Decryption,
            switchPage = ::switchPage
        )
    )
        private set

    var decryptionPageViewState by mutableStateOf(
        value = DecryptionPageViewState(
            privateKey = "",
            selectedLogFiles = emptyList(),
            runtimeLog = "",
            logScrollState = ScrollState(initial = 0),
            onInputPrivateKey = ::onInputPrivateKey,
            onLogFileIsSelected = ::onLogFileIsSelected,
            decodeLog = ::decodeLog,
            openFile = ::openFile
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
        val autOpenFileWhenParsingIsSuccessful =
            DataStoreManager.autoOpenFileWhenParsingIsSuccessful().first()
        decryptionPageViewState = decryptionPageViewState.copy(privateKey = privateKey)
        settingsPageViewState = settingsPageViewState.copy(
            theme = theme,
            autoOpenFileWhenParsingIsSuccessful = autOpenFileWhenParsingIsSuccessful
        )
    }

    private fun onInputPrivateKey(privateKey: String) {
        viewModelScope.launch {
            decryptionPageViewState = decryptionPageViewState.copy(privateKey = privateKey)
            DataStoreManager.updatePrivateKey(privateKey = privateKey)
        }
    }

    private fun onLogFileIsSelected(logPathList: List<String>) {
        decryptionPageViewState = decryptionPageViewState.copy(selectedLogFiles = logPathList)
    }

    private suspend fun decodeLog(): List<File> {
        return withContext(context = Dispatchers.Default) {
            val viewState = decryptionPageViewState
            val selectedLogFiles = viewState.selectedLogFiles
            val result = mutableListOf<File>()
            selectedLogFiles.forEach {
                val logFile = File(it)
                val outFile = buildOutFile(logFile = logFile)
                try {
                    logDecode.decodeFile(
                        privateKey = viewState.privateKey,
                        logFile = logFile,
                        outFile = outFile
                    )
                    result.add(element = outFile)
                } catch (throwable: Throwable) {
                    outFile.delete()
                    appendLog {
                        val stringWriter = StringWriter()
                        throwable.printStackTrace(PrintWriter(stringWriter, true))
                        stringWriter.toString()
                    }
                }
                appendLog {
                    "-----------------------------------------------------------------------"
                }
            }
            if (result.isNotEmpty()) {
                autoOpenFileIfNeed(fileList = result)
            }
            result
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
            val viewState = decryptionPageViewState
            decryptionPageViewState =
                viewState.copy(runtimeLog = viewState.runtimeLog + mLog + "\n\n")
        }
    }

    private suspend fun autoOpenFileIfNeed(fileList: List<File>) {
        if (settingsPageViewState.autoOpenFileWhenParsingIsSuccessful) {
            openFile(fileList = fileList)
        }
    }

    private suspend fun openFile(fileList: List<File>) {
        withContext(context = Dispatchers.Default) {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Action.OPEN)) {
                    fileList.forEach {
                        desktop.open(it)
                    }
                }
            }
        }
    }

    private fun generateTheKeyPair() {
        viewModelScope.launch(context = Dispatchers.Default) {
            val keyPair = DecryptUtils.generateKeyPair()
            secretKeyPageViewState = secretKeyPageViewState.copy(
                privateKey = keyPair.privateKey,
                publicKey = keyPair.publicKey
            )
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