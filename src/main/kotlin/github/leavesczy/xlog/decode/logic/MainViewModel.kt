package github.leavesczy.xlog.decode.logic

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import github.leavesczy.xlog.decode.core.CryptoUtils
import github.leavesczy.xlog.decode.core.LogDecoder
import github.leavesczy.xlog.decode.core.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Desktop.Action
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicLong

class MainViewModel : ViewModel() {

    companion object {
        private const val RUNTIME_LOG_MAX_SIZE = 500
        private const val RUNTIME_LOG_FLUSH_INTERVAL_MS = 100L
        private const val PRIVATE_KEY_PERSIST_DEBOUNCE_MS = 400L
        private const val OUTPUT_FILE_TIME_PATTERN = "yyyyMMdd_HHmmss_SSS"
        private const val LOG_SEPARATOR =
            "-----------------------------------------------------------------------"
    }

    var mainPageViewState by mutableStateOf(
        value = MainPageViewState(
            page = Page.Decode,
            onPageSelected = ::onPageSelected
        )
    )
        private set

    var decodePageViewState by mutableStateOf(
        value = DecodePageViewState(
            privateKey = "",
            selectedLogFiles = emptyList(),
            runtimeLogs = emptyList(),
            lazyListState = LazyListState(),
            onPrivateKeyChanged = ::onPrivateKeyChanged,
            onLogFilesSelected = ::onLogFilesSelected,
            onDecodeLogs = ::decodeLogs,
            onOpenFiles = ::openFiles
        )
    )
        private set

    var secretKeyPageViewState by mutableStateOf(
        value = SecretKeyPageViewState(
            privateKey = "",
            publicKey = "",
            onGenerateKeyPair = ::onGenerateKeyPair
        )
    )
        private set

    var settingsPageViewState by mutableStateOf(
        value = SettingsPageViewState(
            themeMode = ThemeMode.Light,
            onThemeModeSelected = ::onThemeModeSelected,
            autoOpenOnSuccess = false,
            onAutoOpenOnSuccessChanged = ::onAutoOpenOnSuccessChanged
        )
    )
        private set

    private val pendingRuntimeLogs = mutableListOf<RuntimeLogEntry>()

    private val runtimeLogIdGenerator = AtomicLong(0)

    private var privateKeyPersistJob: Job? = null

    private val logDecoder = LogDecoder(logger = object : Logger {
        override fun debug(message: () -> String) {
            enqueueRuntimeLog(message = message)
        }

        override fun error(message: () -> String) {
            enqueueRuntimeLog(message = message)
        }
    })

    init {
        viewModelScope.launch {
            restorePersistedState()
        }
        viewModelScope.launch {
            while (isActive) {
                delay(timeMillis = RUNTIME_LOG_FLUSH_INTERVAL_MS)
                flushPendingRuntimeLogs()
            }
        }
    }

    private suspend fun restorePersistedState() {
        val privateKey = DataStoreManager.privateKeyFlow().first()
        val themeId = DataStoreManager.themeIdFlow().first()
        val themeMode = ThemeMode.fromId(id = themeId) ?: settingsPageViewState.themeMode
        val autoOpenOnSuccess = DataStoreManager.autoOpenOnSuccessFlow().first()
        decodePageViewState = decodePageViewState.copy(privateKey = privateKey)
        settingsPageViewState = settingsPageViewState.copy(
            themeMode = themeMode,
            autoOpenOnSuccess = autoOpenOnSuccess
        )
    }

    private fun onPrivateKeyChanged(privateKey: String) {
        decodePageViewState = decodePageViewState.copy(privateKey = privateKey)
        privateKeyPersistJob?.cancel()
        privateKeyPersistJob = viewModelScope.launch {
            delay(timeMillis = PRIVATE_KEY_PERSIST_DEBOUNCE_MS)
            DataStoreManager.updatePrivateKey(privateKey = privateKey)
        }
    }

    private fun onLogFilesSelected(logFilePaths: List<String>) {
        decodePageViewState = decodePageViewState.copy(selectedLogFiles = logFilePaths)
    }

    private suspend fun decodeLogs(): List<File> {
        val pageState = decodePageViewState
        val outputFiles = mutableListOf<File>()
        withContext(context = Dispatchers.Default) {
            pageState.selectedLogFiles.forEach { logFilePath ->
                val logFile = File(logFilePath)
                val outputFile = buildOutputFile(logFile = logFile)
                try {
                    logDecoder.decodeFile(
                        privateKey = pageState.privateKey,
                        logFile = logFile,
                        outputFile = outputFile
                    )
                    outputFiles.add(element = outputFile)
                } catch (cancellationException: CancellationException) {
                    outputFile.delete()
                    throw cancellationException
                } catch (exception: Exception) {
                    outputFile.delete()
                    enqueueRuntimeLog {
                        val stringWriter = StringWriter()
                        exception.printStackTrace(PrintWriter(stringWriter, true))
                        stringWriter.toString()
                    }
                }
                enqueueRuntimeLog { LOG_SEPARATOR }
            }
        }
        flushPendingRuntimeLogs()
        if (outputFiles.isNotEmpty()) {
            autoOpenFilesIfNeeded(files = outputFiles)
        }
        return outputFiles
    }

    private fun buildOutputFile(logFile: File): File {
        val timestamp = SimpleDateFormat(OUTPUT_FILE_TIME_PATTERN).format(Date())
        return File(
            logFile.parentFile,
            "${logFile.nameWithoutExtension}_$timestamp.txt"
        )
    }

    private fun enqueueRuntimeLog(message: () -> String) {
        val logText = message()
        if (logText.isNotBlank()) {
            val entry = RuntimeLogEntry(
                id = runtimeLogIdGenerator.incrementAndGet(),
                message = logText
            )
            synchronized(pendingRuntimeLogs) {
                pendingRuntimeLogs.add(entry)
            }
        }
    }

    private fun flushPendingRuntimeLogs() {
        val drained = synchronized(pendingRuntimeLogs) {
            if (pendingRuntimeLogs.isEmpty()) {
                return
            }
            val copy = pendingRuntimeLogs.toList()
            pendingRuntimeLogs.clear()
            copy
        }
        val pageState = decodePageViewState
        val mergedLogs = pageState.runtimeLogs + drained
        val trimmedLogs = if (mergedLogs.size > RUNTIME_LOG_MAX_SIZE) {
            mergedLogs.takeLast(n = RUNTIME_LOG_MAX_SIZE / 2)
        } else {
            mergedLogs
        }
        decodePageViewState = pageState.copy(runtimeLogs = trimmedLogs)
    }

    private suspend fun autoOpenFilesIfNeeded(files: List<File>) {
        if (settingsPageViewState.autoOpenOnSuccess) {
            openFiles(files = files)
        }
    }

    private suspend fun openFiles(files: List<File>) {
        withContext(context = Dispatchers.IO) {
            if (!Desktop.isDesktopSupported()) {
                return@withContext
            }
            val desktop = Desktop.getDesktop()
            if (!desktop.isSupported(Action.OPEN)) {
                return@withContext
            }
            files.forEach { file ->
                if (file.isFile) {
                    desktop.open(file)
                }
            }
        }
    }

    private fun onGenerateKeyPair() {
        viewModelScope.launch {
            val keyPair = withContext(context = Dispatchers.Default) {
                CryptoUtils.generateKeyPair()
            }
            secretKeyPageViewState = secretKeyPageViewState.copy(
                privateKey = keyPair.privateKey,
                publicKey = keyPair.publicKey
            )
        }
    }

    private fun onPageSelected(page: Page) {
        mainPageViewState = mainPageViewState.copy(page = page)
    }

    private fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsPageViewState = settingsPageViewState.copy(themeMode = themeMode)
            DataStoreManager.updateThemeId(themeId = themeMode.id)
        }
    }

    private fun onAutoOpenOnSuccessChanged(autoOpen: Boolean) {
        viewModelScope.launch {
            settingsPageViewState = settingsPageViewState.copy(autoOpenOnSuccess = autoOpen)
            DataStoreManager.updateAutoOpenOnSuccess(autoOpen = autoOpen)
        }
    }

}
