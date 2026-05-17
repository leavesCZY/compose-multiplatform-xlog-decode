import github.leavesczy.xlog.decode.core.LogDecode
import github.leavesczy.xlog.decode.core.Logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:17
 * @Desc:
 */
fun main() {
    val privateKey = "fc0ef8f6f96159c94482acee862379496f93b15b4b9cf7389284f6f1fb5b9a94"
    val publicKey =
        "9b46d6ff7774fe7502c5e0eb09a2c110a53dabe8daa4b20174abdca1715bb6cd9c2343d12f03dd4258fa3bad4fbd8004deb8578a35f32983ac912cdada41d34b"
    val zlibNoCryptLogFile = File("core/log/AppednerModeAsync_ZLIB_NoCrypt.xlog")
    decodeFile(logFile = zlibNoCryptLogFile, privateKey = "")
    val zlibHasCryptLogFile = File("core/log/AppednerModeAsync_ZLIB_HasCrypt.xlog")
    decodeFile(logFile = zlibHasCryptLogFile, privateKey = privateKey)
}

private fun decodeFile(logFile: File, privateKey: String) {
    val logDecode = LogDecode(logger = object : Logger {
        override fun debug(log: () -> String) {
            println(log())
        }

        override fun error(log: () -> String) {
            println(log())
        }
    })
    val logFileName = logFile.nameWithoutExtension
    val outFileName =
        logFileName + "_" + SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date()) + ".txt"
    val outFile = File("core/build/$outFileName")
    logDecode.decodeFile(
        privateKey = privateKey,
        logFile = logFile,
        outFile = outFile
    )
}