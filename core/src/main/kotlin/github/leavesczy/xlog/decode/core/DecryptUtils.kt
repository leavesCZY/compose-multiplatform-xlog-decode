package github.leavesczy.xlog.decode.core

import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Security
import javax.crypto.KeyAgreement

/**
 * @Author: leavesCZY
 * @Date: 2024/6/4 14:17
 * @Desc:
 */
object DecryptUtils {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    data class SecretKey(val privateKey: String, val publicKey: String)

    fun generateKeyPair(): SecretKey {
        val curveParameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC")
        keyPairGenerator.initialize(curveParameterSpec)
        val keyPair = keyPairGenerator.generateKeyPair()
        val privateKey = (keyPair.private as ECPrivateKey).d.toString(16)
        val eCPublicKey = keyPair.public as ECPublicKey
        val publicKey = eCPublicKey.q.rawXCoord.toString() + eCPublicKey.q.rawYCoord.toString()
        return SecretKey(privateKey = privateKey, publicKey = publicKey)
    }

    fun getECDHKey(publicKey: ByteArray, privateKey: ByteArray): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH", "BC")
        keyAgreement.init(loadPrivateKey(data = privateKey))
        keyAgreement.doPhase(loadPublicKey(data = publicKey), true)
        return keyAgreement.generateSecret()
    }

    private fun loadPublicKey(data: ByteArray): PublicKey {
        val parameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val keySpec = ECPublicKeySpec(parameterSpec.curve.decodePoint(data), parameterSpec)
        val keyAgreement = KeyFactory.getInstance("ECDH", "BC")
        return keyAgreement.generatePublic(keySpec)
    }

    private fun loadPrivateKey(data: ByteArray): PrivateKey {
        val parameterSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
        val keySpec = ECPrivateKeySpec(BigInteger(1, data), parameterSpec)
        val keyFactory = KeyFactory.getInstance("ECDH", "BC")
        return keyFactory.generatePrivate(keySpec)
    }

    fun teaDecrypt(encryptedData: ByteArray, key: ByteArray): ByteArray {
        val num = encryptedData.size shr 3 shl 3
        val ret = ByteBuffer.allocate(encryptedData.size).order(ByteOrder.LITTLE_ENDIAN)
        var i = 0
        while (i < num) {
            val sv = ByteArray(8)
            ByteBuffer.wrap(encryptedData, i, 8)[sv]
            val x = teaDecipher(sv, key)
            ret.put(x)
            i += 8
        }
        val remain = ByteArray(encryptedData.size - num)
        ByteBuffer.wrap(encryptedData, num, encryptedData.size - num)[remain]
        ret.put(remain)
        return ret.array()
    }

    private fun longToArray(x: Long, y: Long): ByteArray {
        val buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(x.toInt())
        buffer.putInt(y.toInt())
        return buffer.array()
    }

    private fun bytesToInt(b: ByteArray, offset: Int): Int {
        return b[offset].toInt() and 0xFF or ((b[offset + 1].toInt() and 0xFF) shl 8) or ((b[offset + 2].toInt() and 0xFF) shl 16) or ((b[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun teaDecipher(byteArray: ByteArray, k: ByteArray): ByteArray {
        val op = 0xffffffffL
        val delta = 0x9E3779B9L
        var s = (delta shl 4) and op
        var v0 = bytesToInt(byteArray, 0).toLong() and 0x0FFFFFFFFL
        var v1 = bytesToInt(byteArray, 4).toLong() and 0x0FFFFFFFFL
        val k1 = bytesToInt(k, 0).toLong() and 0x0FFFFFFFFL
        val k2 = bytesToInt(k, 4).toLong() and 0x0FFFFFFFFL
        val k3 = bytesToInt(k, 8).toLong() and 0x0FFFFFFFFL
        val k4 = bytesToInt(k, 12).toLong() and 0x0FFFFFFFFL
        var cnt = 16
        while (cnt > 0) {
            v1 = (v1 - (((v0 shl 4) + k3) xor (v0 + s) xor ((v0 shr 5) + k4))) and op
            v0 = (v0 - (((v1 shl 4) + k1) xor (v1 + s) xor ((v1 shr 5) + k2))) and op
            s = (s - delta) and op
            cnt--
        }
        return longToArray(v0, v1)
    }

}