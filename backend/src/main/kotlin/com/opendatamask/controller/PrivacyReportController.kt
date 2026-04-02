package com.opendatamask.controller

import com.opendatamask.model.PrivacyReport
import com.opendatamask.service.PrivacyReportService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class PrivacyReportController(
    private val privacyReportService: PrivacyReportService
) {

    @GetMapping("/api/workspaces/{workspaceId}/privacy-report")
    fun getCurrentReport(@PathVariable workspaceId: Long): PrivacyReport {
        val existing = privacyReportService.getLatestCurrentReport(workspaceId)
        return existing ?: privacyReportService.generateCurrentConfigReport(workspaceId)
    }

    @GetMapping("/api/workspaces/{workspaceId}/privacy-report/download")
    fun downloadCurrentReport(@PathVariable workspaceId: Long): ResponseEntity<String> {
        val existing = privacyReportService.getLatestCurrentReport(workspaceId)
        val report = existing ?: privacyReportService.generateCurrentConfigReport(workspaceId)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"privacy-report-${report.id}.json\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(report.reportJson)
    }

    @GetMapping("/api/workspaces/{workspaceId}/jobs/{jobId}/privacy-report")
    fun getJobReport(
        @PathVariable workspaceId: Long,
        @PathVariable jobId: Long
    ): PrivacyReport {
        val existing = privacyReportService.getJobReport(jobId)
        return existing ?: privacyReportService.generateJobReport(jobId, workspaceId)
    }
}
