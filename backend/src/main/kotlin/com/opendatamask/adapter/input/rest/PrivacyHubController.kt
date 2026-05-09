package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.PrivacyHubSummary
import com.opendatamask.domain.port.input.dto.PrivacyRecommendation
import com.opendatamask.domain.port.input.dto.KAnonymityReport
import com.opendatamask.domain.port.input.dto.GdprComplianceReport
import com.opendatamask.application.service.GdprComplianceService
import com.opendatamask.application.service.KAnonymityService
import com.opendatamask.application.service.PrivacyHubService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/privacy-hub")
class PrivacyHubController(
    private val privacyHubService: PrivacyHubService,
    private val kAnonymityService: KAnonymityService,
    private val gdprComplianceService: GdprComplianceService
) {

    @GetMapping
    fun getSummary(@PathVariable workspaceId: Long): PrivacyHubSummary =
        privacyHubService.getSummary(workspaceId)

    @GetMapping("/recommendations")
    fun getRecommendations(@PathVariable workspaceId: Long): List<PrivacyRecommendation> =
        privacyHubService.getRecommendations(workspaceId)

    @PostMapping("/recommendations/apply")
    fun applyRecommendations(@PathVariable workspaceId: Long): ResponseEntity<Map<String, Int>> {
        val count = privacyHubService.applyRecommendations(workspaceId)
        return ResponseEntity.ok(mapOf("applied" to count))
    }

    @GetMapping("/k-anonymity")
    @io.swagger.v3.oas.annotations.Operation(summary = "Compute k-anonymity risk score for quasi-identifiers in the workspace")
    fun getKAnonymityScore(@PathVariable workspaceId: Long): ResponseEntity<KAnonymityReport> {
        return ResponseEntity.ok(kAnonymityService.computeKAnonymity(workspaceId))
    }

    @GetMapping("/gdpr-report")
    @io.swagger.v3.oas.annotations.Operation(summary = "Generate GDPR Article 5 compliance report for this workspace")
    fun getGdprReport(@PathVariable workspaceId: Long): ResponseEntity<GdprComplianceReport> {
        return ResponseEntity.ok(gdprComplianceService.generateReport(workspaceId))
    }
}

