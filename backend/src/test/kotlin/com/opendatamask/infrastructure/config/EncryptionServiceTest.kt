package com.opendatamask.infrastructure.config

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EncryptionServiceTest {

    private lateinit var encryptionService: EncryptionService

    @BeforeEach
    fun setup() {
        encryptionService = EncryptionService("0123456789abcdef")
    }

    @Test
    fun `encrypt returns non-blank ciphertext`() {
        val ciphertext = encryptionService.encrypt("hello world")
        assertNotNull(ciphertext)
        assertTrue(ciphertext.isNotBlank())
    }

    @Test
    fun `decrypt reverses encrypt for plain text`() {
        val original = "my secret value"
        val ciphertext = encryptionService.encrypt(original)
        assertEquals(original, encryptionService.decrypt(ciphertext))
    }

    @Test
    fun `encrypt produces different ciphertext on each call due to random IV`() {
        val plaintext = "same input"
        val first = encryptionService.encrypt(plaintext)
        val second = encryptionService.encrypt(plaintext)
        assertNotEquals(first, second)
    }

    @Test
    fun `encrypt and decrypt handle empty string`() {
        val ciphertext = encryptionService.encrypt("")
        assertEquals("", encryptionService.decrypt(ciphertext))
    }

    @Test
    fun `encrypt and decrypt handle special characters`() {
        val original = "p@ssw0rd!#\$%^&*()"
        assertEquals(original, encryptionService.decrypt(encryptionService.encrypt(original)))
    }

    @Test
    fun `encrypt and decrypt handle unicode`() {
        val original = "こんにちは世界"
        assertEquals(original, encryptionService.decrypt(encryptionService.encrypt(original)))
    }
}
