package github.leavesczy.xlog.decode.core

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import javax.crypto.KeyAgreement

object CryptoUtils {

    private const val EC_CURVE_NAME = "secp256k1"
    private const val ECDH_ALGORITHM = "ECDH"
    private const val BOUNCY_CASTLE_PROVIDER = "BC"
    private const val TEA_ROUNDS = 16
    private const val TEA_DELTA = 0x9E3779B9L
    private const val UINT32_MASK = 0xFFFFFFFFL
    private const val COORDINATE_HEX_LENGTH = 64

    init {
        if (Security.getProvider(BOUNCY_CASTLE_PROVIDER) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    data class EcdhKeyPair(
        val privateKey: String,
        val publicKey: String
    )

    fun generateKeyPair(): EcdhKeyPair {
        val curveParameterSpec = ECNamedCurveTable.getParameterSpec(EC_CURVE_NAME)
        val keyPairGenerator = KeyPairGenerator.getInstance(ECDH_ALGORITHM, BOUNCY_CASTLE_PROVIDER)
        keyPairGenerator.initialize(curveParameterSpec)
        val keyPair = keyPairGenerator.generateKeyPair()
        val privateKey = padCoordinateHex(
            hex = (keyPair.private as ECPrivateKey).d.toString(16)
        )
        val ecPublicKey = keyPair.public as ECPublicKey
        val publicKey = padCoordinateHex(hex = ecPublicKey.q.rawXCoord.toString()) +
                padCoordinateHex(hex = ecPublicKey.q.rawYCoord.toString())
        return EcdhKeyPair(privateKey = privateKey, publicKey = publicKey)
    }

    fun computeEcdhSharedKey(publicKey: ByteArray, privateKey: ByteArray): ByteArray {
        require(publicKey.isNotEmpty()) { "publicKey must not be empty" }
        require(privateKey.isNotEmpty()) { "privateKey must not be empty" }
        val keyAgreement = KeyAgreement.getInstance(ECDH_ALGORITHM, BOUNCY_CASTLE_PROVIDER)
        keyAgreement.init(loadPrivateKey(privateKeyBytes = privateKey))
        keyAgreement.doPhase(loadPublicKey(publicKeyBytes = publicKey), true)
        return keyAgreement.generateSecret()
    }

    fun teaDecrypt(encryptedData: ByteArray, key: ByteArray): ByteArray {
        require(key.size >= 16) { "TEA key must contain at least 16 bytes, actual=${key.size}" }
        val alignedLength = encryptedData.size and 7.inv()
        val decrypted = ByteArray(encryptedData.size)
        val key0 = readUInt32LittleEndian(bytes = key, offset = 0)
        val key1 = readUInt32LittleEndian(bytes = key, offset = 4)
        val key2 = readUInt32LittleEndian(bytes = key, offset = 8)
        val key3 = readUInt32LittleEndian(bytes = key, offset = 12)
        var offset = 0
        while (offset < alignedLength) {
            decryptTeaBlock(
                source = encryptedData,
                sourceOffset = offset,
                destination = decrypted,
                destinationOffset = offset,
                key0 = key0,
                key1 = key1,
                key2 = key2,
                key3 = key3
            )
            offset += 8
        }
        if (alignedLength < encryptedData.size) {
            System.arraycopy(
                encryptedData,
                alignedLength,
                decrypted,
                alignedLength,
                encryptedData.size - alignedLength
            )
        }
        return decrypted
    }

    private fun padCoordinateHex(hex: String): String {
        val normalized = hex.lowercase().removePrefix("0x")
        return if (normalized.length >= COORDINATE_HEX_LENGTH) {
            normalized.takeLast(n = COORDINATE_HEX_LENGTH)
        } else {
            normalized.padStart(length = COORDINATE_HEX_LENGTH, padChar = '0')
        }
    }

    private fun loadPublicKey(publicKeyBytes: ByteArray): PublicKey {
        val parameterSpec = ECNamedCurveTable.getParameterSpec(EC_CURVE_NAME)
        val keySpec = ECPublicKeySpec(
            parameterSpec.curve.decodePoint(publicKeyBytes),
            parameterSpec
        )
        val keyFactory = KeyFactory.getInstance(ECDH_ALGORITHM, BOUNCY_CASTLE_PROVIDER)
        return keyFactory.generatePublic(keySpec)
    }

    private fun loadPrivateKey(privateKeyBytes: ByteArray): PrivateKey {
        val parameterSpec = ECNamedCurveTable.getParameterSpec(EC_CURVE_NAME)
        val keySpec = ECPrivateKeySpec(BigInteger(1, privateKeyBytes), parameterSpec)
        val keyFactory = KeyFactory.getInstance(ECDH_ALGORITHM, BOUNCY_CASTLE_PROVIDER)
        return keyFactory.generatePrivate(keySpec)
    }

    private fun readUInt32LittleEndian(bytes: ByteArray, offset: Int): Long {
        return ((bytes[offset].toInt() and 0xFF) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 3].toInt() and 0xFF) shl 24)
                ).toLong() and UINT32_MASK
    }

    private fun writeIntLittleEndian(destination: ByteArray, offset: Int, value: Int) {
        destination[offset] = value.toByte()
        destination[offset + 1] = (value ushr 8).toByte()
        destination[offset + 2] = (value ushr 16).toByte()
        destination[offset + 3] = (value ushr 24).toByte()
    }

    private fun decryptTeaBlock(
        source: ByteArray,
        sourceOffset: Int,
        destination: ByteArray,
        destinationOffset: Int,
        key0: Long,
        key1: Long,
        key2: Long,
        key3: Long
    ) {
        var sum = (TEA_DELTA shl 4) and UINT32_MASK
        var value0 = readUInt32LittleEndian(bytes = source, offset = sourceOffset)
        var value1 = readUInt32LittleEndian(bytes = source, offset = sourceOffset + 4)
        repeat(TEA_ROUNDS) {
            value1 =
                (value1 - (((value0 shl 4) + key2) xor (value0 + sum) xor ((value0 shr 5) + key3))) and UINT32_MASK
            value0 =
                (value0 - (((value1 shl 4) + key0) xor (value1 + sum) xor ((value1 shr 5) + key1))) and UINT32_MASK
            sum = (sum - TEA_DELTA) and UINT32_MASK
        }
        writeIntLittleEndian(
            destination = destination,
            offset = destinationOffset,
            value = value0.toInt()
        )
        writeIntLittleEndian(
            destination = destination,
            offset = destinationOffset + 4,
            value = value1.toInt()
        )
    }

}