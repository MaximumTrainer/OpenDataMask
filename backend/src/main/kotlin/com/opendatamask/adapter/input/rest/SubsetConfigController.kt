package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.SubsetTableConfigRequest
import com.opendatamask.adapter.input.rest.dto.SubsetTableConfigResponse
import com.opendatamask.adapter.input.rest.dto.toResponse
import com.opendatamask.application.service.SubsetConfigService
import com.opendatamask.application.service.SubsetEstimationService
import com.opendatamask.domain.port.input.dto.SubsetEstimateResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/subset-config")
@Tag(name = "Subset Config", description = "Manage subset configuration and estimate subset sizes")
class SubsetConfigController(
    private val subsetConfigService: SubsetConfigService,
    private val subsetEstimationService: SubsetEstimationService
) {

    @GetMapping
    @Operation(summary = "List all subset table configurations for a workspace")
    fun listConfigs(
        @PathVariable workspaceId: Long
    ): ResponseEntity<List<SubsetTableConfigResponse>> {
        return ResponseEntity.ok(subsetConfigService.listConfigs(workspaceId).map { it.toResponse() })
    }

    @PostMapping
    fun createOrUpdateConfig(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: SubsetTableConfigRequest
    ): ResponseEntity<SubsetTableConfigResponse> {
        val config = subsetConfigService.createOrUpdateConfig(workspaceId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(config.toResponse())
    }

    @PutMapping("/{cfgId}")
    fun updateConfig(
        @PathVariable workspaceId: Long,
        @PathVariable cfgId: Long,
        @Valid @RequestBody request: SubsetTableConfigRequest
    ): ResponseEntity<SubsetTableConfigResponse> {
        return ResponseEntity.ok(subsetConfigService.updateConfig(workspaceId, cfgId, request).toResponse())
    }

    @DeleteMapping("/{cfgId}")
    @Operation(summary = "Delete a subset table configuration")
    fun deleteConfig(
        @PathVariable workspaceId: Long,
        @PathVariable cfgId: Long
    ): ResponseEntity<Void> {
        subsetConfigService.deleteConfig(workspaceId, cfgId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/estimate")
    @Operation(
        summary = "Estimate subset size without running a job",
        description = "Runs COUNT queries against the source connection to estimate how many rows would be " +
            "included for each table given the current subset config. No data is extracted."
    )
    fun estimateSubset(
        @PathVariable workspaceId: Long
    ): ResponseEntity<SubsetEstimateResponse> =
        ResponseEntity.ok(subsetEstimationService.estimate(workspaceId))
}

