package com.opendatamask.application.service

import com.opendatamask.domain.model.ApiKey
import com.opendatamask.domain.port.input.dto.ApiKeyCreatedResponse
import com.opendatamask.domain.port.input.dto.ApiKeyResponse
import com.opendatamask.domain.port.input.dto.CreateApiKeyRequest
import com.opendatamask.domain.port.output.ApiKeyPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
class ApiKeyService(
    private val apiKeyPort: ApiKeyPort,
    private val passwordEncoder: PasswordEncoder
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun createApiKey(request: CreateApiKeyRequest, createdBy: Long): ApiKeyCreatedResponse {
        val rawKey = generateSecureKey()
        val apiKey = ApiKey(
            name = request.name,
            keyPrefix = rawKey.take(8),
            keyHash = passwordEncoder.encode(rawKey),
            createdBy = createdBy,
            workspaceId = request.workspaceId,
            expiresAt = request.expiresAt
        )
        val saved = apiKeyPort.save(apiKey)
        return ApiKeyCreatedResponse(key = rawKey, apiKey = saved.toResponse())
    }

    fun listApiKeys(userId: Long, isAdmin: Boolean): List<ApiKeyResponse> =
        if (isAdmin) apiKeyPort.findAll().map { it.toResponse() }
        else apiKeyPort.findByCreatedBy(userId).map { it.toResponse() }

    fun getApiKey(id: Long): ApiKeyResponse =
        apiKeyPort.findById(id).orElseThrow { NoSuchElementException("API key not found") }.toResponse()

    @Transactional
    fun revokeApiKey(id: Long) {
        val key = apiKeyPort.findById(id).orElseThrow { NoSuchElementException("API key not found") }
        val revoked = ApiKey(
            id = key.id,
            name = key.name,
            keyPrefix = key.keyPrefix,
            keyHash = key.keyHash,
            createdBy = key.createdBy,
            workspaceId = key.workspaceId,
            createdAt = key.createdAt,
            expiresAt = key.expiresAt,
            revokedAt = Instant.now(),
            lastUsedAt = key.lastUsedAt
        )
        apiKeyPort.save(revoked)
    }

    @Transactional
    fun deleteApiKey(id: Long) = apiKeyPort.deleteById(id)

    // Called by the authentication filter to resolve a raw key to an ApiKey entity
    fun findAndValidateKey(rawKey: String): ApiKey? =
        apiKeyPort.findAllActive().firstOrNull { passwordEncoder.matches(rawKey, it.keyHash) }

    @Transactional
    fun recordUsage(key: ApiKey) {
        val updated = ApiKey(
            id = key.id,
            name = key.name,
            keyPrefix = key.keyPrefix,
            keyHash = key.keyHash,
            createdBy = key.createdBy,
            workspaceId = key.workspaceId,
            createdAt = key.createdAt,
            expiresAt = key.expiresAt,
            revokedAt = key.revokedAt,
            lastUsedAt = Instant.now()
        )
        apiKeyPort.save(updated)
    }

    private fun generateSecureKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return "odm_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun ApiKey.toResponse() = ApiKeyResponse(
        id = id,
        name = name,
        keyPrefix = keyPrefix,
        createdBy = createdBy,
        workspaceId = workspaceId,
        createdAt = createdAt,
        expiresAt = expiresAt,
        revokedAt = revokedAt,
        lastUsedAt = lastUsedAt,
        isActive = isActive
    )
}
