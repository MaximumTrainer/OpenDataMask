package com.opendatamask.adapter.input.rest

import com.opendatamask.application.service.HipaaComplianceService
import com.opendatamask.domain.port.input.dto.HipaaComplianceReport
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "HIPAA Compliance", description = "HIPAA Safe Harbor automated compliance assessment")
class HipaaComplianceController(private val hipaaComplianceService: HipaaComplianceService) {

    @GetMapping("/api/workspaces/{workspaceId}/hipaa-status")
    @Operation(
        summary = "Get HIPAA Safe Harbor compliance report for a workspace",
        description = "Evaluates the workspace masking configuration against all 15 detected HIPAA Safe Harbor PHI categories. " +
            "Returns per-category compliance status and a list of affected columns with their masking state."
    )
    fun getHipaaComplianceReport(
        @PathVariable workspaceId: Long
    ): ResponseEntity<HipaaComplianceReport> =
        ResponseEntity.ok(hipaaComplianceService.getComplianceReport(workspaceId))
}
