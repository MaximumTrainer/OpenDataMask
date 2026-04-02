package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.input.rest.dto.SubsetTableConfigRequest
import com.opendatamask.adapter.input.rest.dto.SubsetTableConfigResponse
import com.opendatamask.adapter.input.rest.dto.toResponse
import com.opendatamask.application.service.SubsetConfigService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/subset-config")
class SubsetConfigController(
    private val subsetConfigService: SubsetConfigService
) {

    @GetMapping
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
    fun deleteConfig(
        @PathVariable workspaceId: Long,
        @PathVariable cfgId: Long
    ): ResponseEntity<Void> {
        subsetConfigService.deleteConfig(workspaceId, cfgId)
        return ResponseEntity.noContent().build()
    }
}
