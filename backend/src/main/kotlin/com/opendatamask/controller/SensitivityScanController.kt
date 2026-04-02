package com.opendatamask.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.opendatamask.dto.SensitivityScanLogEntryDto
import com.opendatamask.dto.SensitivityScanLogResponse
import com.opendatamask.model.ColumnSensitivity
import com.opendatamask.model.ConfidenceLevel
import com.opendatamask.model.SensitivityScanLog
import com.opendatamask.model.SensitivityType
import com.opendatamask.repository.ColumnSensitivityRepository
import com.opendatamask.repository.SensitivityScanLogEntryRepository
import com.opendatamask.service.SensitivityScanService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SensitivityOverrideRequest(
    val isSensitive: Boolean,
    val sensitivityType: SensitivityType,
    val confidenceLevel: ConfidenceLevel
)

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/sensitivity-scan")
class SensitivityScanController(
    private val sensitivityScanService: SensitivityScanService,
    private val columnSensitivityRepository: ColumnSensitivityRepository,
    private val sensitivityScanLogEntryRepository: SensitivityScanLogEntryRepository,
    private val objectMapper: ObjectMapper
) {

    @PostMapping("/run")
    fun runScan(@PathVariable workspaceId: Long): SensitivityScanLog =
        sensitivityScanService.scanWorkspace(workspaceId)

    @GetMapping("/status")
    fun getStatus(@PathVariable workspaceId: Long): SensitivityScanLog? =
        sensitivityScanService.getLatestLog(workspaceId)

    @GetMapping("/results")
    fun getResults(@PathVariable workspaceId: Long): List<ColumnSensitivity> =
        columnSensitivityRepository.findByWorkspaceId(workspaceId)

    @GetMapping("/log")
    fun getLog(@PathVariable workspaceId: Long): List<SensitivityScanLogResponse> =
        sensitivityScanService.getScanLogs(workspaceId).map { it.toResponse() }

    @GetMapping("/log/download")
    fun downloadLog(@PathVariable workspaceId: Long): ResponseEntity<String> {
        val logs = sensitivityScanService.getScanLogs(workspaceId).map { it.toResponse() }
        val json = objectMapper.writeValueAsString(logs)
        val filename = "sensitivity-scan-log-workspace-$workspaceId.json"
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
            .contentType(MediaType.APPLICATION_JSON)
            .body(json)
    }

    @PatchMapping("/columns/{table}/{column}")
    fun updateSensitivity(
        @PathVariable workspaceId: Long,
        @PathVariable table: String,
        @PathVariable column: String,
        @RequestBody request: SensitivityOverrideRequest
    ): ColumnSensitivity {
        val entity = columnSensitivityRepository
            .findByWorkspaceIdAndTableNameAndColumnName(workspaceId, table, column)
            ?: ColumnSensitivity(workspaceId = workspaceId, tableName = table, columnName = column)
        entity.isSensitive = request.isSensitive
        entity.sensitivityType = request.sensitivityType
        entity.confidenceLevel = request.confidenceLevel
        return columnSensitivityRepository.save(entity)
    }

    private fun SensitivityScanLog.toResponse(): SensitivityScanLogResponse {
        val entries = sensitivityScanLogEntryRepository.findByScanLogId(this.id ?: 0L)
            .map { entry ->
                SensitivityScanLogEntryDto(
                    tableName = entry.tableName,
                    columnName = entry.columnName,
                    detectedType = entry.detectedType,
                    confidenceLevel = entry.confidenceLevel,
                    recommendedGenerator = entry.recommendedGenerator,
                    scannedAt = entry.scannedAt
                )
            }
        return SensitivityScanLogResponse(
            id = this.id,
            workspaceId = this.workspaceId,
            startedAt = this.startedAt,
            completedAt = this.completedAt,
            status = this.status,
            scannedColumns = this.columnsScanned,
            detectedColumns = this.sensitiveColumnsFound,
            entries = entries
        )
    }
}
