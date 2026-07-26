package github.leavesczy.xlog.decode.core

import com.github.luben.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream

internal object DecompressUtils {

    private const val ZSTD_READ_BUFFER_SIZE = 8192

    fun decompressZlib(data: ByteArray): ByteArray {
        val inflater = Inflater(true)
        return try {
            val output = ByteArrayOutputStream(data.size.coerceAtLeast(32))
            InflaterOutputStream(output, inflater).use { stream ->
                stream.write(data)
            }
            output.toByteArray()
        } finally {
            inflater.end()
        }
    }

    fun decompressZstd(data: ByteArray): ByteArray {
        ByteArrayInputStream(data).use { input ->
            ZstdInputStream(input).use { zstdInput ->
                ByteArrayOutputStream(data.size.coerceAtLeast(32)).use { output ->
                    val buffer = ByteArray(size = ZSTD_READ_BUFFER_SIZE)
                    while (true) {
                        val bytesRead = zstdInput.read(buffer)
                        if (bytesRead < 0) {
                            break
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                    return output.toByteArray()
                }
            }
        }
    }

}