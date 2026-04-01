package com.opendatamask.controller

import com.opendatamask.model.ColumnSensitivity
import com.opendatamask.model.ConfidenceLevel
import com.opendatamask.model.SensitivityScanLog
import com.opendatamask.model.SensitivityType
import com.opendatamask.repository.ColumnSensitivityRepository
import com.opendatamask.service.SensitivityScanService
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
    private val columnSensitivityRepository: ColumnSensitivityRepository
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
    fun getLog(@PathVariable workspaceId: Long): List<SensitivityScanLog> =
        sensitivityScanService.getScanLogs(workspaceId)

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
}
