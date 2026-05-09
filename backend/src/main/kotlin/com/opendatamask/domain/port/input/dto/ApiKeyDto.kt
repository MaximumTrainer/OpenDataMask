package com.opendatamask.domain.port.input.dto

import java.time.Instant

data class CreateApiKeyRequest(
    val name: String,
    val workspaceId: Long? = null,
    val expiresAt: Instant? = null
)

data class ApiKeyResponse(
    val id: Long,
    val name: String,
    val keyPrefix: String,
    val createdBy: Long,
    val workspaceId: Long?,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val revokedAt: Instant?,
    val lastUsedAt: Instant?,
    val isActive: Boolean
)

// Only returned once at creation time — the full key is never stored or returned again
data class ApiKeyCreatedResponse(
    val key: String,
    val apiKey: ApiKeyResponse
)
