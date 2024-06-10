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
        val byteArrayInputStream = ByteArrayInputStream(data)
        val byteArrayOutputStream = ByteArrayOutputStream()
        val zstdInputStream = ZstdInputStream(byteArrayInputStream)
        val bytes = ByteArray(1000000)
        val bytesRead = zstdInputStream.read(bytes, 0, 1000000)
        byteArrayOutputStream.write(bytes, 0, bytesRead)
        zstdInputStream.close()
        byteArrayInputStream.close()
        byteArrayOutputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

}