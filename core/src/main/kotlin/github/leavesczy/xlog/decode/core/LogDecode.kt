package github.leavesczy.xlog.decode.core

import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * @Author: leavesCZY
 * @Date: 2024/6/6 15:46
 * @Desc:
 */
class LogDecode(private val logger: Logger) {

    //magic start(char)
    //seq(uint16_t)
    //begin hour(char)
    //end hour(char)
    //length(uint32_t)
    //crypt key(uint32_t)
    //log
    //magic end(char)
    private sealed class Magic(val byteSize: Int) {
        data object MagicStart : Magic(byteSize = 1)
        data object Seq : Magic(byteSize = 2)
        data object BeginHour : Magic(byteSize = 1)
        data object EndHour : Magic(byteSize = 1)
        data object Length : Magic(byteSize = 4)
        data class CryptKey(val length: Int) : Magic(byteSize = length)
        data object MagicEnd : Magic(byteSize = 1) {
            const val MARK: Byte = 0x00
        }
    }

    private enum class MagicStartMark(val mark: Byte) {
        NoCompressStart(mark = 0x03),
        NoCompressStart1(mark = 0x06),
        NoCompressNoCryptStart(mark = 0x08),

        CompressStart(mark = 0x04),
        CompressStart1(mark = 0x05),
        CompressStart2(mark = 0x07),
        CompressNoCryptStart(mark = 0x09),

        SyncZstdStart(mark = 0x0A),
        SyncNoCryptZstdStart(mark = 0x0B),

        AsyncZstdStart(mark = 0x0C),
        AsyncNoCryptZstdStart(mark = 0x0D)
    }

    private class LogSpace(
        val magicStartMark: MagicStartMark,
        val cryptKeyMagic: Magic.CryptKey,
        val teaKey: ByteArray?,
        val log: ByteArray
    ) {

        override fun toString(): String {
            return "LogSpace(magicStartMark=$magicStartMark, cryptKeyMagic=$cryptKeyMagic, teaKeySize=${teaKey?.size}, logSize=${log.size})"
        }

    }

    private val marksSizeBeforeLengthMagic = Magic.MagicStart.byteSize +
            Magic.Seq.byteSize +
            Magic.BeginHour.byteSize +
            Magic.EndHour.byteSize

    private val marksSizeBeforeCryptKeyMagic = marksSizeBeforeLengthMagic +
            Magic.Length.byteSize

    fun decodeFile(privateKey: String, logFile: File, outFile: File) {
        val logFileInputStream = FileInputStream(logFile)
        val lodFileDataInputStream = DataInputStream(logFileInputStream)
        val outFileBufferedWriter = outFile.outputStream().bufferedWriter()
        try {
            val lofBuffer = ByteArray(lodFileDataInputStream.available())
            lodFileDataInputStream.readFully(lofBuffer)
            var lofBufferOffset = 0
            while (true) {
                val logSpace = decodeLogSpace(
                    privateKey = privateKey,
                    buffer = lofBuffer,
                    offset = lofBufferOffset
                )
                if (logSpace == null) {
                    logger.debug {
                        "finish!!!"
                    }
                    logger.debug {
                        "-----------------------------------------------------------------------"
                    }
                } else {
                    val log = buildString {
                        append("logSpace: $logSpace")
                        append("\n")
                        val teaKey = logSpace.teaKey
                        if (teaKey == null) {
                            append("teaKey: null")
                        } else {
                            append("teaKey: ${StringUtils.byteArrayToHexString(bytes = teaKey)}")
                        }
                    }
                    logger.debug {
                        log
                    }
                }
                if (logSpace == null) {
                    break
                }
                val decryptedDecompressedLog = decodeLogSpace(logSpace = logSpace)
                outFileBufferedWriter.append(String(bytes = decryptedDecompressedLog))
                val logSpaceSize = marksSizeBeforeCryptKeyMagic +
                        logSpace.cryptKeyMagic.byteSize +
                        logSpace.log.size +
                        Magic.MagicEnd.byteSize
                lofBufferOffset += logSpaceSize
            }
            outFileBufferedWriter.flush()
        } finally {
            logFileInputStream.close()
            lodFileDataInputStream.close()
            outFileBufferedWriter.close()
        }
    }

    private fun decodeLogSpace(privateKey: String, buffer: ByteArray, offset: Int): LogSpace? {
        val bufferSize = buffer.size
        if (offset < 0 || offset >= bufferSize) {
            return null
        }
        for (index in offset..<bufferSize) {
            val mark = buffer[index]
            val magicStartMark = MagicStartMark.entries.find { it.mark == mark }
            if (magicStartMark != null) {
                val cryptKeyMagic = decodeCryptKeyMagic(magicStartMark = magicStartMark)
                val cryptKeyMagicByteSize = cryptKeyMagic.byteSize
                val lengthMarkStartIndex = index + marksSizeBeforeLengthMagic
                val lengthMarkEndIndex = lengthMarkStartIndex + Magic.Length.byteSize
                if (lengthMarkEndIndex < bufferSize) {
                    val logByteSize = ByteBuffer.wrap(
                        buffer,
                        lengthMarkStartIndex,
                        lengthMarkEndIndex - lengthMarkStartIndex
                    ).order(ByteOrder.LITTLE_ENDIAN).getInt()
                    val endMarkIndex =
                        index + marksSizeBeforeCryptKeyMagic + cryptKeyMagicByteSize + logByteSize
                    if (endMarkIndex in 0..<bufferSize && buffer[endMarkIndex] == Magic.MagicEnd.MARK) {
                        val logStartIndex =
                            index + marksSizeBeforeCryptKeyMagic + cryptKeyMagicByteSize
                        val logBuffer = buffer.copyOfRange(
                            fromIndex = logStartIndex,
                            toIndex = logStartIndex + logByteSize
                        )
                        val teaKey = if (magicStartMark == MagicStartMark.CompressStart2 ||
                            magicStartMark == MagicStartMark.AsyncZstdStart
                        ) {
                            if (privateKey.isBlank()) {
                                throw IllegalArgumentException("日志有加密，需输入私钥")
                            }
                            decodeTeaKey(
                                privateKey = privateKey,
                                buffer = buffer,
                                magicStartIndex = index,
                                cryptKeyMagic = cryptKeyMagic
                            )
                        } else {
                            null
                        }
                        return LogSpace(
                            magicStartMark = magicStartMark,
                            cryptKeyMagic = cryptKeyMagic,
                            teaKey = teaKey,
                            log = logBuffer
                        )
                    }
                }
            }
        }
        return null
    }

    private fun decodeTeaKey(
        buffer: ByteArray,
        privateKey: String,
        magicStartIndex: Int,
        cryptKeyMagic: Magic.CryptKey
    ): ByteArray {
        val publicKeyBytes = buffer.copyOfRange(
            fromIndex = magicStartIndex + marksSizeBeforeCryptKeyMagic,
            toIndex = magicStartIndex + marksSizeBeforeCryptKeyMagic + cryptKeyMagic.byteSize
        )
        val publicKeyHexString = StringUtils.byteArrayToHexString(bytes = publicKeyBytes)
        val publicKeyHexStringFormatted = String.format("04%s", publicKeyHexString)
        val teaKey = DecryptUtils.getECDHKey(
            publicKey = StringUtils.hexStringToByteArray(hexString = publicKeyHexStringFormatted),
            privateKey = StringUtils.hexStringToByteArray(hexString = privateKey)
        )
        return teaKey
    }

    private fun decodeLogSpace(logSpace: LogSpace): ByteArray {
        return when (val magicStart = logSpace.magicStartMark) {
            MagicStartMark.CompressStart2, MagicStartMark.AsyncZstdStart -> {
                val teaKey = logSpace.teaKey!!
                val decryptedLogBuffer = DecryptUtils.teaDecrypt(
                    encryptedData = logSpace.log,
                    key = teaKey
                )
                if (magicStart == MagicStartMark.CompressStart2) {
                    DecompressUtils.zlibDecompress(data = decryptedLogBuffer)
                } else {
                    DecompressUtils.zstdDecompress(data = decryptedLogBuffer)
                }
            }

            MagicStartMark.AsyncNoCryptZstdStart -> {
                DecompressUtils.zstdDecompress(data = logSpace.log)
            }

            MagicStartMark.CompressStart, MagicStartMark.CompressNoCryptStart -> {
                DecompressUtils.zlibDecompress(data = logSpace.log)
            }

            MagicStartMark.NoCompressStart, MagicStartMark.NoCompressStart1,
            MagicStartMark.NoCompressNoCryptStart, MagicStartMark.CompressStart1,
            MagicStartMark.SyncZstdStart, MagicStartMark.SyncNoCryptZstdStart -> {
                logSpace.log
            }
        }
    }

    private fun decodeCryptKeyMagic(magicStartMark: MagicStartMark): Magic.CryptKey {
        return when (magicStartMark) {
            MagicStartMark.NoCompressStart,
            MagicStartMark.CompressStart,
            MagicStartMark.CompressStart1 -> {
                Magic.CryptKey(length = 4)
            }

            MagicStartMark.NoCompressStart1,
            MagicStartMark.CompressStart2,
            MagicStartMark.NoCompressNoCryptStart,
            MagicStartMark.CompressNoCryptStart,
            MagicStartMark.SyncZstdStart,
            MagicStartMark.SyncNoCryptZstdStart,
            MagicStartMark.AsyncZstdStart,
            MagicStartMark.AsyncNoCryptZstdStart -> {
                Magic.CryptKey(length = 64)
            }
        }
    }

}