package github.leavesczy.xlog.decode.core

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:17
 * @Desc:
 */
internal object StringUtils {

    fun hexStringToByteArray(hexString: String): ByteArray {
        val len = hexString.length / 2
        val bytes = ByteArray(len)
        for (i in 0 until len) {
            val byte = hexString.substring(i * 2, (i + 1) * 2).toInt(16)
            bytes[i] = byte.toByte()
        }
        return bytes
    }

    fun byteArrayToHexString(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            val str = Integer.toHexString(b.toInt() and 0xFF)
            if (str.length < 2) {
                sb.append(0)
            }
            sb.append(str)
        }
        return sb.toString()
    }

}