package com.opendatamask.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import jakarta.annotation.PostConstruct

private val INSECURE_JWT_DEFAULTS = setOf(
    "change-this-secret-in-production-must-be-at-least-256-bits-long"
)

private val INSECURE_ENCRYPTION_DEFAULTS = setOf(
    "0123456789abcdef"
)

@Component
@Profile("!test")
class StartupSecurityValidator(
    @Value("\${opendatamask.jwt.secret:}") val jwtSecret: String,
    @Value("\${opendatamask.encryption.key:}") val encryptionKey: String
) {
    private val logger = LoggerFactory.getLogger(StartupSecurityValidator::class.java)

    @PostConstruct
    fun validateOnStartup() = validate()

    fun validate() {
        val errors = mutableListOf<String>()

        if (jwtSecret.isBlank()) {
            errors += "JWT_SECRET environment variable must be set. Generate with: openssl rand -base64 32"
        } else if (jwtSecret in INSECURE_JWT_DEFAULTS) {
            errors += "JWT_SECRET is an insecure default. Generate with: openssl rand -base64 32"
        }

        if (encryptionKey.isBlank()) {
            errors += "ENCRYPTION_KEY environment variable must be set. Generate with: openssl rand -base64 32"
        } else if (encryptionKey in INSECURE_ENCRYPTION_DEFAULTS) {
            errors += "ENCRYPTION_KEY is an insecure default. Generate with: openssl rand -base64 32"
        }

        if (errors.isNotEmpty()) {
            val message = "Security configuration is invalid:\n" + errors.joinToString("\n")
            logger.error(message)
            throw IllegalStateException(message)
        }
    }
}
