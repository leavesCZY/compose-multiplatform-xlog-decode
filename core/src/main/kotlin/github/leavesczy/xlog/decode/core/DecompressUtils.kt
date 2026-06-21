package github.leavesczy.xlog.decode.core

import com.github.luben.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Inflater
import java.util.zip.InflaterOutputStream

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:16
 * @Desc:
 */
internal object DecompressUtils {

    fun zlibDecompress(data: ByteArray): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream = InflaterOutputStream(byteArrayOutputStream, Inflater(true))
        outputStream.write(data)
        outputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    fun zstdDecompress(data: ByteArray): ByteArray {
        ByteArrayInputStream(data).use { input ->
            ZstdInputStream(input).use { zstdInputStream ->
                ByteArrayOutputStream().use { output ->
                    val buffer = ByteArray(size = 8192)
                    var bytesRead: Int
                    while (zstdInputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                    return output.toByteArray()
                }
            }
        }
    }

}