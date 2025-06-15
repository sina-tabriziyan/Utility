/**
 * Created by ST on 6/10/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.library.data.encryption

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 16
    private const val SECRET_KEY = "your_secret_key" // This should be securely generated and stored

    fun encrypt(data: String): String {
        val key = getKey(SECRET_KEY)
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = generateIv()
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val cipherText = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv.iv + cipherText, Base64.DEFAULT) // Concatenate IV + cipherText
    }

    fun decrypt(data: String): String {
        val decodedData = Base64.decode(data, Base64.DEFAULT)
        val iv = IvParameterSpec(decodedData.copyOfRange(0, IV_SIZE)) // Extract IV
        val cipherText = decodedData.copyOfRange(IV_SIZE, decodedData.size) // Extract cipherText
        val key = getKey(SECRET_KEY)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)

        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun getKey(secret: String): SecretKeySpec {
        val keyBytes = secret.toByteArray(Charsets.UTF_8).copyOf(KEY_SIZE / 8)
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun generateIv(): IvParameterSpec {
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        return IvParameterSpec(iv)
    }
}
