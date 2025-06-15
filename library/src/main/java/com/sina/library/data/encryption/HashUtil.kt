package com.sina.library.data.encryption

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object HashUtil { // Changed to object for utility functions, similar to static methods in Java

    // SHA1
    fun sha1(input: String): String? { // Return type is nullable as in original Java code
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(input.toByteArray(StandardCharsets.UTF_8)) // Specify Charset
            val sb = StringBuilder()
            for (byte in bytes) {
                sb.append(String.format("%02x", byte)) // More idiomatic way to format hex
            }
            sb.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null // Return null on exception, as original code would implicitly do
        }
    }

    // SHA256
    fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(input.toByteArray(StandardCharsets.UTF_8))
            val hexString = StringBuilder(2 * hash.size) // Pre-allocate for efficiency
            for (byte in hash) {
                val hex = Integer.toHexString(0xff and byte.toInt())
                if (hex.length == 1) {
                    hexString.append('0')
                }
                hexString.append(hex)
            }
            hexString.toString()
        } catch (ex: Exception) {
            // Consider more specific exception handling or re-throwing a custom exception
            throw RuntimeException(ex)
        }
    }

    // MD5
    fun md5(input: String): String {
        val md5 = "MD5"
        return try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance(md5)
            digest.update(input.toByteArray(StandardCharsets.UTF_8)) // Specify Charset
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuilder()
            for (byte in messageDigest) {
                // Convert byte to hex string (ensuring 2 digits with leading zero if needed)
                val hex = String.format("%02x", byte)
                hexString.append(hex)
            }
            hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            "" // Return empty string on exception, as in original code
        }
    }
}