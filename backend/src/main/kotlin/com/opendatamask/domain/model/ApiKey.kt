package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "api_keys")
class ApiKey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    // Human-readable name for the key (e.g., "CI/CD pipeline", "Data team service account")
    @Column(nullable = false)
    var name: String,

    // The key prefix shown to the user after creation (first 8 chars of the raw key)
    @Column(nullable = false, length = 8)
    val keyPrefix: String,

    // BCrypt hash of the full key. The raw key is NEVER stored.
    @Column(nullable = false, length = 256)
    val keyHash: String,

    // The user who created this key
    @Column(nullable = false)
    val createdBy: Long,

    // When null, the key has workspace-global scope; otherwise scoped to one workspace
    @Column
    val workspaceId: Long? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    // Null = never expires
    @Column
    val expiresAt: Instant? = null,

    @Column
    var revokedAt: Instant? = null,

    @Column
    var lastUsedAt: Instant? = null
) {
    val isActive: Boolean get() = revokedAt == null && (expiresAt == null || Instant.now().isBefore(expiresAt))
}
