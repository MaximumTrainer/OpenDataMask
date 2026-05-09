package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.Instant

enum class AuditAction {
    // Workspace
    WORKSPACE_CREATED, WORKSPACE_UPDATED, WORKSPACE_DELETED,
    // Connection
    CONNECTION_CREATED, CONNECTION_UPDATED, CONNECTION_DELETED,
    // Job
    JOB_STARTED, JOB_COMPLETED, JOB_FAILED, JOB_CANCELLED,
    // Masking config
    GENERATOR_CONFIGURED, GENERATOR_DELETED,
    MAPPING_SAVED, MAPPING_DELETED,
    // Privacy
    SENSITIVITY_SCAN_RUN,
    // Access control
    USER_INVITED, USER_REMOVED, PERMISSION_CHANGED,
    ROLE_CHANGED,
    // Security
    USER_LOGIN, USER_LOGOUT, USER_REGISTERED,
    API_KEY_CREATED, API_KEY_REVOKED,
    // Webhooks
    WEBHOOK_CREATED, WEBHOOK_UPDATED, WEBHOOK_DELETED,
    // Schema
    SCHEMA_CHANGE_RESOLVED, SCHEMA_CHANGE_DISMISSED
}

enum class AuditResourceType {
    WORKSPACE, CONNECTION, CONNECTION_PAIR, JOB, GENERATOR, MAPPING,
    SENSITIVITY_SCAN, USER, WORKSPACE_USER, API_KEY, WEBHOOK, SCHEMA_CHANGE
}

// Append-only audit log entry. This entity must NEVER be updated or deleted.
// The table has no UPDATE or DELETE grants in hardened deployments.
@Entity
@Table(name = "audit_logs")
class AuditLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val timestamp: Instant = Instant.now(),

    // The user who performed the action. Null for system-initiated actions.
    @Column
    val actorId: Long? = null,

    @Column
    val actorUsername: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    val action: AuditAction,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    val resourceType: AuditResourceType,

    @Column
    val resourceId: String? = null,

    @Column(nullable = false)
    val workspaceId: Long,

    // JSON snapshot of the resource state before the change (null for creates)
    @Column(columnDefinition = "TEXT")
    val beforeJson: String? = null,

    // JSON snapshot of the resource state after the change (null for deletes)
    @Column(columnDefinition = "TEXT")
    val afterJson: String? = null,

    @Column(length = 45)
    val ipAddress: String? = null,

    // Free-text description for additional context
    @Column(length = 512)
    val description: String? = null
)
