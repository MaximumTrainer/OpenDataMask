package com.opendatamask.adapter.input.rest

import com.opendatamask.application.service.AuditService
import com.opendatamask.domain.port.input.dto.AuditLogResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
@Tag(name = "Audit Log", description = "Immutable audit trail for all user and system actions")
class AuditLogController(private val auditService: AuditService) {

    @GetMapping("/api/workspaces/{workspaceId}/audit-log")
    @Operation(summary = "Get audit log entries for a workspace (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    fun getWorkspaceAuditLog(
        @PathVariable workspaceId: Long,
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(defaultValue = "500") limit: Int
    ): ResponseEntity<List<AuditLogResponse>> =
        ResponseEntity.ok(auditService.getWorkspaceAuditLog(workspaceId, from, to, limit))

    @GetMapping("/api/audit-log")
    @Operation(summary = "Get global audit log (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    fun getGlobalAuditLog(
        @RequestParam(required = false) from: Instant?,
        @RequestParam(required = false) to: Instant?,
        @RequestParam(defaultValue = "500") limit: Int
    ): ResponseEntity<List<AuditLogResponse>> =
        ResponseEntity.ok(auditService.getGlobalAuditLog(from, to, limit))
}