package com.opendatamask.infrastructure.config

import com.opendatamask.domain.port.output.EncryptionPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val IV_SIZE = 16

@Component
class EncryptionService(
    @Value("\${opendatamask.encryption.key}")
    private val encryptionKey: String
) : EncryptionPort {
    private val algorithm = "AES/CBC/PKCS5Padding"
    private val keyAlgorithm = "AES"
    private val secureRandom = SecureRandom()

    private fun getKey(): SecretKeySpec {
        val keyBytes = encryptionKey.toByteArray(Charsets.UTF_8).copyOf(16)
        return SecretKeySpec(keyBytes, keyAlgorithm)
    }

    /**
     * Encrypts [text] using AES/CBC with a randomly generated IV.
     * The IV is prepended to the ciphertext before Base64 encoding so it can be
     * recovered during decryption without requiring separate storage.
     */
    override fun encrypt(plain: String): String {
        val iv = ByteArray(IV_SIZE).also { secureRandom.nextBytes(it) }
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, getKey(), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        // Prepend IV to ciphertext: [iv (16 bytes)][ciphertext]
        val combined = iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts a Base64-encoded value produced by [encrypt].
     * Extracts the IV from the first [IV_SIZE] bytes, then decrypts the remainder.
     */
    override fun decrypt(encrypted: String): String {
        val combined = Base64.getDecoder().decode(encrypted)
        val iv = combined.copyOfRange(0, IV_SIZE)
        val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
