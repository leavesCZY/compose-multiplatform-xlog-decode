import github.leavesczy.xlog.decode.core.LogDecode
import github.leavesczy.xlog.decode.core.Logger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:17
 * @Desc:
 */
fun main() {
    val privateKey = "a763761dabcadf7762cc1dc569e0d64eec757fdedd0aecc02817d5693ac83d74"
    val publicKey =
        "9208edf99ad9825d75c14d88bcc39e3c53c2a2bea193e20d5b3a0a933b6eb4b44dec5757aad56b754c3cb672981d893f3b12222c9c1573740322ad9dc62dd332"

    val zlibHasCryptLogFile = File("core/log/AppednerModeAsync_ZLIB_HasCrypt.xlog")
    decodeFile(logFile = zlibHasCryptLogFile, privateKey = privateKey)

    val zlibNoCryptLogFile = File("core/log/AppednerModeAsync_ZLIB_NoCrypt.xlog")
    decodeFile(logFile = zlibNoCryptLogFile, privateKey = "")
}

private fun decodeFile(logFile: File, privateKey: String) {
    val logDecode = LogDecode(logger = object : Logger {
        override fun debug(log: () -> Any) {
            println(log().toString())
        }

        override fun error(log: () -> Any) {
            println(log().toString())
        }
    })
    val logFileName = logFile.nameWithoutExtension
    val outFileName = logFileName + "_" + SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date()) + ".txt"
    val outFile = File("core/build/$outFileName")
    logDecode.decodeFile(
        privateKey = privateKey,
        logFile = logFile,
        outFile = outFile
    )
}