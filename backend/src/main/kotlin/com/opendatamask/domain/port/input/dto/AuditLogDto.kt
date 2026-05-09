package com.opendatamask.domain.port.input.dto

import com.opendatamask.domain.model.AuditAction
import com.opendatamask.domain.model.AuditResourceType
import java.time.Instant

data class AuditLogResponse(
    val id: Long,
    val timestamp: Instant,
    val actorId: Long?,
    val actorUsername: String?,
    val action: AuditAction,
    val resourceType: AuditResourceType,
    val resourceId: String?,
    val workspaceId: Long,
    val beforeJson: String?,
    val afterJson: String?,
    val ipAddress: String?,
    val description: String?
)
