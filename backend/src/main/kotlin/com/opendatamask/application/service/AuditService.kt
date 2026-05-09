package com.opendatamask.application.service

import com.opendatamask.domain.model.*
import com.opendatamask.domain.port.input.dto.AuditLogResponse
import com.opendatamask.domain.port.output.AuditLogPort
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuditService(private val auditLogPort: AuditLogPort) {

    fun record(
        workspaceId: Long,
        action: AuditAction,
        resourceType: AuditResourceType,
        resourceId: String? = null,
        actorId: Long? = null,
        actorUsername: String? = null,
        beforeJson: String? = null,
        afterJson: String? = null,
        ipAddress: String? = null,
        description: String? = null
    ): AuditLog {
        val entry = AuditLog(
            timestamp = Instant.now(),
            actorId = actorId,
            actorUsername = actorUsername,
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            workspaceId = workspaceId,
            beforeJson = beforeJson,
            afterJson = afterJson,
            ipAddress = ipAddress,
            description = description
        )
        return auditLogPort.save(entry)
    }

    fun getWorkspaceAuditLog(
        workspaceId: Long,
        from: Instant? = null,
        to: Instant? = null,
        limit: Int = 500
    ): List<AuditLogResponse> =
        auditLogPort.findByWorkspaceId(workspaceId, from, to, limit.coerceAtMost(1000)).map { it.toResponse() }

    fun getGlobalAuditLog(
        from: Instant? = null,
        to: Instant? = null,
        limit: Int = 500
    ): List<AuditLogResponse> =
        auditLogPort.findAll(from, to, limit.coerceAtMost(1000)).map { it.toResponse() }

    private fun AuditLog.toResponse() = AuditLogResponse(
        id = id,
        timestamp = timestamp,
        actorId = actorId,
        actorUsername = actorUsername,
        action = action,
        resourceType = resourceType,
        resourceId = resourceId,
        workspaceId = workspaceId,
        beforeJson = beforeJson,
        afterJson = afterJson,
        ipAddress = ipAddress,
        description = description
    )
}
