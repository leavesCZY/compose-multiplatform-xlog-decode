import github.leavesczy.xlog.decode.core.LogDecoder
import github.leavesczy.xlog.decode.core.Logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

fun main() {
    val privateKey = "fc0ef8f6f96159c94482acee862379496f93b15b4b9cf7389284f6f1fb5b9a94"
    val zlibNoCryptLogFile = File("core/log/AppednerModeAsync_ZLIB_NoCrypt.xlog")
    decodeSampleFile(logFile = zlibNoCryptLogFile, privateKey = "")
    val zlibHasCryptLogFile = File("core/log/AppednerModeAsync_ZLIB_HasCrypt.xlog")
    decodeSampleFile(logFile = zlibHasCryptLogFile, privateKey = privateKey)
}

private fun decodeSampleFile(logFile: File, privateKey: String) {
    val logDecoder = LogDecoder(logger = object : Logger {
        override fun debug(message: () -> String) {
            println(message())
        }

        override fun error(message: () -> String) {
            println(message())
        }
    })
    val outputFileName =
        logFile.nameWithoutExtension + "_" + SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date()) + ".txt"
    val outputFile = File("core/build/$outputFileName")
    logDecoder.decodeFile(
        privateKey = privateKey,
        logFile = logFile,
        outputFile = outputFile
    )
}
