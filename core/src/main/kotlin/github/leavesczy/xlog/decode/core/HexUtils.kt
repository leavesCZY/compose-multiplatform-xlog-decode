package github.leavesczy.xlog.decode.core

internal object HexUtils {

    fun toByteArray(hex: String): ByteArray {
        val normalized = hex.trim()
        require(normalized.length % 2 == 0) {
            "Hex string length must be even, actual=${normalized.length}"
        }
        val byteCount = normalized.length / 2
        val bytes = ByteArray(byteCount)
        for (index in 0 until byteCount) {
            val start = index * 2
            val hexByte = normalized.substring(start, start + 2)
            bytes[index] = hexByte.toIntOrNull(radix = 16)?.toByte()
                ?: throw IllegalArgumentException("Invalid hex byte: $hexByte")
        }
        return bytes
    }

    fun toHexString(bytes: ByteArray): String {
        return buildString(capacity = bytes.size * 2) {
            for (byte in bytes) {
                val hex = Integer.toHexString(byte.toInt() and 0xFF)
                if (hex.length < 2) {
                    append('0')
                }
                append(hex)
            }
        }
    }

}