package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.AuditLog
import java.time.Instant

interface AuditLogPort {
    fun save(auditLog: AuditLog): AuditLog
    fun findByWorkspaceId(workspaceId: Long, from: Instant?, to: Instant?, limit: Int): List<AuditLog>
    fun findAll(from: Instant?, to: Instant?, limit: Int): List<AuditLog>
}
