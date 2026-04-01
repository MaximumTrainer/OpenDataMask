package com.opendatamask.config

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StartupSecurityValidatorTest {

    private fun createValidator(jwtSecret: String?, encryptionKey: String?): StartupSecurityValidator {
        return StartupSecurityValidator(jwtSecret ?: "", encryptionKey ?: "")
    }

    @Test
    fun `validate passes when both secrets are set to non-default values`() {
        val validator = createValidator(
            jwtSecret = "a-very-secure-random-jwt-secret-that-is-long-enough",
            encryptionKey = "secure-aes-key-not-default"
        )
        // Should not throw
        validator.validate()
    }

    @Test
    fun `validate throws when JWT_SECRET is blank`() {
        val validator = createValidator(jwtSecret = "", encryptionKey = "secure-key")
        assertThrows<IllegalStateException> { validator.validate() }
    }

    @Test
    fun `validate throws when JWT_SECRET is the insecure default`() {
        val validator = createValidator(
            jwtSecret = "change-this-secret-in-production-must-be-at-least-256-bits-long",
            encryptionKey = "secure-key"
        )
        assertThrows<IllegalStateException> { validator.validate() }
    }

    @Test
    fun `validate throws when ENCRYPTION_KEY is blank`() {
        val validator = createValidator(jwtSecret = "secure-jwt-secret", encryptionKey = "")
        assertThrows<IllegalStateException> { validator.validate() }
    }

    @Test
    fun `validate throws when ENCRYPTION_KEY is the insecure default`() {
        val validator = createValidator(
            jwtSecret = "secure-jwt-secret",
            encryptionKey = "0123456789abcdef"
        )
        assertThrows<IllegalStateException> { validator.validate() }
    }
}
