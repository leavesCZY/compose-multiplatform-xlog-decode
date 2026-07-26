package github.leavesczy.xlog.decode.core

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LogDecoder(private val logger: Logger) {

    // magic start(char)
    // seq(uint16_t)
    // begin hour(char)
    // end hour(char)
    // length(uint32_t)
    // crypt key(char[])
    // log
    // magic end(char)
    private sealed class HeaderField(val byteSize: Int) {
        data object MagicStart : HeaderField(byteSize = 1)
        data object Seq : HeaderField(byteSize = 2)
        data object BeginHour : HeaderField(byteSize = 1)
        data object EndHour : HeaderField(byteSize = 1)
        data object Length : HeaderField(byteSize = 4)
        data class CryptKey(val length: Int) : HeaderField(byteSize = length)
        data object MagicEnd : HeaderField(byteSize = 1) {
            const val MARK: Byte = 0x00
        }
    }

    private enum class StartMark(val mark: Byte) {
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
        AsyncNoCryptZstdStart(mark = 0x0D);

        val isEncrypted: Boolean
            get() = this == CompressStart2 || this == AsyncZstdStart
    }

    private class LogRecord(
        val startMark: StartMark,
        val cryptKeyField: HeaderField.CryptKey,
        val teaKey: ByteArray?,
        val payload: ByteArray
    ) {
        override fun toString(): String {
            return "LogRecord(startMark=$startMark, cryptKeySize=${cryptKeyField.byteSize}, teaKeySize=${teaKey?.size}, payloadSize=${payload.size})"
        }
    }

    private val headerSizeBeforeLength = HeaderField.MagicStart.byteSize +
            HeaderField.Seq.byteSize +
            HeaderField.BeginHour.byteSize +
            HeaderField.EndHour.byteSize

    private val headerSizeBeforeCryptKey = headerSizeBeforeLength + HeaderField.Length.byteSize

    private val startMarkByByte: Array<StartMark?> = run {
        val table = arrayOfNulls<StartMark>(256)
        for (entry in StartMark.entries) {
            table[entry.mark.toInt() and 0xFF] = entry
        }
        table
    }

    private val teaKeyCache = HashMap<String, ByteArray>()

    fun decodeFile(privateKey: String, logFile: File, outputFile: File) {
        require(logFile.isFile) { "Log file does not exist: ${logFile.path}" }
        logger.debug { "start decoding: ${logFile.path}" }
        val fileSize = logFile.length()
        if (fileSize > Int.MAX_VALUE) {
            throw IllegalArgumentException("Log file is too large: ${logFile.path}")
        }
        teaKeyCache.clear()
        val privateKeyBytes = privateKey.trim().takeIf { it.isNotEmpty() }?.let { hex ->
            HexUtils.toByteArray(hex = hex)
        }
        val logBuffer = logFile.readBytes()
        outputFile.parentFile?.mkdirs()
        outputFile.bufferedWriter().use { writer ->
            var offset = 0
            while (true) {
                val record = findNextRecord(
                    privateKeyBytes = privateKeyBytes,
                    buffer = logBuffer,
                    offset = offset
                )
                if (record == null) {
                    logger.debug { "decode successful: ${outputFile.path}" }
                    break
                }
                logger.debug {
                    buildString {
                        append("logRecord: $record")
                        append('\n')
                        val teaKey = record.teaKey
                        if (teaKey == null) {
                            append("teaKey: null")
                        } else {
                            append("teaKey: ${HexUtils.toHexString(bytes = teaKey)}")
                        }
                    }
                }
                val decodedPayload = decodeRecordPayload(record = record)
                writer.append(String(bytes = decodedPayload, charset = Charsets.UTF_8))
                val recordSize = headerSizeBeforeCryptKey +
                        record.cryptKeyField.byteSize +
                        record.payload.size +
                        HeaderField.MagicEnd.byteSize
                offset += recordSize
            }
        }
    }

    private fun findNextRecord(
        privateKeyBytes: ByteArray?,
        buffer: ByteArray,
        offset: Int
    ): LogRecord? {
        val bufferSize = buffer.size
        if (offset !in 0..<bufferSize) {
            return null
        }
        for (index in offset..<bufferSize) {
            val startMark = startMarkByByte[buffer[index].toInt() and 0xFF] ?: continue
            val cryptKeyField = cryptKeyFieldFor(startMark = startMark)
            val cryptKeyByteSize = cryptKeyField.byteSize
            val lengthStartIndex = index + headerSizeBeforeLength
            val lengthEndIndex = lengthStartIndex + HeaderField.Length.byteSize
            if (lengthEndIndex >= bufferSize) {
                continue
            }
            val payloadSize = ByteBuffer.wrap(
                buffer,
                lengthStartIndex,
                HeaderField.Length.byteSize
            ).order(ByteOrder.LITTLE_ENDIAN).int
            if (payloadSize < 0) {
                continue
            }
            val endMarkIndex = index.toLong() +
                    headerSizeBeforeCryptKey +
                    cryptKeyByteSize +
                    payloadSize
            if (endMarkIndex >= bufferSize) {
                continue
            }
            val endMarkIndexInt = endMarkIndex.toInt()
            if (buffer[endMarkIndexInt] != HeaderField.MagicEnd.MARK) {
                continue
            }
            val payloadStartIndex = index + headerSizeBeforeCryptKey + cryptKeyByteSize
            val payload = buffer.copyOfRange(
                fromIndex = payloadStartIndex,
                toIndex = payloadStartIndex + payloadSize
            )
            val teaKey = if (startMark.isEncrypted) {
                if (privateKeyBytes == null) {
                    throw IllegalArgumentException("the log is encrypted and the private key needs to be entered")
                }
                resolveTeaKey(
                    privateKeyBytes = privateKeyBytes,
                    buffer = buffer,
                    recordStartIndex = index,
                    cryptKeyField = cryptKeyField
                )
            } else {
                null
            }
            return LogRecord(
                startMark = startMark,
                cryptKeyField = cryptKeyField,
                teaKey = teaKey,
                payload = payload
            )
        }
        return null
    }

    private fun resolveTeaKey(
        buffer: ByteArray,
        privateKeyBytes: ByteArray,
        recordStartIndex: Int,
        cryptKeyField: HeaderField.CryptKey
    ): ByteArray {
        val publicKeyStartIndex = recordStartIndex + headerSizeBeforeCryptKey
        val publicKeyByteSize = cryptKeyField.byteSize
        val publicKeyBytes = buffer.copyOfRange(
            fromIndex = publicKeyStartIndex,
            toIndex = publicKeyStartIndex + publicKeyByteSize
        )
        val cacheKey = HexUtils.toHexString(bytes = publicKeyBytes)
        teaKeyCache[cacheKey]?.let { return it }
        val uncompressedPublicKey = ByteArray(size = 1 + publicKeyByteSize)
        uncompressedPublicKey[0] = 0x04
        System.arraycopy(publicKeyBytes, 0, uncompressedPublicKey, 1, publicKeyByteSize)
        val teaKey = CryptoUtils.computeEcdhSharedKey(
            publicKey = uncompressedPublicKey,
            privateKey = privateKeyBytes
        )
        teaKeyCache[cacheKey] = teaKey
        return teaKey
    }

    private fun decodeRecordPayload(record: LogRecord): ByteArray {
        return when (val startMark = record.startMark) {
            StartMark.CompressStart2, StartMark.AsyncZstdStart -> {
                val teaKey = checkNotNull(record.teaKey) {
                    "missing TEA key for encrypted record: $startMark"
                }
                val decryptedPayload = CryptoUtils.teaDecrypt(
                    encryptedData = record.payload,
                    key = teaKey
                )
                if (startMark == StartMark.CompressStart2) {
                    DecompressUtils.decompressZlib(data = decryptedPayload)
                } else {
                    DecompressUtils.decompressZstd(data = decryptedPayload)
                }
            }

            StartMark.AsyncNoCryptZstdStart -> {
                DecompressUtils.decompressZstd(data = record.payload)
            }

            StartMark.CompressStart, StartMark.CompressNoCryptStart -> {
                DecompressUtils.decompressZlib(data = record.payload)
            }

            StartMark.NoCompressStart,
            StartMark.NoCompressStart1,
            StartMark.NoCompressNoCryptStart,
            StartMark.CompressStart1,
            StartMark.SyncZstdStart,
            StartMark.SyncNoCryptZstdStart -> {
                record.payload
            }
        }
    }

    private fun cryptKeyFieldFor(startMark: StartMark): HeaderField.CryptKey {
        return when (startMark) {
            StartMark.NoCompressStart,
            StartMark.CompressStart,
            StartMark.CompressStart1 -> {
                HeaderField.CryptKey(length = 4)
            }

            StartMark.NoCompressStart1,
            StartMark.CompressStart2,
            StartMark.NoCompressNoCryptStart,
            StartMark.CompressNoCryptStart,
            StartMark.SyncZstdStart,
            StartMark.SyncNoCryptZstdStart,
            StartMark.AsyncZstdStart,
            StartMark.AsyncNoCryptZstdStart -> {
                HeaderField.CryptKey(length = 64)
            }
        }
    }

}