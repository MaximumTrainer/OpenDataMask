package com.opendatamask.controller

import com.opendatamask.dto.ApplyPresetRequest
import com.opendatamask.dto.GeneratorPresetRequest
import com.opendatamask.dto.GeneratorPresetResponse
import com.opendatamask.service.GeneratorPresetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class GeneratorPresetController(
    private val generatorPresetService: GeneratorPresetService
) {

    @GetMapping("/api/generator-presets")
    fun listSystemPresets(): ResponseEntity<List<GeneratorPresetResponse>> =
        ResponseEntity.ok(generatorPresetService.listSystemPresets())

    @GetMapping("/api/workspaces/{workspaceId}/generator-presets")
    fun listWorkspacePresets(
        @PathVariable workspaceId: Long
    ): ResponseEntity<List<GeneratorPresetResponse>> =
        ResponseEntity.ok(generatorPresetService.listWorkspacePresets(workspaceId))

    @PostMapping("/api/workspaces/{workspaceId}/generator-presets")
    fun createPreset(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: GeneratorPresetRequest
    ): ResponseEntity<GeneratorPresetResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(generatorPresetService.createPreset(workspaceId, request))

    @PutMapping("/api/workspaces/{workspaceId}/generator-presets/{presetId}")
    fun updatePreset(
        @PathVariable workspaceId: Long,
        @PathVariable presetId: Long,
        @Valid @RequestBody request: GeneratorPresetRequest
    ): ResponseEntity<GeneratorPresetResponse> =
        ResponseEntity.ok(generatorPresetService.updatePreset(workspaceId, presetId, request))

    @DeleteMapping("/api/workspaces/{workspaceId}/generator-presets/{presetId}")
    fun deletePreset(
        @PathVariable workspaceId: Long,
        @PathVariable presetId: Long
    ): ResponseEntity<Void> {
        generatorPresetService.deletePreset(workspaceId, presetId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/api/workspaces/{workspaceId}/tables/{tableName}/columns/{columnName}/generator/preset")
    fun applyPreset(
        @PathVariable workspaceId: Long,
        @PathVariable tableName: String,
        @PathVariable columnName: String,
        @Valid @RequestBody request: ApplyPresetRequest
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            generatorPresetService.applyPresetToColumn(workspaceId, tableName, columnName, request.presetId)
        )
}
